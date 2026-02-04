package CamNecT.CamNecT_Server.global.storage.scheduler;

import CamNecT.CamNecT_Server.global.storage.repository.UploadTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UploadTicketExpiryScheduler {

    private final UploadTicketRepository ticketRepo;

    @Scheduled(fixedDelayString = "${app.upload-ticket.expire-job-delay-ms:900000}")
    @Transactional
    public void expirePendingTickets() {
        ticketRepo.bulkExpirePending(LocalDateTime.now());
    }
}