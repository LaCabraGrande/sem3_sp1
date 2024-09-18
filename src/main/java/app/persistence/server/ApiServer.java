package app.persistence.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import app.persistence.apis.MovieAPI;
import app.persistence.config.HibernateConfig;
import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.enums.HibernateConfigState;
import app.persistence.services.MovieConverter;
import app.persistence.services.MovieService;
import io.javalin.Javalin;
import app.persistence.daos.MovieDAO;
import app.persistence.entities.Movie;
import io.javalin.plugin.bundled.CorsPlugin;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiServer {

    public static void main(String[] args) {
        // Initialisering af DAO
        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);

        // Opret en ObjectMapper til at konvertere til JSON
        ObjectMapper mapper = new ObjectMapper();

        // Opret en MovieService instans
        MovieService movieService = new MovieService(movieDAO);

        // Opret en Javalin instans
        Javalin app = Javalin.create().start(7070);

        // Definer en rute til roden
        app.get("/", ctx -> ctx.result("Hej verden!"));

        // Definer en rute til /hello
        app.get("/hello", ctx -> ctx.result("Hello, Javalin!"));

        // Definer en rute til at få listen af film i JSON-format baseret på instruktørens navn
        app.get("/moviesbyinstructor/{instructor}", ctx -> {
            try {
                // Hent instruktørens navn fra URL-parameteren
                String instructorName = ctx.pathParam("instructor");

                // Hent listen af film baseret på instruktørens navn
                List<Movie> moviesOfInstructor = movieDAO.getMoviesByDirector(instructorName);

                // Tjek om der findes nogen film for den givne instruktør
                if (moviesOfInstructor.isEmpty()) {
                    // Hvis der ikke findes nogen film, returnér 404 og en besked
                    ctx.status(404).result("Ingen film fundet for instruktøren: " + instructorName);
                } else {
                    // Hvis der findes film, konverter dem til JSON og returnér som svar
                    // Konverter listen af Movie-objekter til MovieDTO-objekter
                    List<MovieAPI> movieAPIS = moviesOfInstructor.stream()
                            .map(MovieConverter::convertToDTO)
                            .toList();

                    // Konverter listen af MovieDTO til JSON og returnér den
                    String jsonResponse = movieService.convertMoviesToJson(movieAPIS);
                    ctx.contentType("application/json");
                    ctx.result(jsonResponse);
                }
            } catch (Exception e) {
                // Hvis der opstår en fejl, returnér en 500 status og en fejlbesked
                ctx.status(500).result("Fejl ved konvertering af film til JSON: " + e.getMessage());
            }
        });



        // Definer en rute til at få en Movie i JSON-format
        app.get("/movie/1", ctx -> {
            // Opret en Movie-instans
            Movie movie = Movie.builder()
                    .id(1L)
                    .imdbId(123456789L)
                    .title("Inception")
                    .overview("A skilled thief, the absolute best in the dangerous art of extraction...")
                    .releaseDate("2010-07-16")
                    .adult(false)
                    .backdropPath("/s3T1H79vZmV0IblnHlt7iwovZnT.jpg")
                    .posterPath("/q7pH8w2d88jRG59fFVZYwRYGQLb.jpg")
                    .popularity(82.473)
                    .originalLanguage("en")
                    .originalTitle("Inception")
                    .voteAverage(8.8)
                    .voteCount(28572)
                    .genres(new HashSet<>(Set.of(
                            Genre.builder().id(1L).genreId(1).name("Action").build(),
                            Genre.builder().id(2L).genreId(2).name("Science Fiction").build()
                    )))
                    .director(Director.builder().id(1L).name("Christopher Nolan").build())
                    .actors(new HashSet<>(Set.of(
                            Actor.builder().id(1L).name("Leonardo DiCaprio").build(),
                            Actor.builder().id(2L).name("Joseph Gordon-Levitt").build()
                    )))
                    .build();

            // Konverter Movie-objektet til JSON og send det som respons
            String jsonResponse = mapper.writeValueAsString(movie);
            ctx.contentType("application/json");
            ctx.result(jsonResponse);
        });



        // Definer ruter
        app.get("/movies/all", ctx -> {
            try (EntityManager em = HibernateConfig.getEntityManagerFactoryConfig(HibernateConfigState.NORMAL, "movies").createEntityManager()) {
                //MovieDAO movieDAO = new MovieDAO(em);
                List<Movie> movies = movieDAO.getAllMovies();
                // Initialize lazy collections if needed
                movies.forEach(movie -> Hibernate.initialize(movie.getGenres()));
                ctx.json(movies);
            }
        });

        app.get("/movies/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Movie movie = movieDAO.findById((long) id);
            if (movie != null) {
                ctx.json(movie);
            } else {
                ctx.status(404).result("Movie not found");
            }
        });

        app.get("/movies/genre/{genre}", ctx -> {
            String genre = ctx.pathParam("genre");
            List<Movie> movies = movieDAO.getMoviesByGenre(genre);
            ctx.json(movies);
        });

        app.get("/movies/rating/{rating}", ctx -> {
            double rating = Double.parseDouble(ctx.pathParam("rating"));
            List<Movie> movies = movieDAO.getMoviesByRating(rating);
            ctx.json(movies);
        });

        app.get("/movies/year/{year}", ctx -> {
            int year = Integer.parseInt(ctx.pathParam("year"));
            List<Movie> movies = movieDAO.getMoviesByReleaseYear(year);
            ctx.json(movies);
        });
    }
}
