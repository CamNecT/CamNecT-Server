package CamNecT.server.domain.report.dto.request;

import CamNecT.server.domain.report.model.ReportStatus;
import jakarta.validation.constraints.NotNull;


public record ReportProcessRequest(
        @NotNull ReportStatus status
) {
    public ReportStatus getStatus(){
        return status;
    }
}