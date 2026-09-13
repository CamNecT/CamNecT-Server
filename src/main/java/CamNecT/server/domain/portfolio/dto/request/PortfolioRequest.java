package CamNecT.server.domain.portfolio.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Accessors(fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PortfolioRequest {
    @NotBlank @Size(max = 100)
    private String projectTitle;

    @Size(max = 50)
    @Schema(description = "부제목. 수정 시 미전송하면 기존 값을 보존하고, 명시적 null은 삭제합니다.", nullable = true)
    private String subtitle;

    @Size(max = 16000)
    private String description;

    @NotNull
    private LocalDate startedAt;

    private LocalDate endedAt;

    @Size(max = 100)
    private String project_role;

    @Size(max = 10)
    private List<@NotBlank @Size(max = 50) String> techStack = new ArrayList<>();

    @Size(max = 16000)
    private String review;

    @Size(max = 500)
    private String thumbnailKey;

    @Size(max = 10)
    private List<@NotBlank @Size(max = 500) String> attachmentKeys;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean subtitleProvided;

    public PortfolioRequest(String projectTitle, String subtitle, String description,
                            LocalDate startedAt, LocalDate endedAt, String project_role,
                            List<String> techStack, String review, String thumbnailKey,
                            List<String> attachmentKeys) {
        this.projectTitle = projectTitle;
        setSubtitle(subtitle);
        this.description = description;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.project_role = project_role;
        setTechStack(techStack);
        this.review = review;
        this.thumbnailKey = thumbnailKey;
        this.attachmentKeys = attachmentKeys;
    }

    @JsonSetter("subtitle")
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        this.subtitleProvided = true;
    }

    @JsonSetter("techStack")
    public void setTechStack(List<String> techStack) {
        this.techStack = techStack == null ? new ArrayList<>() : new ArrayList<>(techStack);
    }

    @AssertTrue(message = "종료일은 시작일보다 빠를 수 없습니다.")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return startedAt == null || endedAt == null || !endedAt.isBefore(startedAt);
    }
}
