package app.persistence.fetcher;

import app.persistence.enums.HibernateConfigState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Genre;
import app.persistence.daos.GenreDAO;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class FilmFetcher {
    private static final String API_KEY = "8c82181ed8f9642ecd2de69b5e74dee0";
    private static final String BASE_API_URL = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK&sort_by=popularity.desc&with_original_language=da&page=";
    private static final String GENRE_API_URL = "https://api.themoviedb.org/3/genre/movie/list?api_key=" + API_KEY + "&language=da-DK";

    @Getter
    private List<MovieDTO> movieList = new ArrayList<>();
    private Map<Integer, String> genreMap = new HashMap<>();
    private GenreDAO genreDAO;

    public FilmFetcher(GenreDAO genreDAO) {
        this.genreDAO = genreDAO;
        initializeGenreMap();
    }

    public List<MovieDTO> fetchDanishMovies() throws IOException {
        for (int page = 1; page <= 5; page++) {
            String apiUrl = BASE_API_URL + page;
            String jsonResponse = fetchApiResponse(apiUrl);
            System.out.println("jsonResponse: " + jsonResponse);
            extractMovies(jsonResponse);
        }
        return movieList;
    }

    private static String fetchApiResponse(String apiUrl) throws IOException, MalformedURLException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void extractMovies(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            for (JsonNode movieNode : resultsNode) {
                String originalLanguage = movieNode.path("original_language").asText();

                if ("da".equals(originalLanguage)) { // Filter Danish movies
                    MovieDTO movie = new MovieDTO();
                    movie.setId((long) movieNode.path("id").asInt());
                    movie.setTitle(movieNode.path("title").asText());
                    movie.setOverview(movieNode.path("overview").asText());
                    movie.setReleaseDate(movieNode.path("release_date").asText());
                    movie.setPosterPath(movieNode.path("poster_path").asText());
                    movie.setVoteAverage(movieNode.path("vote_average").asDouble());
                    movie.setVoteCount(movieNode.path("vote_count").asInt());
                    movie.setBackdropPath(movieNode.path("backdrop_path").asText());
                    movie.setGenreIds(parseGenreIds(movieNode.path("genre_ids")));
                    movie.setAdult(movieNode.path("adult").asBoolean());
                    movie.setPopularity(movieNode.path("popularity").asDouble());
                    movie.setOriginalLanguage(originalLanguage);

                    movieList.add(movie);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<Integer> parseGenreIds(JsonNode genreIdsNode) {
        Set<Integer> genreIds = new HashSet<>();
        if (genreIdsNode.isArray()) {
            for (JsonNode node : genreIdsNode) {
                genreIds.add(node.asInt());
            }
        }
        return genreIds;
    }

    private void initializeGenreMap() {
        // Hardkodede genrer
        genreMap.put(28, "Action");
        genreMap.put(12, "Adventure");
        genreMap.put(16, "Animation");
        genreMap.put(35, "Comedy");
        genreMap.put(80, "Crime");
        genreMap.put(99, "Documentary");
        genreMap.put(18, "Drama");
        genreMap.put(10751, "Family");
        genreMap.put(14, "Fantasy");
        genreMap.put(36, "History");
        genreMap.put(27, "Horror");
        genreMap.put(10402, "Music");
        genreMap.put(9648, "Mystery");
        genreMap.put(10749, "Romance");
        genreMap.put(878, "Science Fiction");
        genreMap.put(10770, "TV Movie");
        genreMap.put(53, "Thriller");
        genreMap.put(10752, "War");
        genreMap.put(37, "Western");
    }

    public void populateGenres() {
        for (Map.Entry<Integer, String> entry: genreMap.entrySet()) {
            int genreId = entry.getKey();
            String name = entry.getValue();

            Genre genre = new Genre();
            genre.setGenreId(genreId);
            genre.setName(name);
            genreDAO.create(genre);
        }
    }

    private List<String> getGenreNames(List<Integer> genreIds) {
        List<String> genreNames = new ArrayList<>();
        for (Integer id : genreIds) {
            String genreName = genreMap.get(id);
            if (genreName != null) {
                genreNames.add(genreName);
            }
        }
        return genreNames;
    }



    public void printMovieJsonDataForAllMovies() throws Exception {
        List<MovieDTO> movies = fetchDanishMovies();
        ObjectMapper objectMapper = new ObjectMapper();

        for (MovieDTO movie : movies) {
            movie.setGenreNames(getGenreNames(movie.getGenreIds()));
            String json = objectMapper.writeValueAsString(movie);
            System.out.println("Movie JSON Data: " + json);
        }
    }

    public List<String> getGenreNames(Set<Integer> genreIds) {
        GenreDAO genreDAO = GenreDAO.getInstance(HibernateConfigState.TEST); // Skift til korrekt state
        return genreDAO.findGenresByIds(genreIds).stream()
                .map(Genre::getName)
                .collect(Collectors.toList());
    }


}
