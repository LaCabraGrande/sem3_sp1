package app.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import app.dtos.MovieDTO;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "movie", indexes = {
        @Index(name = "idx_movie_imdb_id", columnList = "imdb_id"),
        @Index(name = "idx_movie_title", columnList = "title"),
        @Index(name = "idx_movie_release_date", columnList = "release_date"),
        @Index(name = "idx_movie_popularity", columnList = "popularity"),
        @Index(name = "idx_movie_original_language", columnList = "original_language"),
        @Index(name = "idx_movie_original_title", columnList = "original_title"),
        @Index(name = "idx_movie_vote_average", columnList = "vote_average")
})
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imdb_id", nullable = false, unique = true) // Unik hvis relevant
    private Long imdbId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "overview", length = 1500)
    private String overview;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "adult")
    private boolean adult;

    @Column(name = "backdrop_path")
    private String backdropPath;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "popularity")
    private double popularity;

    @Column(name = "original_language")
    private String originalLanguage;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "vote_average")
    private double voteAverage;

    @Column(name = "vote_count")
    private int voteCount;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @ToString.Exclude
    @JsonManagedReference
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    @ManyToOne(optional = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "director_id", referencedColumnName = "id")
    @ToString.Exclude
    private Director director;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "movie_actor",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    @JsonManagedReference
    @ToString.Exclude
    @Builder.Default
    private Set<Actor> actors = new HashSet<>();

    // Constructor to convert MovieDTO to Movie entity
    public Movie(MovieDTO movieDTO) {
        this.imdbId = movieDTO.getImdbId();
        this.title = movieDTO.getTitle();
        this.duration = movieDTO.getDuration();
        this.overview = movieDTO.getOverview();
        this.releaseDate = movieDTO.getReleaseDate();
        this.adult = movieDTO.getIsAdult();
        this.backdropPath = movieDTO.getBackdropPath();
        this.posterPath = movieDTO.getPosterPath();
        this.popularity = movieDTO.getPopularity();
        this.originalLanguage = movieDTO.getOriginalLanguage();
        this.originalTitle = movieDTO.getOriginalTitle();
        this.voteAverage = movieDTO.getVoteAverage();
        this.voteCount = movieDTO.getVoteCount();

        // Convert genres from DTO to entity
        if (movieDTO.getGenreIds() != null) {
            this.genres = movieDTO.getGenreIds().stream()
                    .map(genreId -> {
                        Genre genre = new Genre();
                        genre.setId((long) genreId); // Assuming genreId is an integer in DTO and long in the entity
                        return genre;
                    })
                    .collect(Collectors.toSet());
        }

        // Convert actors from DTO to entity
        if (movieDTO.getActors() != null) {
            this.actors = movieDTO.getActors().stream()
                    .map(actorDTO -> {
                        Actor actor = new Actor();
                        actor.setName(actorDTO.getName());
                        return actor;
                    })
                    .collect(Collectors.toSet());
        }

        // Convert director from DTO to entity
        if (movieDTO.getDirector() != null) {
            this.director = new Director();
            this.director.setName(movieDTO.getDirector().getName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(id, movie.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
