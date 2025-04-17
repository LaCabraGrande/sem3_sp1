package app.daos;

import app.dtos.ActorDTO;
import app.dtos.DirectorDTO;
import app.dtos.GenreDTO;
import app.dtos.MovieDTO;
import app.entities.Actor;
import app.entities.Director;
import app.entities.Genre;
import app.entities.Movie;
import app.exceptions.JpaException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MovieDAO {

    private static EntityManagerFactory emf;
    private static MovieDAO instance;

    private MovieDAO(EntityManagerFactory emf){
        this.emf = emf;
    }

    public static MovieDAO getInstance(EntityManagerFactory emf){
        if (instance == null){
            instance = new MovieDAO(emf);
        }
        return instance;
    }


    public void create(List<MovieDTO> movieDTOList) {
        EntityManager em = emf.createEntityManager();

        Map<Long, Actor> actorCache = new HashMap<>();
        Map<Long, Director> directorCache = new HashMap<>();
        Set<Long> seenImdbIds = new HashSet<>(); // Bruges til at holde styr på dubletter i batchen

        int saved = 0;
        int total = movieDTOList.size();

        try {
            em.getTransaction().begin();

            // Indlæs alle genrer én gang
            Map<Integer, Genre> genreMap = em.createQuery("SELECT g FROM Genre g", Genre.class)
                    .getResultList()
                    .stream()
                    .collect(Collectors.toMap(Genre::getGenreId, g -> g));

            for (MovieDTO dto : movieDTOList) {
                if (seenImdbIds.contains(dto.getImdbId())) {
                    continue; // Spring over hvis filmen allerede er behandlet i denne batch
                }

                seenImdbIds.add(dto.getImdbId());

                Movie movie = new Movie();
                movie.setImdbId(dto.getImdbId());
                movie.setTitle(dto.getTitle());
                movie.setDuration(dto.getDuration());
                movie.setOverview(dto.getOverview());
                movie.setReleaseDate(dto.getReleaseDate());
                movie.setAdult(dto.getIsAdult());
                movie.setBackdropPath(dto.getBackdropPath());
                movie.setPosterPath(dto.getPosterPath());
                movie.setPopularity(dto.getPopularity());
                movie.setOriginalLanguage(dto.getOriginalLanguage());
                movie.setOriginalTitle(dto.getOriginalTitle());
                movie.setVoteAverage(dto.getVoteAverage());
                movie.setVoteCount(dto.getVoteCount());

                // Director
                DirectorDTO dirDTO = dto.getDirector();
                if (dirDTO != null) {
                    Director director = directorCache.computeIfAbsent(
                            dirDTO.getId(),
                            id -> em.merge(new Director(dirDTO))
                    );
                    movie.setDirector(director);
                }

                // Genres
                Set<Genre> genres = dto.getGenreIds().stream()
                        .map(genreMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                movie.setGenres(genres);

                // Actors
                Set<Actor> actors = dto.getActors().stream()
                        .map(actorDTO -> actorCache.computeIfAbsent(
                                actorDTO.getId(),
                                id -> em.merge(new Actor(actorDTO))
                        ))
                        .collect(Collectors.toSet());
                movie.setActors(actors);

                em.persist(movie);
                saved++;

                if (saved % 1000 == 0) {
                    em.getTransaction().commit();
                    em.clear(); // Ryd cache
                    System.out.println("💾 [" + java.time.LocalTime.now().withNano(0) + "] Gemt film " + saved + " / " + total);
                    em.getTransaction().begin();
                }
            }

            em.getTransaction().commit(); // Sidste batch
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
            throw new JpaException("Fejl ved oprettelse af film.", e);
        } finally {
            em.close();
        }
    }


    public boolean hasMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            long count = em.createQuery("SELECT COUNT(m) FROM Movie m", Long.class).getSingleResult();
            System.out.println("Antal film i databasen (via hasMovies): " + count); // Til debug
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Fejl ved check af film i databasen", e);
        }
    }




    // Metode til at finde en film baseret på id
    public MovieDTO findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Movie movie = em.find(Movie.class, id);
            return new MovieDTO(movie);
        }
    }

    public List<Movie> getAllMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m ORDER BY m.releaseDate ASC", Movie.class);
            return query.getResultList();
        }
    }

    public List<Movie> getAllMoviesWithRelations() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT DISTINCT m FROM Movie m " +
                            "LEFT JOIN FETCH m.genres " +
                            "LEFT JOIN FETCH m.actors " +
                            "LEFT JOIN FETCH m.director " +
                            "ORDER BY m.releaseDate ASC", Movie.class);
            return query.getResultList();
        }
    }


    public boolean validatePrimaryKey(Integer integer) {
        try (EntityManager em = emf.createEntityManager()) {
            Movie movie = em.find(Movie.class, integer);
            return movie != null;
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
            throw new JpaException("Der er opstået en fejl ved optælling af filmene i databasen.", e);
        }
    }

    // Metode til at opdatere titlen på en film med angivelse af den gamle titel og den nye titel
    public void updateMovieTitle(String title, String newTitle) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Movie movie = em.createQuery("SELECT m FROM Movie m WHERE m.originalTitle = :title", Movie.class)
                        .setParameter("title", title)
                        .getSingleResult();

                if (movie != null) {
                    movie.setOriginalTitle(newTitle);
                    em.merge(movie);
                    transaction.commit();
                    System.out.println("Filmen " + title + " er opdateret med en ny titel: " + newTitle);
                }
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new JpaException("Fejl ved opdatering af titlen for filmen "+title, e);
            }
        }
    }

    // Metode til at slette en film baseret på titlen
    public void deleteByTitle(String title) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
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
                if (transaction.isActive()) {
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
    
    // En metode til at slette film med en rating over et givet tal. Bruger den til at rense ud i Movies tabellen
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

    public List<MovieDTO> getMoviesByTitle(String searchString) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT new app.dtos.MovieDTO(m) FROM Movie m WHERE LOWER(m.originalTitle) LIKE :searchString ORDER BY m.releaseDate ASC";
            String formattedSearchString = "%" + searchString.toLowerCase() + "%";
            TypedQuery<MovieDTO> query = em.createQuery(jpql, MovieDTO.class);
            query.setParameter("searchString", formattedSearchString);
            List<MovieDTO> movieDTOS = query.getResultList();
            return movieDTOS;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Database query failed", e); // Returnér mere specifik fejl
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

            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.originalTitle = :title", Movie.class);
            query.setParameter("title", title);
            Movie movie = query.getResultStream().findFirst().orElse(null);

            if (movie != null) {
                movie.setReleaseDate(newReleaseDate);
                em.merge(movie);
                em.getTransaction().commit();

                System.out.println("\nFilmen " + movie.getTitle() + " er opdateret med ny release-dato: " + newReleaseDate);
            } else {
                System.out.println("Ingen film fundet med titlen: " + title);
                em.getTransaction().rollback();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved opdatering af filmens release-dato: " + title, e);
        }
    }

//    public void createNewMovie(MovieDTO movieDTO) {
//        if (movieDTO == null) {
//            throw new IllegalArgumentException("MovieDTO cannot be null");
//        }
//
//        EntityManager em = emf.createEntityManager();
//        try (em) {
//            em.getTransaction().begin();
//
//            // Konverter MovieDTO til en Movie-entitet
//            Movie movie = new Movie(movieDTO);
//
//            // Håndter director
//            Director director = movie.getDirector();
//            if (director != null) {
//                TypedQuery<Director> query = em.createQuery(
//                        "SELECT d FROM Director d WHERE d.name = :name", Director.class);
//                query.setParameter("name", director.getName());
//
//                try {
//                    Director existingDirector = query.getSingleResult();
//                    movie.setDirector(existingDirector);
//                } catch (NoResultException e) {
//                    director = em.merge(director);
//                    movie.setDirector(director);
//                }
//            }
//
//            // Håndter genres
//            if (movie.getGenres() != null) {
//                Set<Genre> mergedGenres = new HashSet<>();
//                for (Genre genre : movie.getGenres()) {
//                    TypedQuery<Genre> query = em.createQuery(
//                            "SELECT g FROM Genre g WHERE g.genreId = :genreId", Genre.class);
//                    query.setParameter("genreId", genre.getId());
//
//                    try {
//                        Genre existingGenre = query.getSingleResult();
//                        mergedGenres.add(existingGenre);
//                    } catch (NoResultException e) {
//                        Genre mergedGenre = em.merge(genre);
//                        mergedGenres.add(mergedGenre);
//                    }
//                }
//                movie.setGenres(mergedGenres);
//            }
//
//            // Håndter actors
//            if (movie.getActors() != null) {
//                Set<Actor> mergedActors = new HashSet<>();
//                for (Actor actor : movie.getActors()) {
//                    Actor mergedActor = em.merge(actor);
//                    mergedActors.add(mergedActor);
//                }
//                movie.setActors(mergedActors);
//            }
//
//            // Persist movie entity
//            em.persist(movie);
//            em.getTransaction().commit();
//        } catch (Exception e) {
//            if (em.getTransaction().isActive()) {
//                em.getTransaction().rollback();
//            }
//            e.printStackTrace();
//            throw new JpaException("Der opstod en fejl ved oprettelsen af filmen: " + movieDTO.getTitle(), e);
//        }
//    }




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
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT m FROM Movie m JOIN m.genres g WHERE g.name = :genreName ORDER BY m.releaseDate ASC";
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
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT m FROM Movie m JOIN m.genres g WHERE g.name = :genreName";
            TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
            query.setParameter("genreName", genreName);
            // Her sætter jeg firstresult for at angive hvilken side jeg vil have og maxresults for at angive hvor mange jeg vil have
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for genre: " + genreName, e);
        }
    }

    public List<Movie> getMoviesByRating(double rating) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT m FROM Movie m WHERE m.voteAverage >= :rating ORDER BY m.releaseDate ASC";
            TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
            query.setParameter("rating", rating);
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for rating: " + rating, e);
        }
    }

    public List<Movie> getMoviesByRatingForAPIServer(double rating, int page, int size) {
        try (EntityManager em = emf.createEntityManager()) {
            if (page < 0 || size <= 0) {
                throw new IllegalArgumentException("Page must be >= 0 and size must be > 0");
            }
            String jpql = "SELECT m FROM Movie m WHERE m.voteAverage >= :rating";
            TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
            query.setParameter("rating", rating);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for rating: " + rating, e);
        }
    }

    public List<Movie> getMoviesByReleaseYearAndNationality(int year, String language) {
        try (EntityManager em = emf.createEntityManager()) {
            String startOfYear = year + "-01-01";
            String endOfYear = year + "-12-31";
            String languageCode = language;

            // Justerer her endOfYear til midnat den 31. december
            endOfYear = endOfYear + " 23:59:59";

            //String jpql = "SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startOfYear AND :endOfYear";
            String jpql = "SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startOfYear AND :endOfYear AND m.originalLanguage = :languageCode ORDER BY m.releaseDate ASC";
            TypedQuery<Movie> query = em.createQuery(jpql, Movie.class);
            query.setParameter("startOfYear", startOfYear);
            query.setParameter("endOfYear", endOfYear);
            query.setParameter("languageCode", languageCode);
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for år: " + year + " og sprog: " + language, e);
        }
    }

    public List<Movie> getMoviesByReleaseYearForAPIServer(int year, int page, int size) {
        try (EntityManager em = emf.createEntityManager()) {
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
        } catch (Exception e) {
            throw new JpaException("Der er opstået en fejl ved hentning af film for år: " + year, e);
        }
    }

    public List<Movie> getMoviesByDirector(String directorName) {
        EntityManager em = emf.createEntityManager();
        List<Movie> movies;
        try {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.director.name = :directorName ORDER BY m.releaseDate ASC", Movie.class);
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

    public MovieDTO findByImdbId(Long imdbId) {
        MovieDTO movieDTO = null;
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<MovieDTO> query = em.createQuery("SELECT new app.dtos.MovieDTO(m) FROM Movie m WHERE m.imdbId = :imdbId", MovieDTO.class);
            query.setParameter("imdbId", imdbId);
            movieDTO = query.getSingleResult();
        } catch (NoResultException e) {
            e.printStackTrace();
            throw new JpaException("Der er opstået en fejl ved hentning af film med imdbId: " + imdbId, e);
        }
        return movieDTO;
    }


    public List<Movie> getMoviesByActor(String actorName) {
        List<Movie> movies;
        try(EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT m FROM Movie m JOIN m.actors a WHERE a.name = :actorName ORDER BY m.releaseDate ASC", Movie.class);
            query.setParameter("actorName", actorName);
            movies = query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new JpaException("Fejl ved hentning af film for skuespiller: " + actorName, e);
        }
        return movies;
    }


}
