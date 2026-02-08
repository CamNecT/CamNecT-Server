package CamNecT.CamNecT_Server.domain.alumni.repository;

import CamNecT.CamNecT_Server.domain.users.model.QUsers;
import CamNecT.CamNecT_Server.domain.users.model.QUserProfile;
import CamNecT.CamNecT_Server.domain.users.model.QUserTagMap;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AlumniRepositoryImpl implements AlumniRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findAlumniIdsByConditions(Long myId, String name, List<Long> tagIdList) {

        QUsers user = QUsers.users;
        QUserProfile profile = QUserProfile.userProfile;
        QUserTagMap myTagMap = new QUserTagMap("myTagMap");
        QUserTagMap commonTagMap = new QUserTagMap("commonTagMap");

        return queryFactory
                .select(user.userId)
                .from(user)
                .join(profile).on(user.userId.eq(profile.userId))
                .where(
                        user.userId.ne(myId),
                        nameContains(name),
                        hasAllTags(tagIdList, user.userId)
                )
                .orderBy(
                        // 나와 공통 태그 개수를 계산하는 서브쿼리를 OrderSpecifier로 래핑
                        new OrderSpecifier<>(Order.DESC,
                                JPAExpressions
                                        .select(commonTagMap.count())
                                        .from(commonTagMap)
                                        .where(
                                                commonTagMap.userId.eq(user.userId),
                                                commonTagMap.tagId.in(
                                                        JPAExpressions
                                                                .select(myTagMap.tagId)
                                                                .from(myTagMap)
                                                                .where(myTagMap.userId.eq(myId))
                                                )
                                        )
                        ),
                        user.createdAt.desc()  // 이건 그대로 사용 가능
                )
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