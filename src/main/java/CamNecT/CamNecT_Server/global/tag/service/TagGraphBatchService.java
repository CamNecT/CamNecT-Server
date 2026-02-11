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

        int topK = props.getTopK();
        int minEvidence = props.getMinEvidence();
        double threshold = props.getScoreThreshold();

        log.info("[TagGraphBatch] start topK={}, minEvidence={}, threshold={}", topK, minEvidence, threshold);

        // 1) stats
        int s1 = batchRepository.upsertProfileTagStats();
        int s2 = batchRepository.upsertPostCommunityTagStats();
        int s3 = batchRepository.upsertPostActivityTagStats();

        // 2) relation
        int r1 = batchRepository.upsertProfileTagRelation(minEvidence, threshold, topK);
        int r2 = batchRepository.upsertPostCommunityTagRelation(minEvidence, threshold, topK);
        int r3 = batchRepository.upsertPostActivityTagRelation(minEvidence, threshold, topK);

        log.info("[TagGraphBatch] done stats(profile={}, community={}, activity={}) relation(profile={}, community={}, activity={})",
                s1, s2, s3, r1, r2, r3);
    }

    public TagGraphBatchResult runNow() {
        int topK = props.getTopK();
        int minEvidence = props.getMinEvidence();
        double threshold = props.getScoreThreshold();

        int s1 = batchRepository.upsertProfileTagStats();
        int s2 = batchRepository.upsertPostCommunityTagStats();
        int s3 = batchRepository.upsertPostActivityTagStats();

        int r1 = batchRepository.upsertProfileTagRelation(minEvidence, threshold, topK);
        int r2 = batchRepository.upsertPostCommunityTagRelation(minEvidence, threshold, topK);
        int r3 = batchRepository.upsertPostActivityTagRelation(minEvidence, threshold, topK);

        return new TagGraphBatchResult(s1, s2, s3, r1, r2, r3);
    }
}