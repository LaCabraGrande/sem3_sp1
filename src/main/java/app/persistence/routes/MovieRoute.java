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

        movieService = new MovieService(movieDAO);
        movieController = new MovieController(movieService);
    }
    // Her defineres alle endpoints for MovieController
    public EndpointGroup getMovieRoutes() {
        return () -> {
            get("movies/all", movieController::getAllMovies);
            get("movies/rating/{rating}", movieController::getMoviesByRating);
            get("movies/genre/{genre}", movieController::getMoviesByGenre);
            get("movies/year/{year}", movieController::getMoviesFromYear);
            get("movies/imdb/{imdbId}", movieController::getMovieByImdbId);
            get("moviesbyinstructor/{instructor}", movieController::getMoviesByInstructor);
            get("moviesbyactor/{actor}", movieController::getMoviesByActor);
            get("moviesbytitle/{title}", movieController::getMoviesByTitle);
            get("movies/minvotes/{minVoteCount}", movieController::getMoviesWithMinimumVotes);
        };
    }
}
