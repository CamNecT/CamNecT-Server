package CamNecT.server.domain.activity.ingestion;

import CamNecT.server.domain.activity.dto.request.AdminActivityRequest;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.enums.ActivityStatus;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import CamNecT.server.global.storage.service.FileStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityImportService {
    private final ActivityImportProperties properties;
    private final ActivityImportRepository imports;
    private final ExternalActivityRepository activities;
    private final UserRepository users;
    private final FileStorage files;
    private final ObjectMapper mapper;

    public record Result(Long activityId, String status, long revision) {}

    @Transactional
    public Result ingest(ActivityImportRequest request) {
        if (!properties.enabled())
            throw new CustomException(ErrorCode.FORBIDDEN);
        if (request.content().getBytes(StandardCharsets.UTF_8).length > 60_000 ||
                (request.applyStartDate() != null && request.applyEndDate() != null &&
                        request.applyStartDate().isAfter(request.applyEndDate())) ||
                (request.applyEndAt() != null && !request.applyEndAt().atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                        .toLocalDate().equals(request.applyEndDate())))
            throw new CustomException(ErrorCode.BAD_REQUEST);
        // One configured author row serializes imports, including the first insertion of a source ID.
        Long authorId = properties.authorUserId();
        if (authorId == null) {
            authorId = users.findByUsername(properties.authorUsername())
                    .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN)).getUserId();
        }
        var author = users.findByIdForUpdate(authorId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
        if (author.getRole() != UserRole.ADMIN || author.getStatus() != UserStatus.ACTIVE)
            throw new CustomException(ErrorCode.FORBIDDEN);
        String sourceId = "KONKUK_JOB_4084#" + request.articleId();
        var entry = imports.findById(sourceId).orElse(null);
        ExternalActivity activity = entry == null ? null : activities.findByIdForUpdate(entry.getActivityId()).orElse(null);
        if (entry != null) {
            if (activity == null) return new Result(entry.getActivityId(), "DELETED", entry.getSourceRevision());
            if (!entry.getManagedHash().equals(managedHash(activity)))
                return new Result(activity.getActivityId(), "MANUAL_OVERRIDE", entry.getSourceRevision());
            if (request.revision() < entry.getSourceRevision())
                return new Result(activity.getActivityId(), "STALE", entry.getSourceRevision());
            if (request.revision() == entry.getSourceRevision() && !entry.getContentHash().equals(request.contentHash()))
                throw new CustomException(ErrorCode.CONFLICT);
        }
        boolean created = activity == null;
        boolean same = !created && entry.getContentHash().equals(request.contentHash());
        if (same) {
            boolean expired = activity.getApplyEndAt() != null ? !activity.getApplyEndAt().isAfter(Instant.now()) :
                    activity.getApplyEndDate() != null && activity.getApplyEndDate().isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")));
            if (expired) activity.close();
            entry.applied(request.revision(), request.contentHash(), managedHash(activity), entry.getThumbnailHash());
            return new Result(activity.getActivityId(), "UNCHANGED", request.revision());
        }
        byte[] thumbnail = same ? null : validateThumbnail(request.thumbnail());
        if (created) {
            activity = activities.save(ExternalActivity.builder().user(author).category(ActivityCategory.EXTERNAL)
                    .title(request.title()).thumbnailKey("기본이미지").build());
            entry = new ActivityImport(sourceId, activity.getActivityId());
        }
        if (!same) {
            String oldKey = activity.getThumbnailKey();
            String imageHash = request.thumbnail() == null ? null : request.thumbnail().sha256();
            if (!Objects.equals(imageHash, entry.getThumbnailHash())) {
                String newKey = thumbnail == null ? "기본이미지" : files.save(
                        "activity/imports/konkuk-4084/" + request.articleId(),
                        new ImageFile(thumbnail, request.thumbnail().contentType()));
                activity.updateThumbnailKey(newKey);
                cleanupAfterTransaction(newKey, oldKey);
            }
        }
        var payload = new AdminActivityRequest(ActivityCategory.EXTERNAL, request.title(), List.of(),
                request.organizer(), null, request.applyStartDate(), request.applyEndDate(), null,
                "https://job.konkuk.ac.kr/bbs/job/4084/" + request.articleId() + "/artclView.do",
                null, request.title(), request.content());
        Instant deadline = request.applyEndAt() == null ? null : request.applyEndAt().toInstant();
        boolean closed = deadline != null ? !deadline.isAfter(Instant.now()) :
                request.applyEndDate() != null && request.applyEndDate().isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")));
        activity.applyImported(payload, deadline, closed ? ActivityStatus.CLOSED : ActivityStatus.OPEN);
        entry.applied(request.revision(), request.contentHash(), managedHash(activity),
                request.thumbnail() == null ? null : request.thumbnail().sha256());
        imports.save(entry);
        return new Result(activity.getActivityId(), created ? "CREATED" : same ? "UNCHANGED" : "UPDATED", request.revision());
    }

    private String managedHash(ExternalActivity activity) {
        try {
            return ActivityImportAuth.sha256(mapper.writeValueAsBytes(Arrays.asList(activity.getTitle(),
                    activity.getCategory(), activity.getOrganizer(), activity.getContext(), activity.getContextTitle(),
                    activity.getApplyStartDate(), activity.getApplyEndDate(), activity.getApplyEndAt(),
                    activity.getOfficialUrl(), activity.getThumbnailKey(), activity.getStatus(),
                    activity.getTargetDescription(), activity.getResultAnnounceDate())));
        } catch (IOException exception) { throw new IllegalStateException(exception); }
    }

    private byte[] validateThumbnail(ActivityImportRequest.Thumbnail thumbnail) {
        if (thumbnail == null) return null;
        try {
            byte[] data = Base64.getDecoder().decode(thumbnail.base64());
            if (data.length < 12 || data.length > 8_000_000 || !ActivityImportAuth.sha256(data).equals(thumbnail.sha256()))
                throw new IllegalArgumentException();
            boolean valid = switch (thumbnail.contentType()) {
                case "image/jpeg" -> data[0] == (byte) 0xff && data[1] == (byte) 0xd8 && data[2] == (byte) 0xff;
                case "image/png" -> Arrays.equals(Arrays.copyOf(data, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
                case "image/webp" -> new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") &&
                        new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
                default -> false;
            };
            if (!valid) throw new IllegalArgumentException();
            return data;
        } catch (IllegalArgumentException exception) { throw new CustomException(ErrorCode.BAD_REQUEST); }
    }

    private void cleanupAfterTransaction(String newKey, String oldKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String key = status == STATUS_COMMITTED ? oldKey : newKey;
                if (key == null || !key.contains("activity/imports/konkuk-4084/")) return;
                try { files.delete(key); }
                catch (RuntimeException exception) { log.warn("Activity import thumbnail cleanup failed", exception); }
            }
        });
    }

    private record ImageFile(byte[] bytes, String contentType) implements MultipartFile {
        @Override public String getName() { return "thumbnail"; }
        @Override public String getOriginalFilename() {
            return "poster." + switch (contentType) { case "image/jpeg" -> "jpg"; case "image/png" -> "png"; default -> "webp"; };
        }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
