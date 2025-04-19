package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.daos.GenreDAO;
import app.daos.MovieDAO;
import app.exceptions.JpaException;
import app.fetcher.FilmFetcher;
import app.services.FilmService;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        MovieDAO movieDAO = MovieDAO.getInstance(emf);
        GenreDAO genreDAO = GenreDAO.getInstance(emf);
        FilmFetcher fetcher = new FilmFetcher(genreDAO);
        FilmService filmService = new FilmService(fetcher);

        try {
            if (movieDAO.hasMovies()) {
                System.out.println("📀 Databasen er allerede fyldt med film.");
                logger.info("Databasen er allerede fyldt med film.");
            } else {
                System.out.println("🎬 Ingen film i databasen. Henter film fra API (TMDB)...");
                logger.info("Ingen film i databasen. Starter dataindsamling fra API...");

                fetcher.populateGenres();
                filmService.fetchAndSaveMovies();

                System.out.println("✅ Film hentet og gemt i databasen.");
                logger.info("Film hentet og gemt i databasen.");
            }

            System.out.println("🚀 Starter Javalin-server...");
            logger.info("Starter Javalin-server.");
            ApplicationConfig.startServer();

        } catch (JpaException e) {
            System.err.println("❌ Fejl under databaseinitialisering: " + e.getMessage());
            logger.error("Fejl under databaseinitialisering", e);
        } catch (Exception e) {
            System.err.println("❌ Uventet fejl: " + e.getMessage());
            logger.error("Uventet fejl i Main", e);
        }
    }
}
