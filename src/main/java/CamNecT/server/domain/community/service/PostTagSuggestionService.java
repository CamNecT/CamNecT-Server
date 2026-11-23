package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import CamNecT.server.global.tag.dto.TagDto;
import CamNecT.server.global.tag.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static CamNecT.server.domain.community.dto.request.CommunityRequestLimits.*;

@Service
public class PostTagSuggestionService {
    public record Draft(@NotNull BoardCode boardCode, @NotNull @Size(max = MAX_TITLE_LENGTH) String title,
                        @NotNull @Size(max = MAX_POST_CONTENT_LENGTH) String content) {}
    public record Suggestion(long id, String name, String evidence) {}
    record Candidate(long id, String evidence) {}

    private final TagRepository tags;
    private final ObjectMapper json;
    private final RestClient reader;
    private final Semaphore capacity = new Semaphore(2);

    public PostTagSuggestionService(TagRepository tags, ObjectMapper json,
                                    @Value("${community.tag-reader-url:http://tag-reader:8081}") String url) {
        this.tags = tags;
        this.json = json;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(800);
        factory.setReadTimeout(4000);
        this.reader = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public List<Suggestion> suggest(Draft draft) {
        if (draft.title().isBlank() && draft.content().isBlank()) return List.of();
        if (!capacity.tryAcquire()) throw new CustomException(CommunityErrorCode.TAG_SUGGESTIONS_UNAVAILABLE);
        List<Candidate> candidates;
        try {
            byte[] body = json.writeValueAsBytes(draft);
            candidates = reader.post().uri("/suggest").contentType(MediaType.APPLICATION_JSON)
                    .contentLength(body.length).body(body).retrieve()
                    .body(new ParameterizedTypeReference<List<Candidate>>() {});
        } catch (RestClientException | JsonProcessingException ex) {
            throw new CustomException(CommunityErrorCode.TAG_SUGGESTIONS_UNAVAILABLE);
        } finally {
            capacity.release();
        }
        if (candidates == null || candidates.isEmpty()) return List.of();
        var ranked = new LinkedHashMap<Long, Candidate>();
        candidates.forEach(candidate -> ranked.putIfAbsent(candidate.id(), candidate));
        var names = tags.findSelectableForPost(ranked.keySet()).stream()
                .collect(Collectors.toMap(TagDto::id, TagDto::name));
        return ranked.values().stream().filter(c -> names.containsKey(c.id())).limit(MAX_TAGS_PER_POST)
                .map(c -> new Suggestion(c.id(), names.get(c.id()), c.evidence())).toList();
    }
}
