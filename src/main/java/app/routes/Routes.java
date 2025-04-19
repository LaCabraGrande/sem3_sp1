package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final MovieRoute movieRoute = new MovieRoute();

    public EndpointGroup getRoutes() {
        return () -> {
            // Tilføj health route
            get("health", ctx -> ctx.result("OK"));

            // Flyt movies ind uden ekstra "api"
            path("movies", movieRoute.getMovieRoutes());
        };
    }
}
