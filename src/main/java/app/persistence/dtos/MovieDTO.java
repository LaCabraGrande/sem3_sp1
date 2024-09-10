package app.persistence.dtos;

import lombok.*;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Setter
public class MovieDTO {

    private Long id;
    private String title;
    private String overview;
    private String releaseDate;
    private Double rating;
    private Boolean adult;
    private String backdropPath;
    private String posterPath;
    private Double popularity;
    private String originalLanguage;
    private String originalTitle;
    private Double voteAverage;
    private Integer voteCount;
    private Set<Integer> genreIds;  // Set of genre IDs
    private List<String> genreNames; // Giv genreNames som List<String>

}
