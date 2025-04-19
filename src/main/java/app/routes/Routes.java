package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;


public class Routes {
    private final MovieRoute movieRoute = new MovieRoute();
    private static final Instant serverStart = Instant.now();

    public EndpointGroup getRoutes() {
        return () -> {
            get("health", ctx -> {
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
                String timestamp = now.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy 'at' HH:mm", Locale.ENGLISH));

                Duration uptime = Duration.between(serverStart, Instant.now());
                long hours = uptime.toHours();
                long minutes = uptime.toMinutes() % 60;
                long seconds = uptime.getSeconds() % 60;

                String uptimeFormatted = String.format("%d:%02d:%02d", hours, minutes, seconds);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("service", "Movie Database API-backend");
                response.put("status", "running");
                response.put("timestamp", timestamp);
                response.put("timezone", now.getZone().toString());
                response.put("uptime", uptimeFormatted);

                ctx.json(response);
            });

            path("movies", movieRoute.getMovieRoutes());
        };
    }
}

