package app.config;

import app.controller.ExceptionController;
import app.exceptions.ApiException;
import app.routes.Routes;
import app.security.controllers.AccessController;
import app.security.enums.Role;
import app.security.routes.SecurityRoutes;
import app.utils.ApiProps;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfig {

    private static final Routes routes = new Routes();
    private static final AccessController accessController = new AccessController();
    private static final ExceptionController exceptionController = new ExceptionController();
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    // 🔧 Konfiguration af Javalin
    public static void configuration(JavalinConfig config) {
        config.showJavalinBanner = false;
        config.router.contextPath = ApiProps.API_CONTEXT;
        config.bundledPlugins.enableDevLogging();
        config.bundledPlugins.enableRouteOverview("/routes", Role.ANYONE);

        config.router.apiBuilder(routes.getRoutes());
        config.router.apiBuilder(SecurityRoutes.getSecuredRoutes());
        config.router.apiBuilder(SecurityRoutes.getSecurityRoutes());

        config.http.defaultContentType = "application/json";
    }

    public static void startServer() {
        Javalin app = Javalin.create(ApplicationConfig::configuration);

        // 🌍 CORS
        app.before(ApplicationConfig::corsHeaders);
        app.options("/*", ApplicationConfig::corsHeadersOptions);

        // 🔐 Access kontrol før matched routes
        app.beforeMatched(accessController::accessHandler);

        // ❗ Exception handling
        app.exception(Exception.class, exceptionController::exceptionHandler);
        app.exception(ApiException.class, exceptionController::apiExceptionHandler);

        // 🚀 Start server
        app.start(ApiProps.PORT);
        logger.info("Server started on port {}", ApiProps.PORT);
    }

    public static void stopServer(Javalin app) {
        app.stop();
        logger.info("Server stopped.");
    }

    // CORS headers
    private static void corsHeaders(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.header("Access-Control-Allow-Credentials", "true");
    }

    private static void corsHeadersOptions(Context ctx) {
        corsHeaders(ctx);
        ctx.status(204);
    }
}
