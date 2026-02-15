package CamNecT.server.domain.alumni.repository;

import CamNecT.server.domain.users.model.QUserProfile;
import CamNecT.server.domain.users.model.QUsers;
import CamNecT.server.domain.users.model.QUserTagMap;
import CamNecT.server.domain.users.model.UserStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AlumniRepositoryImpl implements AlumniRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findAlumniIdsByConditions(Long myId, String name, List<Long> tagIdList, Pageable pageable) {

        QUsers user = QUsers.users;
        QUserProfile profile = QUserProfile.userProfile;
        QUserTagMap commonTagMap = new QUserTagMap("commonTagMap");

        List<Long> myTagIds = (myId != null) ? queryFactory
                .select(QUserTagMap.userTagMap.tagId)
                .from(QUserTagMap.userTagMap)
                .where(QUserTagMap.userTagMap.userId.eq(myId))
                .fetch()
                : Collections.emptyList();

        // 항상 left join은 한다 (별칭이 SQL에 항상 등장)
        BooleanExpression joinCond = myTagIds.isEmpty()
                ? Expressions.FALSE
                : commonTagMap.tagId.in(myTagIds);

        NumberExpression<Long> commonTagCount = commonTagMap.tagId.count().coalesce(0L);

        return queryFactory
                .select(user.userId)
                .from(user)
                .join(profile).on(profile.userId.eq(user.userId))
                .leftJoin(commonTagMap).on(
                        commonTagMap.userId.eq(user.userId).and(joinCond)
                )
                .where(
                        myId == null ? null : user.userId.ne(myId),
                        user.status.eq(UserStatus.ACTIVE),
                        nameContains(name),
                        hasAllTags(tagIdList, user.userId)
                )
                .groupBy(user.userId, user.createdAt)
                .orderBy(
                        commonTagCount.desc(),
                        user.createdAt.desc(),
                        user.userId.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();
    }

    /**
     * 이름 검색 조건
     */
    private BooleanExpression nameContains(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return QUsers.users.name.contains(name);
    }

    /**
     * 모든 태그를 가지고 있는지 확인하는 조건
     * tagIdList의 모든 태그를 가진 유저만 필터링
     */
    private BooleanExpression hasAllTags(List<Long> tagIdList, com.querydsl.core.types.dsl.NumberPath<Long> userId) {
        if (CollectionUtils.isEmpty(tagIdList)) {
            return null;
        }

        QUserTagMap subTagMap = new QUserTagMap("subTagMap");

        // 서브쿼리: 해당 유저가 tagIdList의 태그를 모두 가지고 있는지 확인
        return userId.in(
                JPAExpressions
                        .select(subTagMap.userId)
                        .from(subTagMap)
                        .where(subTagMap.tagId.in(tagIdList))
                        .groupBy(subTagMap.userId)
                        .having(subTagMap.tagId.countDistinct().eq((long) tagIdList.size()))
        );
    }
}