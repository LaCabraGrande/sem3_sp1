package app.services;

import app.config.HibernateConfig;
import app.daos.MovieDAO;
import app.dtos.MovieDTO;
import app.exceptions.ApiException;
import app.fetcher.FilmFetcher;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FilmService {

    private static final Logger logger = LoggerFactory.getLogger(FilmService.class);
    private final FilmFetcher filmFetcher;
    private final MovieDAO movieDAO;

    public FilmService(FilmFetcher filmFetcher) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.movieDAO = MovieDAO.getInstance(emf);
        this.filmFetcher = filmFetcher;
    }

    public void fetchAndSaveMovies() {
        try {
            List<MovieDTO> movieDTOList = filmFetcher.fetchMoviesFromLastTenYears();
            System.out.println("Fetched " + movieDTOList.size() + " movies");
            logger.info("🎬 Hentede {} film fra API'et.", movieDTOList.size());

            for (MovieDTO dto : movieDTOList) {
                dto.setGenreNames(filmFetcher.getGenreNames(dto.getGenreIds()));
            }

            movieDAO.create(movieDTOList);
            System.out.println("Movies saved...");
            logger.info("✅ Alle film er nu gemt i databasen.");
        } catch (Exception e) {
            throw new ApiException(500, "Fejl ved hentning eller gemning af film", e);
        }
    }
}
