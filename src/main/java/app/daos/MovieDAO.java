package app.daos;

import app.dtos.*;
import app.entities.*;
import app.exceptions.JpaException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MovieDAO {

    private static final Logger logger = LoggerFactory.getLogger(MovieDAO.class);

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
            logger.info("Hentede {} film fra databasen.", movies.size());
            return movies.stream().map(MovieDTO::new).toList();
        } catch (Exception e) {
            throw new JpaException("Fejl ved hentning af alle film", e);
        }
    }

    public List<MovieDTO> getFilteredMovies(Map<String, List<String>> filters, int page, int pageSize) {
        try (EntityManager em = emf.createEntityManager()) {
            StringBuilder jpql = new StringBuilder("SELECT DISTINCT m FROM Movie m LEFT JOIN m.genres g LEFT JOIN m.actors a LEFT JOIN m.directors d WHERE 1=1");

            if (filters.containsKey("genre")) jpql.append(" AND g.name IN :genres");
            if (filters.containsKey("year")) jpql.append(" AND FUNCTION('YEAR', m.releaseDate) IN :years");
            if (filters.containsKey("language")) jpql.append(" AND m.originalLanguage IN :languages");
            if (filters.containsKey("rating")) jpql.append(" AND m.voteAverage >= :minRating");
            if (filters.containsKey("duration")) jpql.append(" AND m.duration <= :maxDuration");
            if (filters.containsKey("director")) jpql.append(" AND d.name IN :directors");
            if (filters.containsKey("title")) jpql.append(" AND LOWER(m.title) LIKE :title");
            if (filters.containsKey("actor")) jpql.append(" AND a.name IN :actors");

            TypedQuery<Movie> query = em.createQuery(jpql.toString(), Movie.class);

            if (filters.containsKey("genre")) query.setParameter("genres", filters.get("genre"));
            if (filters.containsKey("year")) query.setParameter("years", filters.get("year").stream().map(Integer::parseInt).toList());
            if (filters.containsKey("language")) query.setParameter("languages", filters.get("language"));
            if (filters.containsKey("rating")) query.setParameter("minRating", Double.parseDouble(filters.get("rating").get(0)));
            if (filters.containsKey("duration")) query.setParameter("maxDuration", Integer.parseInt(filters.get("duration").get(0)));
            if (filters.containsKey("director")) query.setParameter("directors", filters.get("director"));
            if (filters.containsKey("title")) query.setParameter("title", "%" + filters.get("title").get(0).toLowerCase() + "%");
            if (filters.containsKey("actor")) query.setParameter("actors", filters.get("actor"));

            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);

            List<Movie> result = query.getResultList();
            logger.info("Filtreret filmhentning returnerede {} resultater.", result.size());
            return result.stream().map(MovieDTO::new).toList();
        } catch (Exception e) {
            throw new JpaException("Fejl ved hentning af filtrerede film", e);
        }
    }

    public FilterCountDTO getFilteredCounts(Map<String, List<String>> filters) {
        try {
            List<MovieDTO> allMovies = getAllMovies();
            List<MovieDTO> filtered = allMovies.stream()
                    .filter(movie -> filters.entrySet().stream()
                            .allMatch(entry -> matchesFilter(entry.getKey(), entry.getValue(), movie)))
                    .toList();
            return calculateFilterCounts(filtered, filters);
        } catch (Exception e) {
            throw new JpaException("Fejl ved beregning af filtertællinger", e);
        }
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
        return new FilterCountDTO(genreCount, yearCount, languageCount, ratingCount, directorCount, actorCount, titleCount);
    }

    private Map<String, Integer> countBy(List<MovieDTO> filtered, List<MovieDTO> all, Map<String, List<String>> filters, String category, Function<MovieDTO, List<String>> extractor) {
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
                    .getResultList().stream()
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
                    persistException.printStackTrace();
                    logger.warn("Kunne ikke persiste film: {}", dto.getTitle(), persistException);
                }
            }

            em.getTransaction().commit();
            logger.info("✅ Gemte i alt {} film.", saved);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new JpaException("Fejl ved oprettelse af film.", e);
        } finally {
            em.close();
        }
    }

    public boolean hasMovies() {
        try (EntityManager em = emf.createEntityManager()) {
            long count = em.createQuery("SELECT COUNT(m) FROM Movie m", Long.class).getSingleResult();
            logger.info("🎞️ Der er {} film i databasen.", count);
            return count > 0;
        } catch (Exception e) {
            throw new JpaException("Fejl ved check af film i databasen", e);
        }
    }

    public MovieDTO findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Movie movie = em.find(Movie.class, id);
            return new MovieDTO(movie);
        } catch (Exception e) {
            throw new JpaException("Fejl ved hentning af film baseret på ID", e);
        }
    }

    public Movie findByTitle(String title) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m WHERE m.title = :title", Movie.class);
            query.setParameter("title", title);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new JpaException("Fejl ved hentning af film baseret på titel", e);
        }
    }

    public List<MovieDTO> getMoviesByTitle(String searchString) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT new app.dtos.MovieDTO(m) FROM Movie m WHERE LOWER(m.originalTitle) LIKE :searchString ORDER BY m.releaseDate ASC";
            TypedQuery<MovieDTO> query = em.createQuery(jpql, MovieDTO.class);
            query.setParameter("searchString", "%" + searchString.toLowerCase() + "%");
            return query.getResultList();
        } catch (Exception e) {
            throw new JpaException("Fejl ved søgning efter film baseret på titel", e);
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
            throw new JpaException("Fejl under hentning af film med pagination", e);
        }
    }

    public MovieDTO findByImdbId(Long imdbId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<MovieDTO> query = em.createQuery("SELECT new app.dtos.MovieDTO(m) FROM Movie m WHERE m.imdbId = :imdbId", MovieDTO.class);
            query.setParameter("imdbId", imdbId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new JpaException("Ingen film fundet med imdbId: " + imdbId, e);
        } catch (Exception e) {
            throw new JpaException("Fejl ved hentning af film med imdbId: " + imdbId, e);
        }
    }
}
