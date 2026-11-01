package CamNecT.server.domain.activity.ingestion;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActivityImportExpirySchedulerTest {
    @Test void skipsManualRowsAndContinuesPastOneFailedItem() {
        var properties = mock(ActivityImportProperties.class);
        var repository = mock(ActivityImportRepository.class);
        var service = mock(ActivityImportService.class);
        when(properties.enabled()).thenReturn(true);
        when(repository.findExpiredCandidates(eq(""), any(), any(), any())).thenReturn(List.of("a", "b"));
        when(repository.findExpiredCandidates(eq("b"), any(), any(), any())).thenReturn(List.of("c"));
        when(repository.findExpiredCandidates(eq("c"), any(), any(), any())).thenReturn(List.of());
        when(service.expireImported(eq("b"), any())).thenThrow(new IllegalStateException("one item failed"));
        new ActivityImportExpiryScheduler(properties, repository, service).expire();
        verify(service).expireImported(eq("a"), any());
        verify(service).expireImported(eq("c"), any());
    }

    @Test void disabledImportsDoNotRunExpiry() {
        var properties = mock(ActivityImportProperties.class);
        var repository = mock(ActivityImportRepository.class);
        var service = mock(ActivityImportService.class);
        new ActivityImportExpiryScheduler(properties, repository, service).expire();
        verifyNoInteractions(repository, service);
    }
}
