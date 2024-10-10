package app.persistence.services;

import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Movie;
import java.util.stream.Collectors;
import app.persistence.apis.MovieAPI;
import app.persistence.entities.Actor;
import app.persistence.entities.Genre;

public class MovieConverter {

    // Metode til at konvertere en Movie til en MovieDTO
    public static MovieAPI convertToMovieAPI(Movie movie) {
        return MovieAPI.builder()

                .id(movie.getId())
                .imdbId(movie.getImdbId())
                .title(movie.getTitle())
                .duration(movie.getDuration())
                .overview(movie.getOverview())
                .releaseDate(movie.getReleaseDate())
                .adult(movie.isAdult())
                .backdropPath(movie.getBackdropPath())
                .posterPath(movie.getPosterPath())
                .popularity(movie.getPopularity())
                .originalLanguage(movie.getOriginalLanguage())
                .originalTitle(movie.getOriginalTitle())
                .voteAverage(movie.getVoteAverage())
                .voteCount(movie.getVoteCount())

                // Konverter skuespillere til deres navne
                .actors(movie.getActors().stream()
                        .map(Actor::getName)
                        .collect(Collectors.toSet()))

                // Konverter genrer til deres navne
                .genres(movie.getGenres().stream()
                        .map(Genre::getName)
                        .collect(Collectors.toSet()))

                // Instruktørens navn (hvis der er en instruktør)
                .director(movie.getDirector() != null ? movie.getDirector().getName() : null)
                .build();
    }

    // Metode til at konvertere en MovieDTO til en Movie
    public static Movie convertToMovie(MovieDTO movieDTO) {
        Movie movie = new Movie();
        movie.setId(movieDTO.getDatabaseId());
        movie.setImdbId(movieDTO.getImdbId());
        movie.setTitle(movieDTO.getTitle());
        movie.setDuration(movieDTO.getDuration());
        movie.setOverview(movieDTO.getOverview());
        movie.setReleaseDate(movieDTO.getReleaseDate());
        movie.setAdult(movieDTO.getIsAdult());
        movie.setBackdropPath(movieDTO.getBackdropPath());
        movie.setPosterPath(movieDTO.getPosterPath());
        movie.setPopularity(movieDTO.getPopularity());
        movie.setOriginalLanguage(movieDTO.getOriginalLanguage());
        movie.setOriginalTitle(movieDTO.getOriginalTitle());
        movie.setVoteAverage(movieDTO.getVoteAverage());
        movie.setVoteCount(movieDTO.getVoteCount());

        // Hvis du har skuespillere, genrer eller instruktør, skal du også tilføje dem her.
        // movie.setActors(convertActors(movieDTO.getActors())); // Antag en metode til dette
        // movie.setGenres(convertGenres(movieDTO.getGenres())); // Antag en metode til dette

        return movie;
    }

}



