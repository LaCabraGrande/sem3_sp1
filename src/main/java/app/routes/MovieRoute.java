package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import app.controller.MovieController;
import app.security.enums.Role; // 👈 Husk at importere dine roller
import static io.javalin.apibuilder.ApiBuilder.*;

public class MovieRoute {

    private final MovieController movieController = new MovieController();

    // Her defineres alle endpoints for MovieController med roller
    protected EndpointGroup getMovieRoutes() {
        return () -> {
            get("/all", movieController::getAllMovies, Role.ANYONE);
            get("/filtermovies", movieController::getFilteredMovies, Role.ANYONE);
            get("/filtercounts", movieController::getFilteredCounts, Role.ANYONE);
        };
    }
}
