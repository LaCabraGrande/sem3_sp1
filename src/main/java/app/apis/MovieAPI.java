package app.apis;

import lombok.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieAPI {

    private Long id;
    private Long imdbId;
    private String title;
    private Integer duration;
    private String overview;
    private String releaseDate;
    private boolean adult;
    private String backdropPath;
    private String posterPath;
    private double popularity;
    private String originalLanguage;
    private String originalTitle;
    private double voteAverage;
    private int voteCount;

    // Skuespillere som Strings (navne)
    private Set<String> actors;

    // Genrer som Strings (navne)
    private Set<String> genres;

    // Instruktørens navn
    private String director;
}
