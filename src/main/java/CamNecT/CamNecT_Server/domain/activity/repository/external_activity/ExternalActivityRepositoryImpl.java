package CamNecT.CamNecT_Server.domain.activity.repository.external_activity;

import CamNecT.CamNecT_Server.domain.activity.dto.response.ActivityPreviewResponse;
import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivity;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static CamNecT.CamNecT_Server.domain.activity.model.external_activity.QExternalActivity.externalActivity;
import static CamNecT.CamNecT_Server.domain.activity.model.external_activity.QExternalActivityBookmark.externalActivityBookmark;
import static CamNecT.CamNecT_Server.domain.activity.model.external_activity.QExternalActivityTag.externalActivityTag;
import static CamNecT.CamNecT_Server.domain.activity.model.recruitment.QTeamRecruitment.teamRecruitment;
import static CamNecT.CamNecT_Server.domain.users.model.QUserTagMap.userTagMap;
import static CamNecT.CamNecT_Server.global.tag.model.QTag.tag;

@Repository
@RequiredArgsConstructor
public class ExternalActivityRepositoryImpl implements ExternalActivityRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ActivityPreviewResponse> findActivitiesByCondition(
            Long userId, ActivityCategory category, List<Long> tagIds,
            String title, String sortType, Pageable pageable) {

        int pageSize = pageable.getPageSize();

        // 1. 기본 활동 ID 조회 (무한 스크롤을 위해 pageSize + 1)
        List<Long> activityIds = queryFactory
                .select(externalActivity.activityId)
                .from(externalActivity)
                .where(
                        categoryEq(category),
                        titleContains(title),
                        hasAllTags(tagIds)
                )
                .orderBy(getOrderSpecifier(sortType, userId))
                .offset(pageable.getOffset())
                .limit(pageSize + 1)
                .fetch();

        // 2. 활동이 없으면 빈 Slice 반환
        if (activityIds.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        // 3. 활동 일괄 조회 (N+1 해결)
        Map<Long, ExternalActivity> activityMap = queryFactory
                .selectFrom(externalActivity)
                .where(externalActivity.activityId.in(activityIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        ExternalActivity::getActivityId,
                        activity -> activity
                ));

        // 4. 태그 이름 일괄 조회 (N+1 해결)
        List<Tuple> tagTuples = queryFactory
                .select(externalActivityTag.activityId, tag.name)  // tag 대신 tag.name만 선택
                .from(externalActivityTag)
                .join(tag).on(externalActivityTag.tagId.eq(tag.id))
                .where(externalActivityTag.activityId.in(activityIds))
                .fetch();

        // 5. activityId별로 태그 이름 그룹핑
        Map<Long, List<String>> tagMap = tagTuples.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(externalActivityTag.activityId),
                        Collectors.mapping(
                                tuple -> tuple.get(tag.name),  // tag.name으로 변경
                                Collectors.toList()
                        )
                ));

        // 6. 정렬 순서 유지하며 Response 생성
        List<ActivityPreviewResponse> content = activityIds.stream()
                .map(id -> {
                    ExternalActivity activity = activityMap.get(id);
                    List<String> tags = tagMap.getOrDefault(id, Collections.emptyList());  // List<String>

                    return new ActivityPreviewResponse(
                            activity.getActivityId(),
                            activity.getTitle(),
                            activity.getContext(),
                            activity.getThumbnailUrl(),
                            tags
                    );
                })
                .collect(Collectors.toList());

        // 7. 무한 스크롤 여부 판단
        boolean hasNext = false;
        if (content.size() > pageSize) {
            content.remove(pageSize);
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression categoryEq(ActivityCategory category) {
        return category != null ? externalActivity.category.eq(category) : null;
    }

    private BooleanExpression titleContains(String title) {
        return title != null ? externalActivity.title.contains(title) : null;
    }

    private BooleanExpression hasAllTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return null;

        return Expressions.asBoolean(true).isTrue().and(
                externalActivity.activityId.in(
                        JPAExpressions.select(externalActivityTag.activityId)
                                .from(externalActivityTag)
                                .where(externalActivityTag.tagId.in(tagIds))
                                .groupBy(externalActivityTag.activityId)
                                .having(externalActivityTag.tagId.count().eq((long) tagIds.size()))
                )
        );
    }

    private OrderSpecifier<?> getOrderSpecifier(String sortType, Long userId) {
        if (sortType == null) {
            return externalActivity.applyStartDate.desc();
        }

        switch (sortType.toUpperCase()) {
            case "RECOMMEND":
                NumberExpression<Long> matchCount = Expressions.asNumber(
                        JPAExpressions.select(userTagMap.count())
                                .from(userTagMap)
                                .join(externalActivityTag).on(userTagMap.tagId.eq(externalActivityTag.tagId))
                                .where(userTagMap.userId.eq(userId)
                                        .and(externalActivityTag.activityId.eq(externalActivity.activityId)))
                );
                return matchCount.desc();

            case "DEADLINE":
                return externalActivity.applyEndDate.asc();

            case "BOOKMARK":
                NumberExpression<Long> bookmarkCount = Expressions.asNumber(
                        JPAExpressions.select(externalActivityBookmark.count())
                                .from(externalActivityBookmark)
                                .where(externalActivityBookmark.activityId.eq(externalActivity.activityId))
                );
                return bookmarkCount.desc();

            case "RECRUIT":
                NumberExpression<Long> recruitCount = Expressions.asNumber(
                        JPAExpressions.select(teamRecruitment.count())
                                .from(teamRecruitment)
                                .where(teamRecruitment.activityId.eq(externalActivity.activityId))
                );
                return recruitCount.desc();

            case "LATEST":
            default:
                return externalActivity.applyStartDate.desc();
        }
    }
}