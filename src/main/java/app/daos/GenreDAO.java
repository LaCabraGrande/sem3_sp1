package app.daos;

import app.exceptions.JpaException;
import jakarta.persistence.*;
import app.entities.Genre;
import java.util.stream.Collectors;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenreDAO {

    private static GenreDAO instance;
    private static EntityManagerFactory emf;
    private static final Logger logger = LoggerFactory.getLogger(GenreDAO.class);

    public static GenreDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new GenreDAO();
        }
        return instance;
    }

    public Genre findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Genre.class, id);
        } catch (Exception e) {
            logger.error("Fejl ved hentning af genre med id {}", id, e);
            throw new JpaException("Fejl ved hentning af genre med id: " + id, e);
        }
    }

    public Genre update(Genre genre) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Genre updatedGenre = em.merge(genre);
                transaction.commit();
                return updatedGenre;
            } catch (Exception e) {
                if (transaction.isActive()) transaction.rollback();
                logger.error("Fejl ved opdatering af genre: {}", genre, e);
                throw new JpaException("Fejl ved opdatering af genre", e);
            }
        }
    }

    public void create(Genre genre) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                em.persist(genre);
                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) transaction.rollback();
                logger.error("Fejl ved oprettelse af genre: {}", genre, e);
                throw new JpaException("Fejl ved oprettelse af genre", e);
            }
        }
    }

    public Set<Genre> findGenresByIds(Set<Integer> genreIds) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT g FROM Genre g WHERE g.genreId IN :ids", Genre.class)
                    .setParameter("ids", genreIds)
                    .getResultStream()
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Fejl ved hentning af genrer med IDs: {}", genreIds, e);
            throw new JpaException("Fejl ved hentning af genrer med de angivne IDs", e);
        }
    }

    public long countGenres() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(g) FROM Genre g", Long.class).getSingleResult();
        } catch (Exception e) {
            logger.error("Fejl ved optælling af genrer", e);
            throw new JpaException("Fejl ved optælling af genrer", e);
        }
    }
}
