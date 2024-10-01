package app.persistence.services;

import app.persistence.daos.MovieDAO;
import app.persistence.entities.Movie;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import app.persistence.apis.MovieAPI;

public class MovieService {

    private final MovieDAO movieDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public MovieService(MovieDAO movieDAO) {
        this.movieDAO = movieDAO;
    }

    // Returnerer en liste af film med en angiven rating
    public List<Movie> getMoviesWithRatingAbove(double rating) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getVoteAverage() > rating)
                .collect(Collectors.toList());
    }

    // Returnerer en liste af Movies af en angiven genre
    public List<Movie> getMoviesByGenre(String genreName) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getGenres().stream()
                        .anyMatch(genre -> genre.getName().equalsIgnoreCase(genreName)))
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film fra det angivne år
    public List<Movie> getMoviesFromYear(int year) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getReleaseDate() != null && movie.getReleaseDate().startsWith(String.valueOf(year)))
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film med et angivet minimum af stemmer
    public List<Movie> getMoviesWithMinimumVotes(int minVoteCount) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getVoteCount() >= minVoteCount)
                .collect(Collectors.toList());
    }

    // Konverterer en liste af MovieAPIs til en JSON-String som returneres
    public String convertMoviesToJson(List<MovieAPI> movieAPIs) throws Exception {
        try {
            return mapper.writeValueAsString(movieAPIs);
        } catch (Exception e) {
            throw new Exception("Fejl ved konvertering til JSON: " + e.getMessage());
        }
    }


}
