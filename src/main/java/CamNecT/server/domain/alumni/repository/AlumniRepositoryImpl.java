package CamNecT.server.domain.alumni.repository;

import CamNecT.server.domain.users.model.QUsers;
import CamNecT.server.domain.users.model.QUserTagMap;
import CamNecT.server.domain.users.model.UserStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
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
        QUserTagMap commonTagMap = new QUserTagMap("commonTagMap");

        // 1. 내 태그 ID 리스트 조회 (로그인 상태일 때만)
        List<Long> myTagIds = (myId != null) ? queryFactory
                .select(QUserTagMap.userTagMap.tagId)
                .from(QUserTagMap.userTagMap)
                .where(QUserTagMap.userTagMap.userId.eq(myId))
                .fetch() : Collections.emptyList();

        // 2. 공통 태그 카운트 (Null 방지를 위해 coalesce 사용)
        NumberExpression<Long> commonTagCount = commonTagMap.tagId.count().coalesce(0L);

        var query = queryFactory
                .select(user.userId)
                .from(user);

        // 3. 내 태그가 있을 때만 Left Join 수행 (없으면 조인할 필요 없음)
        if (!myTagIds.isEmpty()) {
            query.leftJoin(commonTagMap).on(
                    commonTagMap.userId.eq(user.userId)
                            .and(commonTagMap.tagId.in(myTagIds))
            );
        }

        return query
                .where(
                        user.userId.ne(myId),
                        user.status.eq(UserStatus.ACTIVE),
                        nameContains(name),
                        hasAllTags(tagIdList, user.userId)
                )
                .groupBy(user.userId, user.createdAt) // 정렬 컬럼 포함하여 그룹화
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