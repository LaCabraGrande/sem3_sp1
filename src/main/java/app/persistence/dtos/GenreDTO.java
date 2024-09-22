package app.persistence.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenreDTO {
    private Long id;
    private int genreId;
    private String name;
}
