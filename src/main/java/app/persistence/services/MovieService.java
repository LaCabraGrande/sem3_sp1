package app.persistence.services;

import app.persistence.daos.MovieDAO;
import app.persistence.dtos.MovieDTO;
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

    public List<MovieDTO> getAllMovies() {

        return movieDAO.getAllMovies().stream()
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film med en angiven rating
    public List<MovieDTO> getMoviesByRating(double rating) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getVoteAverage() == rating)
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    // Returnerer en liste af Movies af en angiven genre
    public List<MovieDTO> getMoviesByGenre(String genreName) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getGenres().contains(genreName))
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film fra det angivne år
    public List<MovieDTO> getMoviesFromYear(int year) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> {
                    // Hent de første 4 tegn fra releaseDate som et år
                    String releaseDate = movie.getReleaseDate();
                    if (releaseDate != null && releaseDate.length() >= 4) {
                        int movieYear = Integer.parseInt(releaseDate.substring(0, 4));
                        return movieYear == year;
                    }
                    return false;
                })
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    // Returnerer en liste af film med et angivet minimum af stemmer
    public List<MovieDTO> getMoviesWithMinimumVotes(int minVoteCount) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getVoteCount() >= minVoteCount)
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    public MovieDTO getMovieByImdbId(String imdbId) {
        Movie movie = movieDAO.findByImdbId(Long.valueOf(imdbId));
        if (movie != null) {
            return new MovieDTO(movie);
        }
        return null;
    }

    public List<MovieDTO> getMoviesByInstructor(String instructor) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getDirector() != null && movie.getDirector().getName().equals(instructor))
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    public List<MovieDTO> getMoviesByActor(String actor) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getActors().stream().anyMatch(actorDTO -> actorDTO.getName().equals(actor)))
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    public List<MovieDTO> getMoviesByTitle(String title) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getTitle().toLowerCase().contains(title.toLowerCase()))
                .map(MovieDTO::new)
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
