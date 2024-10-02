package app.persistence.daos;

import app.persistence.config.HibernateConfig;
import app.persistence.dtos.ActorDTO;
import app.persistence.dtos.DirectorDTO;
import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import org.junit.jupiter.api.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieDAOTest {

    private MovieDAO movieDAO;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    void setUp() {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        movieDAO = new MovieDAO(emf);
        em = emf.createEntityManager();
    }

    @AfterAll
    void tearDown() {

        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void cleanUpDatabase() {
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        em.createQuery("DELETE FROM Movie").executeUpdate();
        em.createQuery("DELETE FROM Actor").executeUpdate();
        em.createQuery("DELETE FROM Director").executeUpdate();
        em.createQuery("DELETE FROM Genre").executeUpdate();
        transaction.commit();
    }

    @Test
    void testCreateMovie() {
        Director director = Director.builder().name("Steven Spielberg").build();
        Set<Genre> genres = new HashSet<>();
        genres.add(Genre.builder().genreId(1).name("Action").build());
        genres.add(Genre.builder().genreId(2).name("Adventure").build());

        Set<Actor> actors = new HashSet<>();
        actors.add(Actor.builder().name("Harrison Ford").build());

        Movie movie = Movie.builder()
                .imdbId(1234567L)
                .title("Indiana Jones")
                .overview("An action-adventure movie")
                .releaseDate("1981-06-12")
                .voteAverage(8.5)
                .voteCount(500000)
                .popularity(100.0)
                .originalLanguage("en")
                .originalTitle("Raiders of the Lost Ark")
                .backdropPath("/some/path.jpg")
                .posterPath("/poster.jpg")
                .adult(false)
                .director(director)
                .genres(genres)
                .actors(actors)
                .build();

        movieDAO.create(movie);
        Movie foundMovie = movieDAO.findByTitle("Indiana Jones");
        assertNotNull(foundMovie);
        assertEquals("Indiana Jones", foundMovie.getTitle());
        assertEquals("Steven Spielberg", foundMovie.getDirector().getName());
        assertEquals(2, foundMovie.getGenres().size());
        assertEquals(1, foundMovie.getActors().size());
    }

    @Test
    void testFindById() {
        Movie movie = Movie.builder()
                .imdbId(123456789L)
                .title("Test Movie")
                .overview("A test movie overview")
                .releaseDate("2024-01-01")
                .adult(false)
                .backdropPath("/path/to/backdrop.jpg")
                .posterPath("/path/to/poster.jpg")
                .popularity(7.5)
                .originalLanguage("en")
                .originalTitle("Test Movie Original Title")
                .voteAverage(8.0)
                .voteCount(100)
                .build();

        movieDAO.create(movie);
        Movie foundMovie = movieDAO.findById(movie.getId());
        assertNotNull(foundMovie, "Movie should be found");
        assertEquals(movie.getId(), foundMovie.getId(), "ID should match");
        assertEquals(movie.getTitle(), foundMovie.getTitle(), "Title should match");
        assertEquals(movie.getImdbId(), foundMovie.getImdbId(), "IMDB ID should match");
    }

    @Test
    void testUpdateMovie() {
        Movie movie = createTestMovie("E.T.");
        movie.setTitle("E.T. the Extra-Terrestrial");
        movieDAO.update(movie);
        Movie updatedMovie = movieDAO.findByTitle("E.T. the Extra-Terrestrial");
        assertNotNull(updatedMovie);
        assertEquals("E.T. the Extra-Terrestrial", updatedMovie.getTitle());
    }

    @Test
    void testDeleteMovie() {
        Movie movie = createTestMovie("Jurassic Park");
        movieDAO.delete(movie);
        Movie deletedMovie = movieDAO.findByTitle("Jurassic Park");
        assertNull(deletedMovie);
    }

    @Test
    void testCreateDirectorDTO() {
        DirectorDTO directorDTO = DirectorDTO.builder()
                .name("Steven Spielberg")
                .build();
        movieDAO.create(directorDTO);
        assertNotNull(directorDTO.getName());
        assertEquals("Steven Spielberg", directorDTO.getName());
    }

    @Test
    void testCreateActorDTO() {
        ActorDTO actorDTO = ActorDTO.builder()
                .name("Harrison Ford")
                .build();
        movieDAO.create(actorDTO);
        assertNotNull(actorDTO.getName());
        assertEquals("Harrison Ford", actorDTO.getName());
    }

    @Test
    void testCreateGenreDTO() {
        Genre genre = Genre.builder()
                .genreId(1)
                .name("Action")
                .build();
        movieDAO.create(genre);
        assertNotNull(genre.getName());
        assertEquals("Action", genre.getName());
    }

    @Test
    void testFindByTitle() {
        createTestMovie("Retfærdighedens ryttere");
        Movie foundMovie = movieDAO.findByTitle("Retfærdighedens ryttere");
        assertNotNull(foundMovie);
        assertEquals("Retfærdighedens ryttere", foundMovie.getTitle());
    }

    @Test
    void testFindByImdbId() {
        createTestMovie("Aliens", "Science Fiction", 111161L);
        Movie foundMovie = movieDAO.findByImdbId(111161L);
        assertNotNull(foundMovie);
        assertEquals("Aliens", foundMovie.getTitle());
    }

    @Test
    void testSearchMoviesByTitle() {
        createTestMovie("Retfærdighedens ryttere");
        List<Movie> movies = movieDAO.searchMoviesByTitle("Retfærdighedens ryttere");
        assertEquals(1, movies.size());
        assertEquals("Retfærdighedens ryttere", movies.get(0).getTitle());
    }

    @Test
    void testGetAllMovies() {
        Genre genreDrama = Genre.builder()
                .genreId(1)
                .name("Drama")
                .build();

        Genre genreComedy = Genre.builder()
                .genreId(2)
                .name("Komedie")
                .build();

        Movie movie1 = Movie.builder()
                .title("Jagten")
                .overview("En lærer bliver fejlagtigt anklaget for pædofili, og hans liv bliver vendt op og ned.")
                .releaseDate("2012-12-25")
                .voteAverage(8.3)
                .adult(false)
                .backdropPath("/path/to/backdrop1.jpg")
                .posterPath("/path/to/poster1.jpg")
                .originalLanguage("da")
                .originalTitle("Jagten")
                .popularity(90.0)
                .voteCount(500)
                .imdbId(1234567L)
                .genres(Set.of(genreDrama))
                .build();

        Movie movie2 = Movie.builder()
                .title("Den Store Badedag")
                .overview("En komedie om en skæbnesvanger dag på stranden.")
                .releaseDate("2024-06-01")
                .voteAverage(7.5)
                .adult(false)
                .backdropPath("/path/to/backdrop2.jpg")
                .posterPath("/path/to/poster2.jpg")
                .originalLanguage("da")
                .originalTitle("Den Store Badedag")
                .popularity(75.0)
                .voteCount(300)
                .imdbId(1234568L)
                .genres(Set.of(genreComedy))
                .build();

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            em.persist(genreDrama);
            em.persist(genreComedy);

            em.persist(movie1);
            em.persist(movie2);

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to persist movies");
        }

        // Henter alle film fra testdatabasen
        List<Movie> movies = movieDAO.getAllMovies();

        // Forventer at der er to film i listen
        assertEquals(2, movies.size());
    }

    @Test
    void testCountMovies() {

        createTestMovie("Inception");
        long count = movieDAO.countMovies();
        assertEquals(1, count);
    }

    @Test
    void testGetMoviesByGenre() {

        createTestMovie("Star Wars", "Sci-Fi", 1234568L);

        List<Movie> movies = movieDAO.getMoviesByGenre("Sci-Fi");

        assertEquals(1, movies.size());
        assertEquals("Star Wars", movies.get(0).getTitle());
    }

    @Test
    void testGetMoviesByRating() {
        Movie movie = createTestMovie("Avatar");
        movie.setVoteAverage(9.0);
        movieDAO.update(movie);

        List<Movie> movies = movieDAO.getMoviesByRating(8.0);

        assertEquals(1, movies.size());
        assertEquals("Avatar", movies.get(0).getTitle());
    }

    @Test
    void testGetMoviesByReleaseYear() {
        Movie movie = createTestMovie("The Dark Knight");
        movie.setReleaseDate("2024-08-12");
        movieDAO.update(movie);

        //List<Movie> movies = movieDAO.getMoviesByReleaseYearAndNationality(2024, "en");
        List<Movie> movies = movieDAO.getMoviesByReleaseYearAndNationality(2024, "en");


        assertEquals(1, movies.size());
        assertEquals("The Dark Knight", movies.get(0).getTitle());
    }

    // Hjælpefunktion til at oprette testfilm
    private Movie createTestMovie(String title) {
        return createTestMovie(title, "Action", 1234567L);
    }

    private Movie createTestMovie(String title, String genreName, Long imdb) {
        Director director = Director.builder().name("Some Director").build();
        Set<Genre> genres = new HashSet<>();
        genres.add(Genre.builder().genreId(1).name(genreName).build());

        Set<Actor> actors = new HashSet<>();
        actors.add(Actor.builder().name("Some Actor").build());

        Movie movie = Movie.builder()
                .imdbId(imdb)
                .title(title)
                .overview("An interesting movie")
                .releaseDate("2024-09-18")
                .voteAverage(8.0)
                .voteCount(100000)
                .popularity(80.0)
                .originalLanguage("en")
                .originalTitle(title)
                .backdropPath("/backdrop.jpg")
                .posterPath("/poster.jpg")
                .adult(false)
                .director(director)
                .genres(genres)
                .actors(actors)
                .build();

        movieDAO.create(movie);
        return movie;
    }
}
