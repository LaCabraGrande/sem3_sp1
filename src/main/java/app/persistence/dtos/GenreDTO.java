package app.persistence.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenreDTO {
    private Long id;        // Dette kan være et genereret ID, men det bruges her ikke
    private int genreId;    // Dette er genre ID'et
    private String name;
}
