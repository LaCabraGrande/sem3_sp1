package app;

import app.config.ApplicationConfig;
import app.daos.MovieDAO;
import app.daos.GenreDAO;
import app.dtos.MovieDTO;
import app.fetcher.FilmFetcher;
import app.services.FilmService;
import jakarta.persistence.EntityManagerFactory;
import app.config.HibernateConfig;
import java.io.IOException;
import java.util.List;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        MovieDAO movieDAO = MovieDAO.getInstance(emf);
        GenreDAO genreDAO = GenreDAO.getInstance(emf);
        FilmFetcher fetcher = new FilmFetcher(genreDAO);
        FilmService filmService = new FilmService(fetcher);
        if(movieDAO.hasMovies()){
            System.out.println("Databasen er allerede fyldt med film.");
        } else {
            System.out.println("Ingen film i databasen. Henter film fra API(TMDB)...\n");
            fetcher.populateGenres();
            filmService.fetchAndSaveMovies();
        }
        ApplicationConfig.startServer(7070);
    }
}
