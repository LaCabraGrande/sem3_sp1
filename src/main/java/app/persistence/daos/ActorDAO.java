package app.persistence.daos;

import app.persistence.dtos.ActorDTO;
import app.persistence.entities.Actor;
import app.persistence.exceptions.JpaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.stream.Collectors;

public class ActorDAO {

    private static ActorDAO instance;
    private final EntityManager em;
    private final EntityManagerFactory emf;

    public  ActorDAO(EntityManagerFactory emf) {
        this.emf = emf;
        em = emf.createEntityManager();
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
            throw new JpaException("Der opstod en fejl under oprettelse af en skuespiller", e);
        }
    }

    public Actor findById(Long id) {

        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Actor.class, id);
        } catch (Exception e) {
            throw new JpaException("Der opstod en fejl under oprettelse af en skuespiller", e);
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
            throw new JpaException("Der opstod en fejl under opdatering af en skuespiller", e);
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
            throw new JpaException("Der opstod en fejl under sletning af en skuespiller", e);
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
                    .collect(Collectors.toList());
        } finally {
            em.close();
        }
    }
    public Actor findByName(String name) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT a FROM Actor a WHERE a.name = :name", Actor.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (Exception e) {
            throw new JpaException("Der opstod en fejl under hentning af skuespiller", e);
        }
    }
}
