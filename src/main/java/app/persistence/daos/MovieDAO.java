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

            // Find existing Movie or create new one
            Movie movie = em.find(Movie.class, dto.getId());
            if (movie == null) {
                movie = new Movie();
            }

            movie.setId(dto.getId()); // Set ID for the movie
            movie.setTitle(dto.getTitle());
            movie.setOverview(dto.getOverview());
            movie.setReleaseDate(dto.getReleaseDate());
            movie.setRating(dto.getRating() != null ? dto.getRating() : 0.0); // Handle null values
            movie.setPosterPath(dto.getPosterPath());
            movie.setVoteCount(dto.getVoteCount());

            // Map genreIds to Genre entities
            Set<Genre> genres = dto.getGenreIds().stream()
                    .map(genreId -> {
                        Genre genre = em.find(Genre.class, genreId);
                        if (genre == null) {
                            throw new IllegalStateException("Genre with ID " + genreId + " not found");
                        }
                        return genre;
                    })
                    .collect(Collectors.toSet());

            movie.setGenres(genres);

            // Use merge instead of persist
            em.merge(movie);

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
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
}
