package app.dtos;

import app.entities.Genre;
import app.entities.Movie;
import lombok.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MovieDTO {
    private Long databaseId;
    private Long imdbId;
    private String title;
    private Integer duration;
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

    private Set<ActorDTO> actors;
    private DirectorDTO director;

    public MovieDTO(Movie movie) {
        this.databaseId = movie.getId();
        this.imdbId = movie.getImdbId();
        this.title = movie.getTitle();
        this.duration = movie.getDuration();
        this.overview = movie.getOverview();
        this.releaseDate = movie.getReleaseDate();
        this.isAdult = movie.isAdult();
        this.backdropPath = movie.getBackdropPath();
        this.posterPath = movie.getPosterPath();
        this.popularity = movie.getPopularity();
        this.originalLanguage = movie.getOriginalLanguage();
        this.originalTitle = movie.getOriginalTitle();
        this.voteAverage = movie.getVoteAverage();
        this.voteCount = movie.getVoteCount();

        // Genrer
        if (movie.getGenres() != null) {
            this.genreIds = movie.getGenres().stream()
                    .filter(Objects::nonNull)
                    .map(Genre::getGenreId)
                    .collect(Collectors.toSet());

            this.genreNames = movie.getGenres().stream()
                    .filter(Objects::nonNull)
                    .map(Genre::getName)
                    .collect(Collectors.toList());
        }

        // Skuespillere
        if (movie.getActors() != null) {
            this.actors = movie.getActors().stream()
                    .filter(Objects::nonNull)
                    .map(ActorDTO::new)
                    .collect(Collectors.toSet());
        }

        // Instruktør
        if (movie.getDirector() != null) {
            this.director = new DirectorDTO(movie.getDirector());
        }
    }
}
