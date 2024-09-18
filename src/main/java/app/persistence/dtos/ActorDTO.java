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

    // Hvis du vil vise en liste af film, hvor skuespilleren medvirker:
    private Set<Long> movieIds;
    private Set<String> movieTitles;
}

