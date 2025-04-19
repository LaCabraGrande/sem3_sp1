package app.daos;

import app.dtos.DirectorDTO;
import app.entities.Director;
import app.entities.Movie;
import app.exceptions.JpaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectorDAO {

    private static final Logger logger = LoggerFactory.getLogger(DirectorDAO.class);
    private static DirectorDAO instance;
    private static EntityManagerFactory emf;

    public static DirectorDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new DirectorDAO();
        }
        return instance;
    }

    public void create(DirectorDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Director director = new Director();
            director.setId(dto.getId());
            director.setName(dto.getName());
            em.persist(director);
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Fejl under oprettelse af en instruktør", e);
            throw new JpaException("Der opstod en fejl under oprettelse af en instruktør", e);
        }
    }

    public Director findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id kan ikke være null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Director.class, id);
        } catch (Exception e) {
            logger.error("Fejl under søgning efter en instruktør med ID: {}", id, e);
            throw new JpaException("Der opstod en fejl under søgning efter en instruktør", e);
        }
    }

    public Director findDirectorByName(String name) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT d FROM Director d WHERE d.name = :name", Director.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (Exception e) {
            logger.error("Fejl under søgning efter en instruktør med navn: {}", name, e);
            throw new JpaException("Der opstod en fejl under søgning efter en instruktør", e);
        }
    }

    public Director findDirectorById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Director.class, id);
        } catch (Exception e) {
            logger.error("Fejl under søgning efter en instruktør med ID: {}", id, e);
            throw new JpaException("Der opstod en fejl under søgning efter en instruktør", e);
        }
    }

    public boolean directorExists(Long id) {
        return findDirectorById(id) != null;
    }

    public void update(DirectorDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Director director = em.find(Director.class, dto.getId());
            if (director != null) {
                director.setName(dto.getName());
                em.merge(director);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Fejl under opdatering af en instruktør", e);
            throw new JpaException("Der opstod en fejl under opdatering af en instruktør", e);
        }
    }

    public Director findOrCreateDirector(Director director) {
        if (director == null) {
            throw new IllegalArgumentException("En instruktør kan ikke være null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            Director foundDirector = em.find(Director.class, director.getId());
            if (foundDirector != null) {
                return foundDirector;
            }

            em.getTransaction().begin();
            em.persist(director);
            em.getTransaction().commit();
            return director;
        } catch (Exception e) {
            logger.error("Fejl under oprettelse af en instruktør", e);
            throw new JpaException("Der opstod en fejl under oprettelse af en instruktør", e);
        }
    }

    public void create(Director director) {
        if (director == null) {
            throw new IllegalArgumentException("En instruktør kan ikke være null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.merge(director);
            em.getTransaction().commit();
        } catch (Exception e) {
            logger.error("Fejl under merge/persist af en instruktør", e);
            throw new JpaException("Der opstod en fejl under merge eller persist af en instruktør", e);
        }
    }

    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            Director director = em.find(Director.class, id);
            if (director != null) {
                em.remove(director);
            }
            transaction.commit();
        } catch (Exception e) {
            logger.error("Fejl under sletning af instruktør med ID: {}", id, e);
            throw new JpaException("Der opstod en fejl under sletning af en instruktør", e);
        }
    }

    public List<DirectorDTO> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Director> directors = em.createQuery("SELECT d FROM Director d", Director.class).getResultList();

            return directors.stream()
                    .map(d -> {
                        List<Movie> movies = em.createQuery("SELECT m FROM Movie m WHERE m.director.id = :directorId", Movie.class)
                                .setParameter("directorId", d.getId())
                                .getResultList();

                        Set<Long> movieIds = movies.stream().map(Movie::getId).collect(Collectors.toSet());
                        Set<String> movieTitles = movies.stream().map(Movie::getTitle).collect(Collectors.toSet());

                        return DirectorDTO.builder()
                                .id(d.getId())
                                .name(d.getName())
                                .movieIds(movieIds)
                                .movieTitles(movieTitles)
                                .build();
                    })
                    .toList();
        }
    }
}
