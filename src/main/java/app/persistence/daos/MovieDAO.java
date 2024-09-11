package app.persistence.daos;
import app.persistence.dtos.GenreDTO;
import app.persistence.entities.Genre;
import jakarta.persistence.*;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Movie;
import app.persistence.config.HibernateConfig;
import app.persistence.enums.HibernateConfigState;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MovieDAO implements IDAO<Movie> {

    private static MovieDAO instance;
    private static EntityManagerFactory emf;
    private EntityManager em;

    private MovieDAO() {
        em = emf.createEntityManager();
    }

    public static MovieDAO getInstance(HibernateConfigState state) {
        if (instance == null) {
            emf = HibernateConfig.getEntityManagerFactoryConfig(state, "movie");
            instance = new MovieDAO();
        }
        return instance;
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Override
    public Movie findById(Long id) {
        return em.find(Movie.class, id);
    }

    @Override
    public Movie update(Movie movie) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Movie updatedMovie = em.merge(movie);
            transaction.commit();
            return updatedMovie;
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    @Override
    public void create(MovieDTO dto) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();

            // Opret en ny Movie-entitet
            Movie movie = new Movie();

            // Map værdier fra DTO til Movie-entiteten
            movie.setImdbId(dto.getImdbId());
            movie.setTitle(dto.getTitle());
            movie.setOverview(dto.getOverview());
            movie.setReleaseDate(dto.getReleaseDate());
            movie.setAdult(dto.getIsAdult() != null ? dto.getIsAdult() : false);  // Håndter null-værdier for adult
            movie.setBackdropPath(dto.getBackdropPath());
            movie.setPosterPath(dto.getPosterPath());
            movie.setPopularity(dto.getPopularity() != null ? dto.getPopularity() : 0.0);  // Håndter null-værdier
            movie.setOriginalLanguage(dto.getOriginalLanguage());
            movie.setOriginalTitle(dto.getOriginalTitle());
            movie.setVoteAverage(dto.getVoteAverage() != null ? dto.getVoteAverage() : 0.0);  // Håndter null-værdier
            movie.setVoteCount(dto.getVoteCount() != null ? dto.getVoteCount() : 0);  // Håndter null-værdier

            // Map genreIds fra DTO til Genre-entiteter
            Set<Genre> genres = dto.getGenreIds().stream()
                    .map(genreId -> {
                        Genre genre = em.createQuery("SELECT g FROM Genre g WHERE g.genreId = :genreId", Genre.class)
                                .setParameter("genreId", genreId)
                                .getSingleResult();
                        if (genre == null) {
                            throw new IllegalStateException("Genre with ID " + genreId + " not found");
                        }
                        return genre;
                    })
                    .collect(Collectors.toSet());

            // Sæt genres på Movie-entiteten
            movie.setGenres(genres);

            // Brug persist for at oprette en ny Movie
            em.persist(movie);

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }




    @Override
    public void create(GenreDTO dto) {

    }

    @Override
    public void create(Genre genre) {

    }

    @Override
    public List<Movie> getAllMovies() {
        return em.createQuery("SELECT m FROM Movie m", Movie.class).getResultList();
    }

    public long countMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(m) FROM Movie m", Long.class).getSingleResult();
        }
    }
}
