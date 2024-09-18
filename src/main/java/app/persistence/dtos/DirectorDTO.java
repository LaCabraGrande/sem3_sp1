package app.persistence.dtos;

import lombok.*;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DirectorDTO {
    private Long id;
    private String name;

    // Hvis du vil vise en liste af film, som instruktøren har instrueret:
    private Set<Long> movieIds;
    private Set<String> movieTitles;
}
