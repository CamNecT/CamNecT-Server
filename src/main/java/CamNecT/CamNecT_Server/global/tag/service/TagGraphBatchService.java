package CamNecT.CamNecT_Server.global.tag.service;

import CamNecT.CamNecT_Server.global.tag.dto.TagGraphBatchResult;
import CamNecT.CamNecT_Server.global.tag.model.props.TagBatchProps;
import CamNecT.CamNecT_Server.global.tag.model.props.TagRelationProps;
import CamNecT.CamNecT_Server.global.tag.repository.TagGraphBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagGraphBatchService {

    private final TagGraphBatchRepository batchRepository;
    private final TagRelationProps props;
    private final TagBatchProps batchProps;

    @Scheduled(cron = "${app.tag-batch.cron}")
    @Transactional
    public void runDaily() {
        if (!batchProps.isEnabled()) return;
        doRun();
    }

    @Transactional
    public TagGraphBatchResult runNow() {
        return doRun();
    }

    private TagGraphBatchResult doRun() {
        int topK = props.getTopK();
        int minEvidence = props.getMinEvidence();
        double threshold = props.getScoreThreshold();

        // 배치 시작 시각(컷오프)
        Timestamp cutoff = Timestamp.valueOf(LocalDateTime.now());

        log.info("[TagGraphBatch] start topK={}, minEvidence={}, threshold={}, cutoff={}",
                topK, minEvidence, threshold, cutoff);

        // 1) stats
        int s1 = batchRepository.upsertProfileTagStats();
        int s2 = batchRepository.upsertPostCommunityTagStats();
        int s3 = batchRepository.upsertPostActivityTagStats();

        // 2) relation upsert
        int r1 = batchRepository.upsertProfileTagRelation(minEvidence, threshold, topK);
        int r2 = batchRepository.upsertPostCommunityTagRelation(minEvidence, threshold, topK);
        int r3 = batchRepository.upsertPostActivityTagRelation(minEvidence, threshold, topK);

        // 3) relation cleanup (이번 배치에서 갱신되지 않은 오래된 것 삭제)
        int c1 = batchRepository.cleanupProfileRelations(cutoff);
        int c2 = batchRepository.cleanupPostCommunityRelations(cutoff);
        int c3 = batchRepository.cleanupPostActivityRelations(cutoff);

        log.info("[TagGraphBatch] done stats(profile={}, community={}, activity={}) " +
                        "relation(profile={}, community={}, activity={}) cleanup(profile={}, community={}, activity={})",
                s1, s2, s3, r1, r2, r3, c1, c2, c3);

        return new TagGraphBatchResult(s1, s2, s3, r1, r2, r3);
    }
}