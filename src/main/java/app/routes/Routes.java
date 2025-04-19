package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;

public class Routes {
    private final MovieRoute movieRoute = new MovieRoute();
    private static final Instant serverStart = Instant.now(); // registreres ved opstart

    public EndpointGroup getRoutes() {
        return () -> {
            get("health", ctx -> {
                String timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy 'at' HH:mm", Locale.ENGLISH));

                Duration uptime = Duration.between(serverStart, Instant.now());
                long hours = uptime.toHours();
                long minutes = uptime.toMinutes() % 60;
                long seconds = uptime.getSeconds() % 60;

                String uptimeFormatted = String.format("%d:%02d:%02d", hours, minutes, seconds);

                ctx.json(Map.of(
                        "status", "running",
                        "service", "Movie Database API-backend",
                        "timestamp", timestamp,
                        "uptime", uptimeFormatted
                ));
            });

            path("movies", movieRoute.getMovieRoutes());
        };
    }
}

