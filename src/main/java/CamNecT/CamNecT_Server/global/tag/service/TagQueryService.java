package CamNecT.CamNecT_Server.global.tag.service;

import CamNecT.CamNecT_Server.global.tag.dto.TagCategoryDto;
import CamNecT.CamNecT_Server.global.tag.dto.TagDto;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import CamNecT.CamNecT_Server.global.tag.model.TagCategory;
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

    private final TagCategoryRepository categoryRepo;
    private final TagRepository tagRepo;

    public List<TagCategoryDto> listCategoriesWithTags() {

        List<TagCategory> categories =
                categoryRepo.findAllByActiveTrueOrderBySortOrderAscIdAsc();
        if (categories.isEmpty()) return List.of();

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