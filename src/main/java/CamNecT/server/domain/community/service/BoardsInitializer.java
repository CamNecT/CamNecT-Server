package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.domain.community.repository.BoardsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BoardsInitializer implements ApplicationRunner {

    private final BoardsRepository boardsRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensure(BoardCode.INFO, "정보");
        ensure(BoardCode.QUESTION, "질문");
    }

    private void ensure(BoardCode code, String name) {
        if (boardsRepository.existsByCode(code)) return;

        try {
            boardsRepository.save(Boards.of(code, name));
        } catch (DataIntegrityViolationException e) {
            // 여러 인스턴스가 동시에 올라와서 누가 먼저 insert 했을 수 있음 → 무시
        }
    }
}
