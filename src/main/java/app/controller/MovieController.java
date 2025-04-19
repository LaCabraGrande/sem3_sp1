package app.controller;

import app.config.HibernateConfig;
import app.daos.MovieDAO;
import app.dtos.MovieDTO;
import app.dtos.FilterCountDTO;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.services.MovieService;
import app.exceptions.ApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieController {
    private final MovieDAO movieDAO;
    private final MovieService movieService = new MovieService();
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);
    private static final List<String> SUPPORTED_FILTERS = List.of("genre", "year", "language", "rating", "director", "actor", "title");

    public MovieController() {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        this.movieDAO = MovieDAO.getInstance(emf);
    }

    public void getAllMovies(Context ctx) throws ApiException {
        try {
            String pageParam = ctx.queryParam("page");
            String sizeParam = ctx.queryParam("size");

            int page = (pageParam != null) ? Integer.parseInt(pageParam) : 0;
            int size = (sizeParam != null) ? Integer.parseInt(sizeParam) : 20;

            List<MovieDTO> movies = movieDAO.getMovies(page, size).stream()
                    .map(MovieDTO::new)
                    .toList();

            logger.info("✅ Hentede {} film (page: {}, size: {})", movies.size(), page, size);
            ctx.json(movies);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Ugyldige værdier for page eller size", e);
        } catch (Exception e) {
            logger.error("❌ Fejl ved hentning af filmene", e);
            throw new ApiException(500, "Der opstod en fejl ved hentning af filmene", e);
        }
    }

    public void getFilteredCounts(Context ctx) throws ApiException {
        try {
            Map<String, List<String>> filterParams = new HashMap<>();
            for (String key : SUPPORTED_FILTERS) {
                List<String> values = ctx.queryParams(key);
                if (values != null && !values.isEmpty()) {
                    filterParams.put(key, values);
                }
            }

            FilterCountDTO filterCountDTO = movieDAO.getFilteredCounts(filterParams);
            logger.info("✅ Beregnede filter-tællinger");
            ctx.json(filterCountDTO);
        } catch (Exception e) {
            logger.error("❌ Fejl i getFilteredCounts", e);
            throw new ApiException(500, "Fejl ved hentning af filter counts", e);
        }
    }

    public void getFilteredMovies(Context ctx) throws ApiException {
        try {
            Map<String, List<String>> filterCriteria = new HashMap<>();
            for (String key : SUPPORTED_FILTERS) {
                List<String> values = ctx.queryParams(key);
                if (values != null && !values.isEmpty()) {
                    filterCriteria.put(key, values);
                }
            }

            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
            int pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

            List<MovieDTO> filteredMovies = movieDAO.getFilteredMovies(filterCriteria, page, pageSize);
            logger.info("✅ Hentede {} filtrerede film", filteredMovies.size());
            ctx.json(filteredMovies);
        } catch (Exception e) {
            logger.error("❌ Fejl i getFilteredMovies", e);
            throw new ApiException(500, "Fejl ved hentning af filtrerede film", e);
        }
    }
}
