package app.persistence.services;

import app.persistence.daos.MovieDAO;
import app.persistence.entities.Movie;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import app.persistence.apis.MovieAPI;

public class MovieService {

    private final MovieDAO movieDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public MovieService(MovieDAO movieDAO) {
        this.movieDAO = movieDAO;
    }

    // Returnerer en liste af film med en rating over 7
    public List<Movie> getMoviesWithRatingAbove(double rating) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getVoteAverage() > rating)
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film som har genren "Krig"
    public List<Movie> getMoviesByGenre(String genreName) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getGenres().stream()
                        .anyMatch(genre -> genre.getName().equalsIgnoreCase(genreName)))
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film fra 2024
    public List<Movie> getMoviesFromYear(int year) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getReleaseDate() != null && movie.getReleaseDate().startsWith(String.valueOf(year)))
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film med et minimum antal stemmer
    public List<Movie> getMoviesWithMinimumVotes(int minVoteCount) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getVoteCount() >= minVoteCount)
                .collect(Collectors.toList());
    }

    // Metode til at konvertere en liste af Movie til JSON
    public String convertMoviesToJson(List<MovieAPI> movies) throws Exception {
        return mapper.writeValueAsString(movies);
    }
}
