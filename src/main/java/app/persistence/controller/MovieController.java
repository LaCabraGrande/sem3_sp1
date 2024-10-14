package app.persistence.controller;

import app.persistence.apis.MovieAPI;
import app.persistence.config.HibernateConfig;
import app.persistence.daos.MovieDAO;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Movie;
import app.persistence.services.MovieConverter;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.persistence.services.MovieService;

import java.util.Comparator;
import java.util.List;

public class MovieController {
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);
    private static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory("moviedb");
    private final MovieService movieService;
    private final MovieDAO movieDAO;

    public MovieController(MovieService movieService) {
        this.movieDAO = new MovieDAO(emf);
        this.movieService = new MovieService(movieDAO);
    }

    public void getAllMovies(Context ctx) {
        try {
            String pageParam = ctx.queryParam("page");
            String sizeParam = ctx.queryParam("size");

            int page = (pageParam != null) ? Integer.parseInt(pageParam) : 0;
            int size = (sizeParam != null) ? Integer.parseInt(sizeParam) : 20;

            List<MovieAPI> movieAPIS = movieService.getAllMoviesByPageAndSize(page, size);

            logger.info("Modtaget {} film", movieAPIS.size()); // Log antal modtagne film
            ctx.json(movieAPIS); // Returner liste af værelser
        } catch (NumberFormatException e) {
            ctx.status(400).result("Ugyldige værdier for page eller size");
        } catch (Exception e) {
            logger.error("Der opstod en fejl ved hentning af filmene: {}", e.getMessage()); // Log fejl
            ctx.status(500).result("Der opstod en fejl: " + e.getMessage());
        }
    }

    public void getMoviesByRating(Context ctx) {
        double rating = Double.parseDouble(ctx.pathParam("rating"));
        List<MovieDTO> movies = movieService.getMoviesByRating(rating);
        ctx.json(movies);
    }

    public void getMoviesByGenre(Context ctx) {
        String genre = ctx.pathParam("genre");
        List<MovieDTO> movies = movieService.getMoviesByGenre(genre);
        ctx.json(movies);
    }

    public void getMoviesFromYear(Context ctx) {
        int year = Integer.parseInt(ctx.pathParam("year"));
        List<MovieDTO> movies = movieService.getMoviesFromYear(year);
        ctx.json(movies);
    }

    public void getMoviesWithMinimumVotes(Context ctx) {
        int minVoteCount = Integer.parseInt(ctx.pathParam("minVoteCount"));
        List<MovieDTO> movies = movieService.getMoviesWithMinimumVotes(minVoteCount);
        ctx.json(movies);
    }

    public void getMovieByImdbId(Context ctx) {
        String imdbId = ctx.pathParam("imdbId");
        MovieDTO movie = movieService.getMovieByImdbId(imdbId);
        if (movie != null) {
            ctx.json(movie);
        } else {
            ctx.status(404).result("Movie not found");
        }
    }

    public void getMoviesByInstructor(Context ctx) {
         String instructor = ctx.pathParam("instructor");

        List<MovieDTO> movies = movieService.getMoviesByInstructor(instructor);

        ctx.json(movies);
    }

    public void getMoviesByActor(Context ctx) {
        String actorName = ctx.pathParam("actor");

        try {
            List<MovieDTO> moviesOfActor = movieService.getMoviesByActor(actorName);

            if (moviesOfActor.isEmpty()) {
                ctx.status(404).result("No movies found with the actor: " + actorName);
            } else {
                ctx.json(moviesOfActor);
            }
        } catch (Exception e) {
            logger.error("Error fetching movies by actor: " + e.getMessage(), e);
            ctx.status(500).result("An unexpected error occurred. Please try again later.");
        }
    }


    public void getMoviesByTitle(Context ctx) {
        try {
            String title = ctx.pathParam("title");
            System.out.println("\nTitle som vi får fra mit API-kald i movies.http: " + title);
            System.out.println("\n");

            if (title == null || title.trim().isEmpty()) {
                ctx.status(400).result("Title parameter is missing or empty");
                return;
            }

            List<MovieDTO> movieDTOS = movieService.getMoviesByTitle(title);

            if (movieDTOS.isEmpty()) {
                ctx.status(404).result("No movies found with the title: " + title);
            } else {
                ctx.json(movieDTOS);
            }

        } catch (Exception e) {
            // Log undtagelsen for at se hvad der går galt
            logger.error("Error fetching movies by title: " + e.getMessage(), e);
            ctx.status(500).result("An unexpected error occurred. Please try again later.");
        }
    }


}
