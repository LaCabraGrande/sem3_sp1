package app.persistence.controller;

import app.persistence.dtos.MovieDTO;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.persistence.services.MovieService;

import java.util.List;

public class MovieController {
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    public void getAllMovies(Context ctx) {
        try {
            logger.info("Received request to get all rooms"); // Log anmodning
            List<MovieDTO> movies = movieService.getAllMovies();  // roomService returnerer nu allerede RoomDTO
            logger.info("Returning {} rooms", movies.size()); // Log antal værelser der returneres
            ctx.json(movies);  // Returner som JSON
        } catch (Exception e) {
            logger.error("Error getting all movies: {}", e.getMessage()); // Log fejl
            ctx.status(500).result("Internal Server Error"); // Returner fejl
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
        String actor = ctx.pathParam("actor");
        List<MovieDTO> movies = movieService.getMoviesByActor(actor);
        ctx.json(movies);
    }

    public void getMoviesByTitle(Context ctx) {
        String title = ctx.pathParam("title");
        List<MovieDTO> movies = movieService.getMoviesByTitle(title);
        ctx.json(movies);
    }
}
