package app.persistence.daos;

import app.persistence.config.HibernateConfig;
import app.persistence.dtos.ActorDTO;
import app.persistence.entities.Actor;
import app.persistence.enums.HibernateConfigState;
import app.persistence.exceptions.JpaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.stream.Collectors;

public class ActorDAO {

    private static ActorDAO instance;
    private static EntityManagerFactory emf;
    private EntityManager em;

    private  ActorDAO() {
        em = emf.createEntityManager();
    }

    public static ActorDAO getInstance(HibernateConfigState state) {
        if (instance == null) {
            emf = HibernateConfig.getEntityManagerFactoryConfig(state, "movie");
            instance = new ActorDAO();
        }
        return instance;
    }


    public void create(Actor actor) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(actor);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new JpaException("An error occurred while creating actor", e);
        }
    }



    public Actor findById(Long id) {

        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Actor.class, id); // Return an Actor, not ActorDTO
        } catch (Exception e) {
            throw new JpaException("An error occurred while finding actor", e);
        }
    }



    public void update(ActorDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Actor actor = em.find(Actor.class, dto.getId());
            if (actor != null) {
                actor.setName(dto.getName());
                em.merge(actor);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new JpaException("An error occurred while updating actor", e);
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Actor actor = em.find(Actor.class, id);
            if (actor != null) {
                em.remove(actor);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new JpaException("An error occurred while deleting actor", e);
        } finally {
            em.close();
        }
    }

    public List<ActorDTO> getAll() {
        EntityManager em = emf.createEntityManager();
        try {
            List<Actor> actors = em.createQuery("SELECT a FROM Actor a", Actor.class).getResultList();
            return actors.stream()
                    .map(a -> ActorDTO.builder()
                            .id(a.getId())
                            .name(a.getName())
                            .build())
                    .collect(Collectors.toList()); // Hvis du bruger Java 8 eller nyere
        } finally {
            em.close();
        }
    }

}
