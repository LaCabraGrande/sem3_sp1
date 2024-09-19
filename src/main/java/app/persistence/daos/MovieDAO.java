package app.persistence.daos;

import app.persistence.dtos.ActorDTO;
import app.persistence.dtos.DirectorDTO;
import app.persistence.dtos.GenreDTO;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.exceptions.JpaException;
import jakarta.persistence.*;
import app.persistence.config.HibernateConfig;
import app.persistence.enums.HibernateConfigState;

import java.util.*;
import java.util.stream.Collectors;

public class MovieDAO {

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

    public Movie findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Movie.class, id);
        }
    }

    public Movie findByTitle(String title) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT m FROM Movie m WHERE m.title = :title", Movie.class);
            query.setParameter("title", title);

            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }


    private MovieDTO convertToDTO(Movie movie) {
        return MovieDTO.builder()
                .databaseId(movie.getId())
                .imdbId(movie.getImdbId())
                .title(movie.getTitle())
                .overview(movie.getOverview())
                .releaseDate(movie.getReleaseDate())
                .isAdult(movie.isAdult())
                .backdropPath(movie.getBackdropPath())
                .posterPath(movie.getPosterPath())
                .popularity(movie.getPopularity())
                .originalLanguage(movie.getOriginalLanguage())
                .originalTitle(movie.getOriginalTitle())
                .voteAverage(movie.getVoteAverage())
                .voteCount(movie.getVoteCount())
                .genreIds(getGenreIds(movie))
                .actors(convertActorsToDTO(movie.getActors()))
                .director(convertDirectorToDTO(movie.getDirector()))
                .build();
    }

    private Set<Integer> getGenreIds(Movie movie) {
        return movie.getGenres().stream()
                .map(Genre::getGenreId)
                .collect(Collectors.toSet());
    }

    private Set<ActorDTO> convertActorsToDTO(Set<Actor> actors) {
        return actors.stream()
                .map(actor -> ActorDTO.builder()
                        .id(actor.getId())
                        .name(actor.getName())
                        .movieIds(new HashSet<>())
                        .movieTitles(new HashSet<>())
                        .build())
                .collect(Collectors.toSet());
    }

    private DirectorDTO convertDirectorToDTO(Director director) {
        if (director != null) {
            return DirectorDTO.builder()
                    .id(director.getId())
                    .name(director.getName())
                    .build();
        }
        return null;
    }

    // Tilføjer en ny film til databasen
    public void createMovie(Movie movie) {
        try (EntityManager em = emf.createEntityManager()) {
            try {
                em.getTransaction().begin();
                em.persist(movie);
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
            }
        }
    }
    public long countMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            // Start transaktionen
            em.getTransaction().begin();

            // Forespørgsel til at tælle antallet af film
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(m) FROM Movie m", Long.class);
            long count = query.getSingleResult();

            // Commit transaktionen
            em.getTransaction().commit();

            return count;
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved optælling af filmene i databasen.", e);
        }
    }


    // Metode til at opdatere release-date for en film baseret på titlen
    public void updateReleaseDateByTitle(String title, String newReleaseDate) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                // Find filmen baseret på titlen
                Movie movie = em.createQuery("SELECT m FROM Movie m WHERE m.title = :title", Movie.class)
                        .setParameter("title", title)
                        .getSingleResult();

                if (movie != null) {
                    transaction.begin();
                    // Opdater filmens release-date
                    movie.setReleaseDate(newReleaseDate);
                    // Gem opdateringen i databasen
                    em.merge(movie);
                    transaction.commit();
                    System.out.println("Filmen " + movie.getTitle() + " er opdateret med ny release-date: " + newReleaseDate);
                }
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Fejl ved opdatering af release-date for filmen "+title, e);
            }
        }
    }

    // Metode til at slette en film baseret på titlen
    public void deleteByTitle(String title) {
        EntityTransaction transaction = null;
        try (EntityManager em = emf.createEntityManager()) {
            transaction = em.getTransaction();
            try {
                transaction.begin();

                // Find filmen baseret på titlen
                Movie movie = em.createQuery("SELECT m FROM Movie m WHERE m.title = :title", Movie.class)
                        .setParameter("title", title)
                        .getSingleResult();

                if (movie != null) {
                    // Slet filmen fra databasen
                    em.remove(movie);
                    transaction.commit();
                    System.out.println("Filmen " + movie.getTitle() + " er slettet fra databasen");
                } else {
                    System.out.println("Ingen film fundet med titlen: " + title);
                    if (transaction.isActive()) {
                        transaction.rollback();
                    }
                }
            } catch (NoResultException e) {
                System.out.println("Ingen film fundet med titlen: " + title);
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Fejl ved sletning af filmen " + title, e);
            }
        }
    }


    // Metode til at søge efter film baseret på en del af titlen (case-insensitive)
    public List<Movie> searchMoviesByTitle(String searchString) {
        try (EntityManager em = emf.createEntityManager()) {
            // Lav søgningen case-insensitive med LOWER() og brug LIKE til delmatch
            return em.createQuery("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(:searchString)", Movie.class)
                    .setParameter("searchString", "%" + searchString + "%")  // Brug '%' til at matche delstrenge
                    .getResultList();
        }
    }

    // Få den gennemsnitlige rating af alle film
    public double getTotalAverageRating() {
        try (EntityManager em = emf.createEntityManager()) {
            Double averageRating = em.createQuery("SELECT AVG(m.voteAverage) FROM Movie m", Double.class)
                    .getSingleResult();
            return averageRating != null ? averageRating : 0.0;  // Hvis der ikke er film, returneres 0.0
        }
    }

    // Få de top-10 laveste ratede film
    public List<Movie> getTop10LowestRatedMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT m FROM Movie m ORDER BY m.voteAverage ASC", Movie.class)
                    .setMaxResults(10)
                    .getResultList();
        }
    }

    // Få de top-10 højeste ratede film
    public List<Movie> getTop10HighestRatedMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT m FROM Movie m ORDER BY m.voteAverage DESC", Movie.class)
                    .setMaxResults(10)
                    .getResultList();
        }
    }

    // Få de top-10 mest populære film
    public List<Movie> getTop10MostPopularMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT m FROM Movie m ORDER BY m.popularity DESC", Movie.class)
                    .setMaxResults(10)
                    .getResultList();
        }
    }

    public Movie update(Movie movie) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Movie updatedMovie = em.merge(movie);
                transaction.commit();
                return updatedMovie;
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("An error occurred while updating movie", e);
            }
        }
    }

    // Metode til at opdatere release-datoen på en film baseret på titlen
    public void updateMovieReleaseDate(String title, String newReleaseDate) {
        if (title == null || newReleaseDate == null) {
            throw new IllegalArgumentException("Title and new release date cannot be null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Find filmen baseret på titlen
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.title = :title", Movie.class);
            query.setParameter("title", title);
            Movie movie = query.getResultStream().findFirst().orElse(null);

            if (movie != null) {
                // Opdater filmens release-date
                movie.setReleaseDate(newReleaseDate);

                // Gem opdateringen i databasen
                em.merge(movie);

                em.getTransaction().commit();

                System.out.println("Filmen " + movie.getTitle() + " er opdateret med ny release-date: " + newReleaseDate);
            } else {
                System.out.println("Ingen film fundet med titlen: " + title);
                em.getTransaction().rollback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved opdatering af filmens release-date: " + title, e);
        }
    }

    public void createNewMovie(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie cannot be null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Håndter director
            Director director = movie.getDirector();
            if (director != null) {
                TypedQuery<Director> query = em.createQuery(
                        "SELECT d FROM Director d WHERE d.name = :name", Director.class);
                query.setParameter("name", director.getName());

                try {
                    Director existingDirector = query.getSingleResult();
                    movie.setDirector(existingDirector);
                } catch (NoResultException e) {
                    director = em.merge(director);
                    movie.setDirector(director);
                }
            }

            // Håndter genres
            if (movie.getGenres() != null) {
                Set<Genre> mergedGenres = new HashSet<>();
                for (Genre genre : movie.getGenres()) {
                    TypedQuery<Genre> query = em.createQuery(
                            "SELECT g FROM Genre g WHERE g.genreId = :genreId", Genre.class);
                    query.setParameter("genreId", genre.getGenreId());

                    try {
                        Genre existingGenre = query.getSingleResult();
                        mergedGenres.add(existingGenre);
                    } catch (NoResultException e) {
                        Genre mergedGenre = em.merge(genre);
                        mergedGenres.add(mergedGenre);
                    }
                }
                movie.setGenres(mergedGenres);
            }

            // Håndter actors
            if (movie.getActors() != null) {
                Set<Actor> mergedActors = new HashSet<>();
                for (Actor actor : movie.getActors()) {
                    Actor mergedActor = em.merge(actor);
                    mergedActors.add(mergedActor);
                }
                movie.setActors(mergedActors);
            }

            // Gem filmen
            em.persist(movie);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved opretning af filmen: " + movie.getTitle(), e);
        }
    }



    public void create(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie cannot be null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            // Håndter director - tjek om instruktøren allerede findes i databasen
            Director director = movie.getDirector();
            if (director != null) {
                TypedQuery<Director> query = em.createQuery(
                        "SELECT d FROM Director d WHERE d.name = :name", Director.class);
                query.setParameter("name", director.getName());

                try {
                    Director existingDirector = query.getSingleResult();  // Hent eksisterende director
                    movie.setDirector(existingDirector);  // Brug eksisterende director
                } catch (NoResultException e) {
                    // Hvis ingen eksisterende director findes, opret en ny
                    director = em.merge(director);
                    movie.setDirector(director);  // Sæt den nye director på filmen
                }
            }

            // Håndter genres - merge for at sikre, at eksisterende genres opdateres eller oprettes
            if (movie.getGenres() != null) {
                Set<Genre> mergedGenres = new HashSet<>();
                for (Genre genre : movie.getGenres()) {
                    Genre mergedGenre = em.merge(genre);  // Merge genres
                    mergedGenres.add(mergedGenre);
                }
                movie.setGenres(mergedGenres); // Sæt de merge-destinationsgenrer på filmen
            }

            // Håndter actors - merge for at sikre, at eksisterende actors opdateres eller oprettes
            if (movie.getActors() != null) {
                Set<Actor> mergedActors = new HashSet<>();
                for (Actor actor : movie.getActors()) {
                    Actor mergedActor = em.merge(actor);  // Merge actors
                    mergedActors.add(mergedActor);
                }
                movie.setActors(mergedActors); // Sæt de merge-destinationsaktører på filmen
            }

            // Her gemmer vi filmen med dens opdaterede eller nye relationer
            em.persist(movie);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved opretning af filmen: "+movie.getTitle(), e);
        }
    }

    public void create(GenreDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Genre existingGenre = em.createQuery("SELECT g FROM Genre g WHERE g.genreId = :genreId", Genre.class)
                        .setParameter("genreId", dto.getGenreId())
                        .getResultStream()
                        .findFirst()
                        .orElse(null);

                Genre genre;
                if (existingGenre == null) {
                    genre = new Genre();
                    genre.setGenreId(dto.getGenreId());
                    genre.setName(dto.getName());
                    em.persist(genre);
                } else {
                    existingGenre.setName(dto.getName());
                    genre = em.merge(existingGenre);
                }

                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("An error occurred while creating or updating genre", e);
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
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("An error occurred while creating genre", e);
            }
        }
    }

    public void create(ActorDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Actor actor = new Actor();
                actor.setId(dto.getId());
                actor.setName(dto.getName());
                em.persist(actor);
                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("An error occurred while creating actor", e);
            }
        }
    }

    public void create(DirectorDTO dto) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Director director = new Director();
                director.setId(dto.getId());
                director.setName(dto.getName());
                em.persist(director);
                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("An error occurred while creating director", e);
            }
        }
    }

    public List<Movie> getAllMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m", Movie.class);
            return query.getResultList();
        }
    }

    public List<Movie> getMoviesByGenre(String genreName) {
        String jpql = "SELECT m FROM Movie m JOIN m.genres g WHERE g.name = :genreName";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("genreName", genreName);
        return query.getResultList();
    }

    public List<Movie> getMoviesByRating(double rating) {
        String jpql = "SELECT m FROM Movie m WHERE m.voteAverage >= :rating";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("rating", rating);
        return query.getResultList();
    }

    public List<Movie> getMoviesByReleaseYear(int year) {
        String startOfYear = year + "-01-01";
        String endOfYear = year + "-12-31";

        // Justerer her endOfYear til midnat den 31. december
        endOfYear = endOfYear + " 23:59:59";

        String jpql = "SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startOfYear AND :endOfYear";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("startOfYear", startOfYear);
        query.setParameter("endOfYear", endOfYear);
        return query.getResultList();
    }

        public List<Actor> getActorsByMovieTitle(String title) {
        String jpql = "SELECT a FROM Actor a JOIN a.movies m WHERE m.title = :title";
        TypedQuery<Actor> query = em.createQuery(jpql, Actor.class);
        query.setParameter("title", title);
        return query.getResultList();
    }

    public Director getDirectorByMovieTitle(String title) {
        String jpql = "SELECT m.director FROM Movie m WHERE m.title = :title";
        TypedQuery<Director> query = em.createQuery(jpql, Director.class);
        query.setParameter("title", title);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<Movie> findMoviesByActor(String actorName) {
        EntityManager em = emf.createEntityManager();
        List<Movie> movies;

        try {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT m FROM Movie m JOIN m.actors a WHERE a.name = :actorName", Movie.class);
            query.setParameter("actorName", actorName);

            movies = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("An error occurred while fetching movies for the actor: " + actorName, e);
        } finally {
            em.close();
        }
        return movies;
    }

    // Her henter jeg alle film med en bestemt instruktør
    public List<Movie> getMoviesByDirector(String directorName) {
        EntityManager em = emf.createEntityManager();
        List<Movie> movies;
        try {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.director.name = :directorName", Movie.class);
                    query.setParameter("directorName", directorName);
            movies = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("An error occurred while fetching movies for the director: " + directorName, e);
        } finally {
            em.close();
        }
        return movies;
    }





}
