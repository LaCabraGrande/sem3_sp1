package app.persistence.services;

import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.dtos.MovieDTO;
import app.persistence.daos.MovieDAO;
import app.persistence.daos.GenreDAO;
import app.persistence.daos.ActorDAO;
import app.persistence.daos.DirectorDAO;
import app.persistence.dtos.ActorDTO;
import app.persistence.dtos.DirectorDTO;
import app.persistence.exceptions.JpaException;
import app.persistence.fetcher.FilmFetcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilmService {
    private final FilmFetcher filmFetcher;
    private final MovieDAO movieDAO;
    private final GenreDAO genreDAO;
    private final ActorDAO actorDAO;
    private final DirectorDAO directorDAO;

    public FilmService(FilmFetcher filmFetcher, MovieDAO movieDAO, GenreDAO genreDAO, ActorDAO actorDAO, DirectorDAO directorDAO) {
        this.filmFetcher = filmFetcher;
        this.movieDAO = movieDAO;
        this.genreDAO = genreDAO;
        this.actorDAO = actorDAO;
        this.directorDAO = directorDAO;
    }

    public void fetchAndSaveMovies() {
        try {
            // Fetch movies from FilmFetcher
            List<MovieDTO> movieDTOList = filmFetcher.fetchDanishMovies();
            System.out.println("Fetched " + movieDTOList.size() + " movies");

            // Process each movie DTO
            for (MovieDTO movieDTO : movieDTOList) {
                try {
                    saveMovieWithDetails(movieDTO);
                } catch (Exception innerException) {
                    // Log individual movie save errors
                    System.err.println("Error saving movie: " + innerException.getMessage());
                }
            }
        } catch (Exception e) {
            // Log the error that occurred during fetching
            System.err.println("Error fetching movies: " + e.getMessage());
            throw new JpaException("Error fetching and saving movies", e);
        }
    }

    private void saveMovieWithDetails(MovieDTO movieDTO) {
        // Check if the movie already exists in the database
        Movie existingMovie = movieDAO.findByTitle(movieDTO.getTitle());

        if (existingMovie == null) {
            // Handle genreIds and update genreNames
            Set<Integer> genreIds = movieDTO.getGenreIds();
            List<String> genreNames = filmFetcher.getGenreNames(genreIds);

            movieDTO.setGenreNames(genreNames);

            // Handle Director
            DirectorDTO directorDTO = handleDirector(movieDTO);
            movieDTO.setDirector(directorDTO);

            // Handle Actors
            Set<ActorDTO> actorDTOs = handleActors(movieDTO);
            movieDTO.setActors(actorDTOs);

            // Convert MovieDTO to Movie entity
            Movie movie = convertToEntity(movieDTO);

            // Save Movie entity to the database via DAO

            movieDAO.create(movie);
        }
    }

    private Movie convertToEntity(MovieDTO movieDTO) {
        Movie movie = new Movie();
        movie.setImdbId(movieDTO.getImdbId());
        movie.setTitle(movieDTO.getTitle());
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

        // Handle genres
        Set<Genre> genres = genreDAO.findGenresByIds(movieDTO.getGenreIds());
        movie.setGenres(genres);

        // Handle actors
        Set<Actor> actors = new HashSet<>();
        for (ActorDTO actorDTO : movieDTO.getActors()) {

            // Create new Actor entity
            Actor newActor = new Actor();
            newActor.setId(actorDTO.getId());
            newActor.setName(actorDTO.getName());
            newActor.setMovieIds(actorDTO.getMovieIds()); // Set movieIds
            newActor.setMovieTitles(actorDTO.getMovieTitles()); // Set movieTitles

            actors.add(newActor);
        }
        movie.setActors(actors);

        // Convert DirectorDTO to Director entity
        Director director = convertToDirectorEntity(movieDTO.getDirector());
        movie.setDirector(director);

        return movie;
    }



    private DirectorDTO handleDirector(MovieDTO movieDTO) {
        // Handle Director if available in DTO
        if (movieDTO.getDirector() != null) {
            return movieDTO.getDirector();
        }
        return new DirectorDTO(); // Return a new empty DirectorDTO if not present
    }

    private Set<ActorDTO> handleActors(MovieDTO movieDTO) {
        // Kontrollér om actors allerede er sat
        if (movieDTO.getActors() != null && !movieDTO.getActors().isEmpty()) {
            //System.out.println("Actors already set: " + movieDTO.getActors());
            return movieDTO.getActors();
        }

        // Hvis der ikke er skuespillere, returner et tomt sæt
        System.out.println("No actors found, returning empty set.");
        return new HashSet<>();
    }


    private Director convertToDirectorEntity(DirectorDTO directorDTO) {
        Director director = new Director();
        director.setId(directorDTO.getId());
        director.setName(directorDTO.getName());
        // Hvis der er flere felter, tilføj dem her
        return director;
    }

}
