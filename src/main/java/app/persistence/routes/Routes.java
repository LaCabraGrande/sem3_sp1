package app.persistence.routes;

import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Routes {
    private static EntityManagerFactory emf;
    private final MovieRoute movieRoute;

    public Routes(EntityManagerFactory emf) {
       movieRoute = new MovieRoute(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> {
            path("api/movies", movieRoute.getMovieRoutes());
        };
    }

}
