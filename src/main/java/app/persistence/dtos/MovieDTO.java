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
    private Long databaseId;
    private Long imdbId;
    private String title;
    private String overview;
    private String releaseDate;
    private Boolean isAdult;
    private String backdropPath;
    private String posterPath;
    private Double popularity;
    private String originalLanguage;
    private String originalTitle;
    private Double voteAverage;
    private Integer voteCount;
    private Set<Integer> genreIds;
    private List<String> genreNames;
}
