package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final MovieRoute movieRoute = new MovieRoute();

    public EndpointGroup getRoutes() {
        return () -> {
            path("api/movies", movieRoute.getMovieRoutes());
        };
    }
}
