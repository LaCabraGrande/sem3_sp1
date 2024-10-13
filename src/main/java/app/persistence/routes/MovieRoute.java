package app.persistence.routes;

import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;
import app.persistence.controller.MovieController;
import app.persistence.services.MovieService;
import app.persistence.daos.MovieDAO;
import static io.javalin.apibuilder.ApiBuilder.*;

public class MovieRoute {
    EntityManagerFactory emf;
    MovieService movieService;
    MovieController movieController;
    MovieDAO movieDAO;

    public MovieRoute(EntityManagerFactory emf) {
        this.emf = emf;

        // Først initialiseres movieDAO, så den kan bruges til at initialisere movieService
        movieDAO = new MovieDAO(emf);
        movieService = new MovieService(movieDAO);
        movieController = new MovieController(movieService);
    }

    // Her defineres alle endpoints for MovieController
    public EndpointGroup getMovieRoutes() {
        return () -> {
            get("/all", movieController::getAllMovies);
            get("/rating/{rating}", movieController::getMoviesByRating);
            get("/genre/{genre}", movieController::getMoviesByGenre);
            get("/year/{year}", movieController::getMoviesFromYear);
            get("/imdb/{imdbId}", movieController::getMovieByImdbId);
            get("/instructor/{instructor}", movieController::getMoviesByInstructor);
            get("/actor/{actor}", movieController::getMoviesByActor);
            get("/title/{title}", movieController::getMoviesByTitle);
            get("/minvotes/{minVoteCount}", movieController::getMoviesWithMinimumVotes);
        };
    }
}
