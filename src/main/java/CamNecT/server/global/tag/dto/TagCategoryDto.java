package CamNecT.server.global.tag.dto;

import java.util.List;

public record TagCategoryDto(
        Long categoryId,
        String categoryCode,
        String categoryName,
        List<TagDto> tags
) {}