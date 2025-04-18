package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import app.controller.MovieController;
import static io.javalin.apibuilder.ApiBuilder.*;

public class MovieRoute {

    private final MovieController movieController = new MovieController();

    // Her defineres alle endpoints for MovieController
    protected EndpointGroup getMovieRoutes() {
        return () -> {
            get("/all", movieController::getAllMovies);
            get("/filtermovies", movieController::getFilteredMovies);
            get("/filtercounts", movieController::getFilteredCounts);
//            get("/rating/{rating}", movieController::getMoviesByRating);
//            get("/genre/{genre}", movieController::getMoviesByGenre);
//            get("/year/{year}", movieController::getMoviesFromYear);
//            get("/imdb/{imdbId}", movieController::getMovieByImdbId);
//            get("/instructor/{instructor}", movieController::getMoviesByInstructor);
//            get("/actor/{actor}", movieController::getMoviesByActor);
//            get("/title/{title}", movieController::getMoviesByTitle);
//            get("/minvotes/{minVoteCount}", movieController::getMoviesWithMinimumVotes);
        };
    }
}
