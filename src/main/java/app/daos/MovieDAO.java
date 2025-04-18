package app.daos;

import app.dtos.*;
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

import java.util.function.Function;


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

    public List<MovieDTO> getAllMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery(
                    "SELECT DISTINCT m FROM Movie m " +
                            "LEFT JOIN FETCH m.genres " +
                            "LEFT JOIN FETCH m.actors " +
                            "LEFT JOIN FETCH m.director " +
                            "ORDER BY m.releaseDate ASC", Movie.class);
            List<Movie> movies = query.getResultList();
            return movies.stream().map(MovieDTO::new).collect(Collectors.toList());
        }
    }

    /**
     * Hent film baseret på flere filtre
     */
    public List<MovieDTO> getFilteredMovies(Map<String, List<String>> filters, int page, int pageSize) {
        try (EntityManager em = emf.createEntityManager()) {
            StringBuilder jpql = new StringBuilder("SELECT DISTINCT m FROM Movie m LEFT JOIN m.genres g LEFT JOIN m.actors a LEFT JOIN m.directors d WHERE 1=1");

            if (filters.containsKey("genre")) {
                jpql.append(" AND g.name IN :genres");
            }
            if (filters.containsKey("year")) {
                jpql.append(" AND FUNCTION('YEAR', m.releaseDate) IN :years");
            }
            if (filters.containsKey("language")) {
                jpql.append(" AND m.originalLanguage IN :languages");
            }
            if (filters.containsKey("rating")) {
                jpql.append(" AND m.voteAverage >= :minRating");
            }
            if (filters.containsKey("duration")) {
                jpql.append(" AND m.duration <= :maxDuration");
            }
            if (filters.containsKey("director")) {
                jpql.append(" AND d.name IN :directors");
            }
            if (filters.containsKey("title")) {
                jpql.append(" AND LOWER(m.title) LIKE :title");
            }
            if (filters.containsKey("actor")) {
                jpql.append(" AND a.name IN :actors");
            }

            TypedQuery<Movie> query = em.createQuery(jpql.toString(), Movie.class);

            if (filters.containsKey("genre")) {
                query.setParameter("genres", filters.get("genre"));
            }
            if (filters.containsKey("year")) {
                List<Integer> years = filters.get("year").stream().map(Integer::parseInt).toList();
                query.setParameter("years", years);
            }
            if (filters.containsKey("language")) {
                query.setParameter("languages", filters.get("language"));
            }
            if (filters.containsKey("rating")) {
                query.setParameter("minRating", Double.parseDouble(filters.get("rating").get(0)));
            }
            if (filters.containsKey("duration")) {
                query.setParameter("maxDuration", Integer.parseInt(filters.get("duration").get(0)));
            }
            if (filters.containsKey("director")) {
                query.setParameter("directors", filters.get("director"));
            }
            if (filters.containsKey("title")) {
                String titleSearch = "%" + filters.get("title").get(0).toLowerCase() + "%";
                query.setParameter("title", titleSearch);
            }
            if (filters.containsKey("actor")) {
                query.setParameter("actors", filters.get("actor"));
            }

            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);

            List<Movie> result = query.getResultList();
            return result.stream().map(MovieDTO::new).toList();
        }
    }

    public FilterCountDTO getFilteredCounts(Map<String, List<String>> filters) {
        List<MovieDTO> allMovies = getAllMovies();

        List<MovieDTO> filtered = allMovies.stream()
                .filter(movie -> filters.entrySet().stream()
                        .allMatch(entry -> matchesFilter(entry.getKey(), entry.getValue(), movie)))
                .collect(Collectors.toList());

        return calculateFilterCounts(filtered, filters);
    }

    private boolean matchesFilter(String key, List<String> values, MovieDTO movie) {
        if (values == null || values.isEmpty()) return true;
        return switch (key) {
            case "genre" -> values.stream().anyMatch(v -> movie.getGenreNames().contains(v));
            case "year" -> movie.getReleaseDate() != null && values.contains(movie.getReleaseDate().substring(0, 4));
            case "language" -> values.contains(movie.getOriginalLanguage());
            case "rating" -> values.stream().map(Double::parseDouble).anyMatch(v -> Math.floor(movie.getVoteAverage()) == v);
            case "director" -> movie.getDirector() != null && values.contains(movie.getDirector().getName());
            case "actor" -> movie.getActors() != null && movie.getActors().stream().map(ActorDTO::getName).anyMatch(values::contains);
            case "title" -> values.contains(movie.getTitle());
            default -> false;
        };
    }

    private FilterCountDTO calculateFilterCounts(List<MovieDTO> filtered, Map<String, List<String>> filters) {
        List<MovieDTO> all = getAllMovies();

        Map<String, Integer> genreCount = countBy(filtered, all, filters, "genre", MovieDTO::getGenreNames);
        Map<String, Integer> yearCount = countBy(filtered, all, filters, "year", m -> List.of(String.valueOf(m.getReleaseYear())));
        Map<String, Integer> languageCount = countBy(filtered, all, filters, "language", m -> List.of(m.getOriginalLanguage()));
        Map<String, Integer> ratingCount = countBy(filtered, all, filters, "rating", m -> List.of(String.valueOf((int) Math.floor(m.getRating()))));
        Map<String, Integer> directorCount = countBy(filtered, all, filters, "director", m -> List.of(m.getDirectorName()));
        Map<String, Integer> actorCount = countBy(filtered, all, filters, "actor", MovieDTO::getActorNames);
        Map<String, Integer> titleCount = countBy(filtered, all, filters, "title", m -> List.of(m.getTitle()));

        return new FilterCountDTO(
                genreCount,
                yearCount,
                languageCount,
                ratingCount,
                directorCount,
                actorCount,
                titleCount
        );
    }

    private Map<String, Integer> countBy(List<MovieDTO> filtered, List<MovieDTO> all, Map<String, List<String>> filters,
                                         String category, Function<MovieDTO, List<String>> extractor) {
        List<MovieDTO> base = filters.keySet().size() == 1 && filters.containsKey(category) ? all : filtered;

        Map<String, Integer> countMap = new TreeMap<>();
        base.forEach(movie -> extractor.apply(movie).forEach(val -> countMap.put(val, countMap.getOrDefault(val, 0) + 1)));

        return countMap;
    }

    public void create(List<MovieDTO> movieDTOList) {
        EntityManager em = emf.createEntityManager();

        Map<Long, Actor> actorCache = new HashMap<>();
        Map<Long, Director> directorCache = new HashMap<>();
        Set<Long> seenImdbIds = new HashSet<>();

        int saved = 0;
        int total = movieDTOList.size();

        try {
            em.getTransaction().begin();

            Map<Integer, Genre> genreMap = em.createQuery("SELECT g FROM Genre g", Genre.class)
                    .getResultList()
                    .stream()
                    .collect(Collectors.toMap(Genre::getGenreId, g -> g));

            for (MovieDTO dto : movieDTOList) {
                if (seenImdbIds.contains(dto.getImdbId())) continue;
                seenImdbIds.add(dto.getImdbId());

                try {
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

                    DirectorDTO dirDTO = dto.getDirector();
                    if (dirDTO != null) {
                        Director director = directorCache.computeIfAbsent(
                                dirDTO.getId(),
                                id -> em.merge(new Director(dirDTO))
                        );
                        movie.setDirector(director);
                    } else {
                        System.out.println("⚠️ Film uden instruktør: " + dto.getTitle());
                    }

                    Set<Genre> genres = dto.getGenreIds().stream()
                            .map(genreMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    movie.setGenres(genres);

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
                        em.clear();
                        System.out.println("💾 [" + java.time.LocalTime.now().withNano(0) + "] Gemt film " + saved + " / " + total);
                        em.getTransaction().begin();
                    }

                } catch (Exception persistException) {
                    System.out.println("❌ Fejl ved persist af film: " + dto.getTitle() + " | IMDB: " + dto.getImdbId());
                    persistException.printStackTrace();
                }
            }

            em.getTransaction().commit();
            System.out.println("✅ Sidste transaction committed med i alt: " + saved + " film.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("❌ Transaction rullet tilbage: " + e.getMessage());
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

//    public List<Movie> getAllMovies() {
//        try (EntityManager em = emf.createEntityManager()) {
//            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m ORDER BY m.releaseDate ASC", Movie.class);
//            return query.getResultList();
//        }
//    }

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
