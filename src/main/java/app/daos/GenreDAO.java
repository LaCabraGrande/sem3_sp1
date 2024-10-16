package app.daos;

import app.exceptions.JpaException;
import jakarta.persistence.*;
import app.entities.Genre;
import java.util.stream.Collectors;
import java.util.Set;

public class GenreDAO {

    private static GenreDAO instance;
    private static EntityManagerFactory emf;

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
            throw new JpaException("An error occurred while fetching genre by id", e);
        }
    }

    public Genre update(Genre genre) {
         try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                em.getTransaction().begin();
                Genre updatedGenre = em.merge(genre);
                em.getTransaction().commit();
                return updatedGenre;
            } catch (Exception e) {
                transaction.rollback();
                throw new JpaException("An error occurred while updating genre", e);
            }
        }
    }

    public void create(Genre genre) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                em.getTransaction().begin();
                em.persist(genre);
                em.getTransaction().commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new JpaException("An error occurred while creating genre", e);
            }
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
