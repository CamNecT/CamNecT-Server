package CamNecT.server.global.tag.service;

import CamNecT.server.global.tag.model.TagRelation;
import CamNecT.server.global.tag.model.enums.TagRelationContext;
import CamNecT.server.global.tag.repository.TagRelationRepository;
import CamNecT.server.global.tag.model.props.TagRelationProps;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagSimilarityService {

    private final TagRelationRepository tagRelationRepository;
    private final TagRelationProps props;

    /**
     * seedTags(원 태그들)를 context 기준으로 확장해서 (tagId -> weight)로 반환합니다.
     * - 원 태그 weight = 1.0
     * - 유사 태그 weight = alpha * score (원 태그보다 낮게)
     */
    public Map<Long, Double> expand(TagRelationContext context, Collection<Long> seedTags) {
        if (seedTags == null || seedTags.isEmpty()) return Map.of();

        Map<Long, Double> weights = new HashMap<>();
        for (Long t : seedTags) {
            if (t == null) continue;
            weights.put(t, 1.0); // 원태그
        }

        int topK = props.getTopK();
        double alpha = props.getAlpha();

        for (Long fromTagId : seedTags) {
            if (fromTagId == null) continue;

            // props topK 반영
            var rels = tagRelationRepository.findById_ContextAndId_FromTagIdOrderByScoreDesc(
                    context,
                    fromTagId,
                    PageRequest.of(0, topK)
            );

            for (TagRelation r : rels) {
                Long to = r.toTagId();
                if (to == null) continue;

                // 유사태그 가중치: alpha * score
                double w = alpha * (r.getScore() == null ? 0.0 : r.getScore());

                // 동일 태그(원태그)가 이미 1.0이면 유지
                // 여러 fromTag에서 동일 toTag가 나오면 더 큰 weight 채택 (max)
                weights.merge(to, w, Math::max);
            }
        }
        return weights;
    }

    /**
     * 후보 태그(candidateTags)가 확장가중치(weights)에 얼마나 매칭되는지 점수화.
     * - 합(sum) 방식: 단순하고 빠름
     */
    public double scoreBySum(Map<Long, Double> weights, Collection<Long> candidateTags) {
        if (weights == null || weights.isEmpty() || candidateTags == null || candidateTags.isEmpty()) return 0.0;

        double s = 0.0;
        for (Long t : candidateTags) {
            if (t == null) continue;
            Double w = weights.get(t);
            if (w != null) s += w;
        }
        return s;
    }

    /**
     * 태그가 많은 쪽이 유리해지는 걸 완화한 점수:
     * - seedTags 기준으로 "각 seed당 최고 매칭 1개"만 반영하는 형태가 더 이상적이지만,
     *   지금은 간단히 상한(capping)만 제공.
     */
    public double scoreByCappedSum(Map<Long, Double> weights, Collection<Long> candidateTags, double cap) {
        return Math.min(scoreBySum(weights, candidateTags), cap);
    }
}
