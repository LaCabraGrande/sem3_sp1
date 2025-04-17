package app.services;

import app.config.HibernateConfig;
import app.daos.MovieDAO;
import app.dtos.MovieDTO;
import app.entities.Movie;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import app.apis.MovieAPI;
import jakarta.persistence.EntityManagerFactory;

public class MovieService {
    private MovieDAO movieDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public MovieService() {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.movieDAO = MovieDAO.getInstance(emf);
    }


    public List<MovieDTO> getAllMovies() {

        return movieDAO.getAllMovies().stream()
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    public List<MovieAPI> getAllMoviesByPageAndSize(int page, int size) {
        List<Movie> allMovies = movieDAO.getMovies(page, size);
        List<MovieAPI> movieAPIS = allMovies.stream()
                .map(MovieConverter::convertToMovieAPI)
                .toList();
        return movieAPIS;

    }

    // Returnerer en liste af film med en angiven rating
//    public List<MovieDTO> getMoviesByRating(double rating) {
//        return movieDAO.getAllMovies().stream()
//                .filter(movie -> movie.getVoteAverage() == rating)
//                .map(MovieDTO::new)
//                .collect(Collectors.toList());
//    }

    public List<MovieDTO> getMoviesByRating(double rating) {
        return movieDAO.getAllMoviesWithRelations().stream()
                .filter(movie -> movie.getVoteAverage() == rating)
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }


    public List<MovieDTO> getMoviesByGenre(String genreName) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getGenres().stream()
                        .anyMatch(genre -> genre.getName().equalsIgnoreCase(genreName)))
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
        MovieDTO movieDTO = movieDAO.findByImdbId(Long.valueOf(imdbId));
        return movieDTO;
    }

    public List<MovieDTO> getMoviesByInstructor(String instructor) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getDirector() != null && movie.getDirector().getName() != null
                        && movie.getDirector().getName().equals(instructor))
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    public List<MovieDTO> getMoviesByActor(String actor) {
        return movieDAO.getAllMovies().stream()
                .filter(movie -> movie.getActors() != null &&
                        movie.getActors().stream().anyMatch(actorDTO -> actorDTO.getName().equalsIgnoreCase(actor)))
                .map(movie -> {
                    // Kontroller for null og initialiser hvis nødvendigt
                    if (movie.getGenres() == null) {
                        movie.setGenres(new HashSet<>()); // eller returner en tom set
                    }
                    return new MovieDTO(movie);
                })
                .collect(Collectors.toList());
    }

//    public List<MovieDTO> getMoviesByTitle(String title) {
//        // Hent filmene fra DAO
//        List<Movie> movies = movieDAO.getMoviesByTitle(title);
//
//        // Tjekker om listen er null eller tom
//        if (movies == null || movies.isEmpty()) {
//            // Log en besked eller håndter tilfælde uden fundne film
//            System.out.println("\nIngen film fundet med titlen: " + title+"\n");
//            return Collections.emptyList(); // Returnerer en tom liste
//        }
//
//        // Konverterer Movie objekter til MovieDTO
//        return movies.stream()
//                .map(MovieDTO::new)
//                .collect(Collectors.toList());
//    }

    // Konverterer en liste af MovieAPIs til en JSON-String som returneres
    public String convertMoviesToJson(List<MovieAPI> movieAPIs) throws Exception {
        try {
            return mapper.writeValueAsString(movieAPIs);
        } catch (Exception e) {
            throw new Exception("Fejl ved konvertering til JSON: " + e.getMessage());
        }
    }
}
