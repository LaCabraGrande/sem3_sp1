package app.services;

import app.config.HibernateConfig;
import app.daos.ActorDAO;
import app.daos.DirectorDAO;
import app.daos.GenreDAO;
import app.daos.MovieDAO;
import app.dtos.ActorDTO;
import app.dtos.DirectorDTO;
import app.dtos.MovieDTO;
import app.entities.Actor;
import app.entities.Director;
import app.entities.Genre;
import app.entities.Movie;
import app.exceptions.JpaException;
import app.fetcher.FilmFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilmService {
    private final FilmFetcher filmFetcher;
    private final MovieDAO movieDAO;
    private final GenreDAO genreDAO;
    private final ActorDAO actorDAO;
    private final DirectorDAO directorDAO;
    private final ObjectMapper mapper = new ObjectMapper();
    private final EntityManagerFactory emf;

    public FilmService(FilmFetcher filmFetcher) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.movieDAO = MovieDAO.getInstance(emf);
        this.actorDAO = ActorDAO.getInstance(emf);
        this.directorDAO = DirectorDAO.getInstance(emf);
        this.genreDAO = GenreDAO.getInstance(emf);
        this.filmFetcher = filmFetcher;
        this.emf = emf;
    }

    public void fetchAndSaveMovies() {
        try {
            //List<MovieDTO> movieDTOList = filmFetcher.fetchDanishMovies();

            List<MovieDTO> movieDTOList = filmFetcher.fetchMoviesFromLastTenYears();
            System.out.println("Fetched " + movieDTOList.size() + " movies");

            // Sæt genre-navne på filmene før vi gemmer dem
            for (MovieDTO dto : movieDTOList) {
                Set<Integer> genreIds = dto.getGenreIds();
                List<String> genreNames = filmFetcher.getGenreNames(genreIds);
                dto.setGenreNames(genreNames);
            }

            // Brug ny create-metode i DAO til at gemme alle film på én gang
            movieDAO.create(movieDTOList);

            System.out.println("Movies saved......");
        } catch (Exception e) {
            System.err.println("Fejl når jeg henter eller gemmer film: " + e.getMessage());
            throw new JpaException("Fejl når jeg henter eller gemmer film", e);
        }
    }


    private Movie convertToEntity(MovieDTO movieDTO) {
        Movie movie = new Movie();
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

        Set<Genre> genres = genreDAO.findGenresByIds(movieDTO.getGenreIds());
        movie.setGenres(genres);

        Set<Actor> actors = new HashSet<>();
        for (ActorDTO actorDTO : movieDTO.getActors()) {
            Actor newActor = new Actor();
            newActor.setId(actorDTO.getId());
            newActor.setName(actorDTO.getName());
            newActor.setMovieIds(actorDTO.getMovieIds());
            newActor.setMovieTitles(actorDTO.getMovieTitles());

            actors.add(newActor);
        }
        movie.setActors(actors);
        Director director = convertToDirectorEntity(movieDTO.getDirector());
        movie.setDirector(director);
        return movie;
    }

    private DirectorDTO handleDirector(MovieDTO movieDTO) {
        if (movieDTO.getDirector() != null) {
            return movieDTO.getDirector();
        }
        return new DirectorDTO();
    }

    private Set<ActorDTO> handleActors(MovieDTO movieDTO) {
        Set<ActorDTO> actorsToReturn = new HashSet<>();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Her undersøger jeg om der er skuespillere i skuespillerlisten i movieDTO
            if (movieDTO.getActors() != null && !movieDTO.getActors().isEmpty()) {
                for (ActorDTO actorDTO : movieDTO.getActors()) {
                    // Her undersøger jeg om skuespilleren allerede er tilknyttet filmen
                    TypedQuery<Long> query = em.createQuery(
                            "SELECT COUNT(a) FROM Movie m JOIN m.actors a WHERE m.id = :movieId AND a.id = :actorId", Long.class);
                    query.setParameter("movieId", movieDTO.getDatabaseId());
                    query.setParameter("actorId", actorDTO.getId());

                    Long count = query.getSingleResult();

                    if (count == 0) {
                        // Hvis skuespilleren ikke allerede er i relationen, tilføjer jeg skuespilleren til sættet
                        actorsToReturn.add(actorDTO);
                    } else {
                        System.out.println("Skuespilleren " + actorDTO.getName() + " er allerede tilknyttet filmen.");
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Der opstod en fejl ved behandling af skuespillere: " + e.getMessage());
            e.printStackTrace();
        }

        return actorsToReturn;
    }

    private Director convertToDirectorEntity(DirectorDTO directorDTO) {
        Director director = new Director();
        director.setId(directorDTO.getId());
        director.setName(directorDTO.getName());
        return director;
    }

    public List<Actor> getActorsByMovieTitle(String title) {
        EntityManager em = emf.createEntityManager();
        List<Actor> actors;
        try {
            String jpql = "SELECT a FROM Actor a JOIN a.movies m WHERE m.originalTitle = :title";
            TypedQuery<Actor> query = em.createQuery(jpql, Actor.class);
            query.setParameter("title", title);
            actors = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Fejl ved hentning af skuespillere for film med titel: " + title, e);
        } finally {
            em.close();
        }
        return actors;
    }

    public Director getDirectorByMovieTitle(String title) {
        EntityManager em = emf.createEntityManager();
        Director director;
        try {
            String jpql = "SELECT m.director FROM Movie m WHERE m.originalTitle = :title";
            TypedQuery<Director> query = em.createQuery(jpql, Director.class);
            query.setParameter("title", title);
            director = query.getResultStream().findFirst().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Fejl ved hentning af instruktør for film med titel: " + title, e);
        } finally {
            em.close();
        }
        return director;
    }

    public List<Movie> findMoviesByActor(String actorName) {
        EntityManager em = emf.createEntityManager();
        List<Movie> movies;
        try {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT m FROM Movie m JOIN m.actors a WHERE a.name = :actorName", Movie.class);
            query.setParameter("actorName", actorName);
            movies = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der opstod en fejl ved hentningen af film for skuespiller: " + actorName, e);
        } finally {
            em.close();
        }
        return movies;
    }
    // Returnerer en liste af film instrueret af en angiven instruktør
    public List<Movie> getMoviesByDirector(String directorName) throws Exception {
        try {
            return movieDAO.getMoviesByDirector(directorName);
        } catch (Exception e) {
            throw new Exception("Fejl ved hentning af film: " + e.getMessage());
        }
    }
}
