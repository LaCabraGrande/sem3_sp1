package app.persistence.config;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import jakarta.persistence.EntityManagerFactory;
import app.persistence.routes.Routes;

public class ApplicationConfig {
    private static Routes routes;

    public static void configuration(JavalinConfig config) {
        config.showJavalinBanner = false;
        config.bundledPlugins.enableRouteOverview("/routes");
        config.router.contextPath = "/api"; // base path for all endpoints
        config.router.apiBuilder(routes.getRoutes());
        config.http.defaultContentType = "application/json"; // default content type for requests
    }

    public static Javalin startServer(int port, EntityManagerFactory emf) {
        routes = new Routes(emf);
        var app = Javalin.create(ApplicationConfig::configuration);
        app.start(port);
        return app;
    }

    public static void stopServer(Javalin app) {
        app.stop();
    }
}
