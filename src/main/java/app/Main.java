package app;

import app.config.ApplicationConfig;
import app.daos.MovieDAO;
import app.daos.GenreDAO;
import app.dtos.MovieDTO;
import app.fetcher.FilmFetcher;
import app.services.MovieService;
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

        if(movieDAO.hasMovies()){
            System.out.println("Movies already in database");
        } else {
            List<MovieDTO> danishMovies;

            // Kontroller om tabellerne er tomme inden jeg opretter dem
            fetcher.populateGenres();

            // Kontroller om tabellerne er tomme inden jeg opretter dem
            danishMovies = fetcher.fetchDanishMovies();

            // Opretter alle film en ad gangen i databasen
            System.out.println("Saving movies...");
            for (MovieDTO movieDTO : danishMovies) {
                movieDAO.createNewMovie(movieDTO);
            }
            System.out.println("Movies saved.");
        }
        ApplicationConfig.startServer(7070);
    }
}
