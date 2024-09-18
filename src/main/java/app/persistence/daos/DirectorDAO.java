package app.persistence.daos;

import app.persistence.config.HibernateConfig;
import app.persistence.dtos.DirectorDTO;
import app.persistence.entities.Director;
import app.persistence.entities.Movie;
import app.persistence.enums.HibernateConfigState;
import app.persistence.exceptions.JpaException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectorDAO {

    private static DirectorDAO instance;
    private static EntityManagerFactory emf;
    private EntityManager em;

    private  DirectorDAO() {
        em = emf.createEntityManager();
    }

    public static DirectorDAO getInstance(HibernateConfigState state) {
        if (instance == null) {
            emf = HibernateConfig.getEntityManagerFactoryConfig(state, "movie");
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
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new JpaException("An error occurred while creating director", e);
        }
    }


    public Director findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Director director = em.find(Director.class, id);
            System.out.println("Found director with ID " + id + ": " + director);
            return director;
        } catch (Exception e) {
            e.printStackTrace(); // Udførlige fejlmeddelelser
            throw new JpaException("An error occurred while finding director by ID", e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }


    public Director findDirectorById(Long id) {
        try {
            return em.find(Director.class, id);
        } catch (Exception e) {
            throw new JpaException("An error occurred while finding the director by ID", e);
        }
    }

    public boolean directorExists(Long id) {
        return findDirectorById(id) != null;
    }



    public void update(DirectorDTO dto) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Director director = em.find(Director.class, dto.getId());
            if (director != null) {
                director.setName(dto.getName());
                em.merge(director);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new JpaException("An error occurred while updating director", e);
        } finally {
            em.close();
        }
    }

    public Director findOrCreateDirector(Director director) {
        if (director == null) {
            throw new IllegalArgumentException("Director cannot be null");
        }

        //System.out.println("Attempting to find director with ID: " + director.getId());

        Director foundDirector = null;

        try (EntityManager em = emf.createEntityManager()) {
            // Først, prøv at finde direktøren i databasen
            foundDirector = em.find(Director.class, director.getId());

            if (foundDirector != null) {
                //System.out.println("Director found: " + foundDirector);
                return foundDirector;
            }

            // Hvis ikke fundet, opret en ny direktør
            //System.out.println("Director not found, creating new director: " + director);
            em.getTransaction().begin();
            em.persist(director);
            em.getTransaction().commit();
            //System.out.println("Director created successfully: " + director);
            return director;
        } catch (Exception e) {
            e.printStackTrace(); // Udførlige fejlmeddelelser
            throw new JpaException("An error occurred while finding or creating director", e);
        }
    }



    public void create(Director director) {
        if (director == null) {
            throw new IllegalArgumentException("Director cannot be null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Brug merge i stedet for persist for at undgå problemer med vedhæftede entiteter
            em.merge(director);
            em.getTransaction().commit();
            System.out.println("Director created or updated: " + director);
        } catch (Exception e) {
            e.printStackTrace(); // Udførlige fejlmeddelelser
            throw new JpaException("An error occurred while creating or updating director", e);
        }
    }







    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Director director = em.find(Director.class, id);
            if (director != null) {
                em.remove(director);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new JpaException("An error occurred while deleting director", e);
        } finally {
            em.close();
        }
    }




    public List<DirectorDTO> getAll() {
        EntityManager em = emf.createEntityManager();
        try {
            // Hent alle Director entiteter
            List<Director> directors = em.createQuery("SELECT d FROM Director d", Director.class).getResultList();

            return directors.stream()
                    .map(d -> {
                        // Hent filmene som instruktøren har instrueret
                        List<Movie> movies = em.createQuery("SELECT m FROM Movie m WHERE m.director.id = :directorId", Movie.class)
                                .setParameter("directorId", d.getId())
                                .getResultList();

                        // Hent film-ID'er og film-titler
                        Set<Long> movieIds = movies.stream()
                                .map(Movie::getId)
                                .collect(Collectors.toSet());
                        Set<String> movieTitles = movies.stream()
                                .map(Movie::getTitle)
                                .collect(Collectors.toSet());

                        // Byg DirectorDTO med film-ID'er og film-titler
                        return DirectorDTO.builder()
                                .id(d.getId())
                                .name(d.getName())
                                .movieIds(movieIds)
                                .movieTitles(movieTitles)
                                .build();
                    })
                    .toList();
        } finally {
            em.close();
        }
    }

}
