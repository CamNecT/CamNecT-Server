package CamNecT.CamNecT_Server.global.tag.service;

import CamNecT.CamNecT_Server.global.tag.dto.TagCategoryDto;
import CamNecT.CamNecT_Server.global.tag.dto.TagDto;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import CamNecT.CamNecT_Server.global.tag.model.TagCategory;
import CamNecT.CamNecT_Server.global.tag.model.enums.TagScope;
import CamNecT.CamNecT_Server.global.tag.repository.TagCategoryRepository;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagQueryService {

    // DB에 저장해둘 tag_categories.code 값(권장)
    private static final String CODE_ADOPTION_STATUS = "adoption_status";
    private static final String CODE_RECRUIT_STATUS  = "recruit_status";
    private static final String CODE_TEMP_TAGS       = "temp_tags";

    private final TagCategoryRepository categoryRepo;
    private final TagRepository tagRepo;

    public List<TagCategoryDto> listCategoriesWithTags(TagScope scope) {
        TagScope s = (scope == null) ? TagScope.DEFAULT : scope;

        // 1) 기본 카테고리 코드들 조회 (adoption/recruit 제외)
        List<String> baseCodes = categoryRepo.findBaseCategoryCodesExcluding(
                List.of(CODE_ADOPTION_STATUS, CODE_RECRUIT_STATUS, CODE_TEMP_TAGS)
        );

        // 2) scope에 따라 추가 코드 끼워넣기
        List<String> codes = new ArrayList<>(baseCodes);
        if (s == TagScope.COMMUNITY_QUESTION) {
            codes.add(CODE_ADOPTION_STATUS);
        } else if (s == TagScope.ACTIVITY_RECRUIT) {
            codes.add(CODE_RECRUIT_STATUS);
        }

        // 3) 카테고리 로드
        List<TagCategory> categories = categoryRepo.findAllByActiveTrueAndCodeInOrderBySortOrderAscIdAsc(codes);
        if (categories.isEmpty()) return List.of();

        // 4) 태그 로드
        List<Long> categoryIds = categories.stream().map(TagCategory::getId).toList();
        List<Tag> tags = tagRepo.findActiveByCategoryIds(categoryIds);

        Map<Long, List<TagDto>> tagsByCatId = new HashMap<>();
        for (Tag t : tags) {
            Long catId = t.getCategory().getId();
            tagsByCatId.computeIfAbsent(catId, k -> new ArrayList<>())
                    .add(new TagDto(t.getId(), t.getName()));
        }

        return categories.stream()
                .map(c -> new TagCategoryDto(
                        c.getId(),
                        c.getCode(),
                        c.getName(),
                        tagsByCatId.getOrDefault(c.getId(), List.of())
                ))
                .toList();
    }
}