package app.persistence.dtos;

import lombok.*;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ActorDTO {
    private Long id;
    private String name;

    private Set<Long> movieIds;
    private Set<String> movieTitles;
}

