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
            List<MovieDTO> movieDTOList = filmFetcher.fetchDanishMovies();
            System.out.println("Fetched " + movieDTOList.size() + " movies");

            for (MovieDTO movieDTO : movieDTOList) {
                try {
                    saveMovieWithDetails(movieDTO);
                } catch (Exception innerException) {
                    System.err.println("Fejl når jeg gemmer filmen: " + innerException.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fejl når jeg henter en film: " + e.getMessage());
            throw new JpaException("Fejl når jeg henter en film: ", e);
        }
    }

    private void saveMovieWithDetails(MovieDTO movieDTO) {
        // Her undersøger jeg om filmen allerede eksisterer i databasen
        Movie existingMovie = movieDAO.findByTitle(movieDTO.getTitle());

        if (existingMovie == null) {
            Set<Integer> genreIds = movieDTO.getGenreIds();
            List<String> genreNames = filmFetcher.getGenreNames(genreIds);

            movieDTO.setGenreNames(genreNames);

            DirectorDTO directorDTO = handleDirector(movieDTO);
            movieDTO.setDirector(directorDTO);

            Set<ActorDTO> actorDTOs = handleActors(movieDTO);
            movieDTO.setActors(actorDTOs);

            // Konverterer MovieDTO til Movie entity
            Movie movie = convertToEntity(movieDTO);

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

        Set<Genre> genres = genreDAO.findGenresByIds(movieDTO.getGenreIds());
        movie.setGenres(genres);

        Set<Actor> actors = new HashSet<>();
        for (ActorDTO actorDTO : movieDTO.getActors()) {

            // Opretter en ny Actor entity
            Actor newActor = new Actor();
            newActor.setId(actorDTO.getId());
            newActor.setName(actorDTO.getName());
            newActor.setMovieIds(actorDTO.getMovieIds()); // Set movieIds
            newActor.setMovieTitles(actorDTO.getMovieTitles()); // Set movieTitles

            actors.add(newActor);
        }
        movie.setActors(actors);

        // Konverterer DirectorDTO til Director entity
        Director director = convertToDirectorEntity(movieDTO.getDirector());
        movie.setDirector(director);

        return movie;
    }

    private DirectorDTO handleDirector(MovieDTO movieDTO) {
        // Undsøger her om der allerede er en instruktør sat
        if (movieDTO.getDirector() != null) {
            return movieDTO.getDirector();
        }
        return new DirectorDTO();
    }

    private Set<ActorDTO> handleActors(MovieDTO movieDTO) {
        // Kontrollér om actors allerede er sat
        if (movieDTO.getActors() != null && !movieDTO.getActors().isEmpty()) {
            return movieDTO.getActors();
        }

        // Hvis der ikke er skuespillere tilknyttet, returner jeg her et tomt sæt
        System.out.println("Ingen skuespillere fundet, returnerer et tomt sæt.");
        return new HashSet<>();
    }

    private Director convertToDirectorEntity(DirectorDTO directorDTO) {
        Director director = new Director();
        director.setId(directorDTO.getId());
        director.setName(directorDTO.getName());
        return director;
    }
}
