package app.persistence.daos;

import app.persistence.dtos.ActorDTO;
import app.persistence.dtos.DirectorDTO;
import app.persistence.dtos.GenreDTO;
import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.exceptions.JpaException;
import jakarta.persistence.*;
import app.persistence.config.HibernateConfig;
import app.persistence.enums.HibernateConfigState;
import java.util.*;

public class MovieDAO {

    private static MovieDAO instance;
    private static EntityManagerFactory emf;
    private final EntityManager em;

    private MovieDAO() {
        em = emf.createEntityManager();
    }

    // Singleton-metode til at hente en instans af MovieDAO
    public static MovieDAO getInstance(HibernateConfigState state) {
        if (instance == null) {
            emf = HibernateConfig.getEntityManagerFactoryConfig(state, "movie");
            instance = new MovieDAO();
        }
        return instance;
    }

    // Her lukker jeg EntityManagerFactory
    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }


    public void dropAllTables() {
       try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            // Her sletter jeg alle mine tabeller i databasen
            em.createNativeQuery("DROP TABLE IF EXISTS Movie CASCADE").executeUpdate();
            em.createNativeQuery("DROP TABLE IF EXISTS Genre CASCADE").executeUpdate();
            em.createNativeQuery("DROP TABLE IF EXISTS Actor CASCADE").executeUpdate();
            em.createNativeQuery("DROP TABLE IF EXISTS Director CASCADE").executeUpdate();
            em.createNativeQuery("DROP TABLE IF EXISTS movie_actor CASCADE").executeUpdate();
            em.createNativeQuery("DROP TABLE IF EXISTS movie_genre CASCADE").executeUpdate();
            em.getTransaction().commit();
            System.out.println("Alle tabeller er blevet slettet i Databasen!");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        }
    }

    // Metode til at finde en film baseret på id
    public Movie findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Movie.class, id);
        }
    }

    // Metode til at finde en film baseret på titlen
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

    // Metode til at tælle alle film i databasen
    public long countMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(m) FROM Movie m", Long.class);
            long count = query.getSingleResult();
            em.getTransaction().commit();
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved optælling af filmene i databasen.", e);
        }
    }

    // Metode til at opdatere titlen på en film med angivelse af den gamle titel og den nye titel
    public void updateMovieTitle(String title, String newTitle) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Movie movie = em.createQuery("SELECT m FROM Movie m WHERE m.title = :title", Movie.class)
                        .setParameter("title", title)
                        .getSingleResult();

                if (movie != null) {
                    movie.setTitle(newTitle);
                    em.merge(movie);
                    transaction.commit();
                    System.out.println("Filmen " + title + " er opdateret med en ny titel: " + newTitle);
                }
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Fejl ved opdatering af titel for filmen "+title, e);
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

    // Metode til at slette film der ikke har nogen release-dato tilknyttet
    public int deleteMoviesWithoutReleaseDate() {
        int deleted = 0;
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                em.getTransaction().begin();
                List<Movie> movies = em.createQuery(
                                "SELECT m FROM Movie m WHERE m.releaseDate IS NULL OR LENGTH(m.releaseDate) < 6", Movie.class)
                        .getResultList();


                if (!movies.isEmpty()) {
                    for(Movie movie : movies) {
                        em.remove(movie);
                        deleted++;
                    }
                    em.getTransaction().commit();
                    System.out.println("Alle film uden release-dato er slettet fra databasen");
                } else {
                    System.out.println("Ingen film uden release-dato fundet");
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                }
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Fejl ved sletning af film uden release-dato", e);
            }
        }
        return deleted;
    }
    public int deleteMoviesWithRatingOver(double rating) {
        int deleted = 0;
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                em.getTransaction().begin();
                List<Movie> movies = em.createQuery(
                        "SELECT m FROM Movie m WHERE m.voteAverage > :rating", Movie.class)
                        .setParameter("rating", rating)
                        .getResultList();

                if (!movies.isEmpty()) {
                    for(Movie movie : movies) {
                        em.remove(movie);
                        deleted++;
                    }
                    em.getTransaction().commit();
                    System.out.println("Alle film med en rating over " + rating + " er slettet fra databasen");
                } else {
                    System.out.println("Ingen film med en rating over " + rating + " fundet");
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                }
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Fejl ved sletning af film med rating over " + rating, e);
            }
        }
        return deleted;
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
                throw new JpaException("Der er opstået en fejl ved opdatering af filmen: " + movie.getTitle(), e);
            }
        }
    }

    // Metode til at opdatere release-datoen på en film baseret på titlen
    public void updateMovieReleaseDate(String title, String newReleaseDate) {
        if (title == null || newReleaseDate == null) {
            throw new IllegalArgumentException("Titel og ny release-dato må ikke være null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.title = :title", Movie.class);
            query.setParameter("title", title);
            Movie movie = query.getResultStream().findFirst().orElse(null);

            if (movie != null) {
                movie.setReleaseDate(newReleaseDate);
                em.merge(movie);
                em.getTransaction().commit();

                System.out.println("Filmen " + movie.getTitle() + " er opdateret med ny release-dato: " + newReleaseDate);
            } else {
                System.out.println("Ingen film fundet med titlen: " + title);
                em.getTransaction().rollback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved opdatering af filmens release-dato: " + title, e);
        }
    }

    public void createNewMovie(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie cannot be null");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

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

            if (movie.getActors() != null) {
                Set<Actor> mergedActors = new HashSet<>();
                for (Actor actor : movie.getActors()) {
                    Actor mergedActor = em.merge(actor);
                    mergedActors.add(mergedActor);
                }
                movie.setActors(mergedActors);
            }
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

            // Her tjekker jeg om instruktøren allerede findes i databasen
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

            // Her merger jeg for at sikre, at eksisterende genrer opdateres eller oprettes
            if (movie.getGenres() != null) {
                Set<Genre> mergedGenres = new HashSet<>();
                for (Genre genre : movie.getGenres()) {
                    Genre mergedGenre = em.merge(genre);
                    mergedGenres.add(mergedGenre);
                }
                movie.setGenres(mergedGenres);
            }

            // Her merger jeg for at sikre, at eksisterende actors opdateres eller oprettes
            if (movie.getActors() != null) {
                Set<Actor> mergedActors = new HashSet<>();
                for (Actor actor : movie.getActors()) {
                    Actor mergedActor = em.merge(actor);
                    mergedActors.add(mergedActor);
                }
                movie.setActors(mergedActors);
            }
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

    // Metode til at oprette en ny genre men bruger den kun i test-klassen
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
                throw new JpaException("Der er opstået en fejl ved oprettelse af genren: "+dto.getName(), e);
            }
        }
    }

    // Metode til at oprette en ny genre men bruger den kun i test-klassen
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
                throw new JpaException("Der er opstået en fejl ved oprettelse af genren: "+genre.getName(), e);
            }
        }
    }

    // Metode til at oprette en ny skuespiller men bruger den kun i test-klassen
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
                throw new JpaException("Der er opstået en fejl ved oprettelse af skuespilleren: "+dto.getName(), e);
            }
        }
    }

    // Metode til at oprette en ny skuespiller men bruger den kun i test-klassen
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
                throw new JpaException("Der er opstået en fejl ved oprettelse af instruktøren: "+dto.getName(), e);
            }
        }
    }

    public List<Movie> getAllMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m", Movie.class);
            return query.getResultList();
        }
    }

    public List<Movie> getMovies(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }

        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m", Movie.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            // Log fejlen eller håndter den efter behov
            throw new RuntimeException("Fejl under hentning af film: " + e.getMessage(), e);
        }
    }

    public List<Movie> getMoviesByGenre(String genreName) {
        try {
            String jpql = "SELECT m FROM Movie m JOIN m.genres g WHERE g.name = :genreName";
            TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
            query.setParameter("genreName", genreName);
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for genre: " + genreName, e);
        }
    }

    public List<Movie> getMoviesByGenreForAPIServer(String genreName, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }
        try {
            String jpql = "SELECT m FROM Movie m JOIN m.genres g WHERE g.name = :genreName";
            TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
            query.setParameter("genreName", genreName);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for genre: " + genreName, e);
        }
    }

    public List<Movie> getMoviesByRating(double rating) {
        String jpql = "SELECT m FROM Movie m WHERE m.voteAverage >= :rating";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("rating", rating);
        return query.getResultList();
    }

    public List<Movie> getMoviesByRatingForAPIServer(double rating, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }
        String jpql = "SELECT m FROM Movie m WHERE m.voteAverage >= :rating";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("rating", rating);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<Movie> getMoviesByReleaseYearAndNationality(int year, String language) {
        String startOfYear = year + "-01-01";
        String endOfYear = year + "-12-31";
        String languageCode = language;

        // Justerer her endOfYear til midnat den 31. december
        endOfYear = endOfYear + " 23:59:59";

        //String jpql = "SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startOfYear AND :endOfYear";
        String jpql = "SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startOfYear AND :endOfYear AND m.originalLanguage = :languageCode";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("startOfYear", startOfYear);
        query.setParameter("endOfYear", endOfYear);
        query.setParameter("languageCode", languageCode);
        return query.getResultList();
    }

    public List<Movie> getMoviesByReleaseYearForAPIServer(int year, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
        }
        String startOfYear = year + "-01-01";
        String endOfYear = year + "-12-31";

        // Justerer her endOfYear til midnat den 31. december
        endOfYear = endOfYear + " 23:59:59";

        String jpql = "SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startOfYear AND :endOfYear";
        TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
        query.setParameter("startOfYear", startOfYear);
        query.setParameter("endOfYear", endOfYear);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<Movie> getMoviesByDirector(String directorName) {
        EntityManager em = emf.createEntityManager();
        List<Movie> movies;
        try {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.director.name = :directorName", Movie.class);
            query.setParameter("directorName", directorName);
            movies = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der opstod en fejl da jeg prøvede at hente film instrueret af: " + directorName, e);
        } finally {
            em.close();
        }
        return movies;
    }

    public void delete(Movie movie) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Movie managedMovie = em.find(Movie.class, movie.getId());
                em.remove(managedMovie);
                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Der er opstået en fejl ved sletning af filmen: " + movie.getTitle(), e);
            }
        }
    }

    public Movie findByImdbId(Long imdbId) {
        Movie movie = null;
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.imdbId = :imdbId", Movie.class);
            query.setParameter("imdbId", imdbId);
            movie = query.getSingleResult();
            System.out.println(movie);
        } catch (NoResultException e) {
            System.out.println("Ingen film fundet med imdbId: " + imdbId);
        }
        return movie;
    }

    public List<Movie> getMoviesByActor(String actorName) {
        List<Movie> movies;
        try(EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT m FROM Movie m JOIN m.actors a WHERE a.name = :actorName", Movie.class);
            query.setParameter("actorName", actorName);
            movies = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Fejl ved hentning af film for skuespiller: " + actorName, e);
        }
        return movies;
    }
}
