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
}
