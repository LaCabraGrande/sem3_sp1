package app.persistence.daos;

import app.persistence.config.HibernateConfig;
import app.persistence.enums.HibernateConfigState;
import app.persistence.exceptions.JpaException;
import jakarta.persistence.*;
import app.persistence.entities.Genre;
import java.util.stream.Collectors;
import java.util.Set;

public class GenreDAO {

    private static GenreDAO instance;
    private static EntityManagerFactory emf;
    private EntityManager em;

    public GenreDAO() {
        em = emf.createEntityManager();
    }

    public static GenreDAO getInstance(HibernateConfigState state) {
        if (instance == null) {
            emf = HibernateConfig.getEntityManagerFactoryConfig(state, "genre");
            instance = new GenreDAO();
        }
        return instance;
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    public Genre findById(Long id)
    {
        return em.find(Genre.class, id);
    }

    public Genre update(Genre genre) {
        EntityTransaction transaction = em.getTransaction();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Genre updatedGenre = em.merge(genre);
            em.getTransaction().commit();
            return updatedGenre;
        } catch (Exception e) {
            transaction.rollback();
            throw new JpaException("An error occurred while updating genre", e);
        }
    }

    public void create(Genre genre) {
        EntityTransaction transaction = em.getTransaction();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(genre);
            em.getTransaction().commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new JpaException("An error occurred while creating genre", e);
        }
    }

    public Set<Genre> findGenresByIds(Set<Integer> genreIds) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT g FROM Genre g WHERE g.genreId IN :ids", Genre.class)
                    .setParameter("ids", genreIds)
                    .getResultStream() // Brug getResultStream for at få en stream af resultater
                    .collect(Collectors.toSet()); // Saml resultaterne i et Set
        } catch (Exception e) {
            throw new JpaException("An error occurred while fetching genres by ids", e);
        }
    }

    public long countGenres() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT COUNT(g) FROM Genre g", Long.class).getSingleResult();
        }
    }
}
