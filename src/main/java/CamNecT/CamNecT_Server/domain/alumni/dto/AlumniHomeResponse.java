package CamNecT.CamNecT_Server.domain.alumni.dto;

import CamNecT.CamNecT_Server.global.tag.model.Tag;

import java.util.List;

public record AlumniHomeResponse (
        Long userId,
        String name,
        ProfileCardDto profile,
        List<Tag> tagList
) {
}
