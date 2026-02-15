package CamNecT.server.domain.community.model;

import CamNecT.server.domain.community.model.enums.BoardCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "boards",
        uniqueConstraints = @UniqueConstraint(name = "uk_boards_code", columnNames = "code")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Boards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 20)
    private BoardCode code; // INFO, QUESTION

    @Column(name = "name", nullable = false, length = 30)
    private String name; // "정보", "질문"

    public static Boards of(BoardCode code, String name) {
        return Boards.builder()
                .code(code)
                .name(name)
                .build();
    }
}
