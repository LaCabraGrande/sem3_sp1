package app.services;

import app.config.HibernateConfig;
import app.daos.MovieDAO;
import app.dtos.MovieDTO;
import app.exceptions.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MovieService {
    private static final Logger logger = LoggerFactory.getLogger(MovieService.class);

    private final MovieDAO movieDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public MovieService() {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.movieDAO = MovieDAO.getInstance(emf);
    }

    public List<MovieDTO> getAllMoviesByPageAndSize(int page, int size) {
        try {
            List<MovieDTO> movies = movieDAO.getMovies(page, size)
                    .stream()
                    .map(MovieDTO::new)
                    .toList();

            logger.info("✅ Hentede {} film (page: {}, size: {})", movies.size(), page, size);
            return movies;
        } catch (Exception e) {
            logger.error("❌ Kunne ikke hente film (page: {}, size: {})", page, size, e);
            throw new ApiException(500, "Kunne ikke hente film", e);
        }
    }

    public String convertMoviesToJson(List<MovieDTO> movieDTOs) {
        try {
            String json = mapper.writeValueAsString(movieDTOs);
            logger.info("✅ Konverterede {} film til JSON", movieDTOs.size());
            return json;
        } catch (Exception e) {
            logger.error("❌ Fejl ved konvertering af film til JSON", e);
            throw new ApiException(500, "Fejl ved konvertering til JSON", e);
        }
    }
}
