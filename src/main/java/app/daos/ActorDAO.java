package app.daos;

import app.dtos.ActorDTO;
import app.entities.Actor;
import app.exceptions.JpaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ActorDAO {

    private static final Logger logger = LoggerFactory.getLogger(ActorDAO.class);
    private static ActorDAO instance;
    private static EntityManagerFactory emf;

    public static ActorDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
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
            logger.error("Fejl under oprettelse af en skuespiller", e);
            throw new JpaException("Der opstod en fejl under oprettelse af en skuespiller", e);
        }
    }

    public Actor findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Actor.class, id);
        } catch (Exception e) {
            logger.error("Fejl under hentning af skuespiller med ID: " + id, e);
            throw new JpaException("Der opstod en fejl under hentning af en skuespiller", e);
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
            logger.error("Fejl under opdatering af skuespiller: " + dto.getName(), e);
            throw new JpaException("Der opstod en fejl under opdatering af en skuespiller", e);
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
            logger.error("Fejl under sletning af skuespiller med ID: " + id, e);
            throw new JpaException("Der opstod en fejl under sletning af en skuespiller", e);
        }
    }

    public List<ActorDTO> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Actor> actors = em.createQuery("SELECT a FROM Actor a", Actor.class).getResultList();
            return actors.stream()
                    .map(a -> ActorDTO.builder()
                            .id(a.getId())
                            .name(a.getName())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Fejl ved hentning af alle skuespillere", e);
            throw new JpaException("Fejl ved hentning af alle skuespillere", e);
        }
    }

    public Actor findByName(String name) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Actor> query = em.createQuery("SELECT a FROM Actor a WHERE a.name = :name", Actor.class);
            query.setParameter("name", name);
            return query.getSingleResult();
        } catch (Exception e) {
            logger.error("Fejl under hentning af skuespiller med navn: " + name, e);
            throw new JpaException("Der opstod en fejl under hentning af skuespiller", e);
        }
    }
}
