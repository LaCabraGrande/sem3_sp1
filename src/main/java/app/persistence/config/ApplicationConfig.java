package app.persistence.config;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.rendering.template.JavalinThymeleaf; // Importer Thymeleaf
import jakarta.persistence.EntityManagerFactory;
import app.persistence.routes.Routes;

public class ApplicationConfig {
    private static Routes routes;

    public static void configuration(JavalinConfig config) {
        config.showJavalinBanner = false;
        config.bundledPlugins.enableRouteOverview("/routes");
        config.router.contextPath = "/"; // base path for all endpoints
        config.router.apiBuilder(routes.getRoutes());
        config.http.defaultContentType = "application/json";
        config.staticFiles.add("/public"); // default content type for requests
        config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.templateEngine()));
    }

    public static void startServer(int port, EntityManagerFactory emf) {
        routes = new Routes(emf);
        Javalin app = Javalin.create(ApplicationConfig::configuration);

        // Gengiv index.html ved hjælp af Thymeleaf
        //app.get("/", ctx -> ctx.redirect("/public/index.html")); // Redirect til index.html i public-mappen

        app.start(port);
    }

    public static void stopServer(Javalin app) {
        app.stop();
    }
}
