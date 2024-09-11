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
    // Bruges ikke da jeg hardkoder genrerne fra et map
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
        for (int page = 1; page <= 50; page++) {
            String apiUrl = BASE_API_URL + page;
            String jsonResponse = fetchApiResponse(apiUrl);
            //System.out.println("jsonResponse: " + jsonResponse);
            extractMovies(jsonResponse);
        }
        return movieList;
    }

    // Her henter jeg JSON data fra en URL
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
            //Her mapper jeg JSON data til et JsonNode objekt
            JsonNode rootNode = mapper.readTree(jsonResponse);
            //Her henter jeg alle filmene fra JSON data
            JsonNode resultsNode = rootNode.path("results");

            for (JsonNode movieNode : resultsNode) {
                //System.out.println("movieNode: " + movieNode);
                String originalLanguage = movieNode.path("original_language").asText();

                if ("da".equals(originalLanguage)) { // Filter Danish movies
                    MovieDTO movie = new MovieDTO();
                    movie.setImdbId(movieNode.path("id").asLong());
                    movie.setTitle(movieNode.path("title").asText());
                    movie.setOverview(movieNode.path("overview").asText());
                    movie.setReleaseDate(movieNode.path("release_date").asText());
                    movie.setPosterPath(movieNode.path("poster_path").asText());
                    movie.setVoteAverage(movieNode.path("vote_average").asDouble());
                    movie.setVoteCount(movieNode.path("vote_count").asInt());
                    movie.setBackdropPath(movieNode.path("backdrop_path").asText());
                    movie.setGenreIds(parseGenreIds(movieNode.path("genre_ids")));
                    movie.setIsAdult(movieNode.path("adult").asBoolean());
                    movie.setOriginalTitle(movieNode.path("original_title").asText());
                    movie.setPopularity(movieNode.path("popularity").asDouble());
                    movie.setOriginalLanguage(originalLanguage);

                    movieList.add(movie);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Her parser jeg genreIds fra JSON data og returnerer et Set af genreIds
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
        genreMap.put(12, "Eventyr");
        genreMap.put(16, "Animation");
        genreMap.put(35, "Komedie");
        genreMap.put(80, "Krimi");
        genreMap.put(99, "Dokumentar");
        genreMap.put(18, "Drama");
        genreMap.put(10751, "Familie");
        genreMap.put(14, "Fantasy");
        genreMap.put(36, "Historie");
        genreMap.put(27, "Gyser");
        genreMap.put(10402, "Musik");
        genreMap.put(9648, "Mystery");
        genreMap.put(10749, "Romantik");
        genreMap.put(878, "Science Fiction");
        genreMap.put(10770, "TV-film");
        genreMap.put(53, "Thriller");
        genreMap.put(10752, "Krig");
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

    // Her udskriver jeg JSON data for alle film
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
