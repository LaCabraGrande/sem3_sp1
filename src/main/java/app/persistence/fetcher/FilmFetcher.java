package app.persistence.fetcher;

import app.persistence.dtos.ActorDTO;
import app.persistence.dtos.DirectorDTO;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Genre;
import app.persistence.daos.GenreDAO;
import app.persistence.exceptions.JpaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FilmFetcher {
    private static final Logger LOGGER = Logger.getLogger(FilmFetcher.class.getName());
    private static final String API_KEY = System.getenv("API_KEY");
    private static final String BASE_API_URL = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK&sort_by=popularity.desc&with_original_language=da&page=";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Getter
    private List<MovieDTO> movieList = new ArrayList<>();
    @Getter
    private Map<Integer, String> genreMap = new HashMap<>();
    private final GenreDAO genreDAO;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Thread pool
    public FilmFetcher(GenreDAO genreDAO) {
        this.genreDAO = genreDAO;
        initializeGenreMap();
    }

    public List<MovieDTO> fetchDanishMovies() throws IOException, InterruptedException {
        int page = 1;
        boolean hasMorePages = true;

        List<Future<List<MovieDTO>>> futures = new ArrayList<>();

        while (hasMorePages) {
            String apiUrl = BASE_API_URL + page;
            LOGGER.info("Fetching URL: " + apiUrl);

            Future<List<MovieDTO>> future = executorService.submit(() -> {
                String jsonResponse = fetchApiResponse(apiUrl);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode resultsNode = rootNode.path("results");
                if (!resultsNode.isEmpty()) {
                    return extractMovies(resultsNode);
                }
                return Collections.emptyList();
            });

            futures.add(future);

            page++;
            if (page > getTotalPages()) { // Antal af sider skal hentes én gang i starten
                hasMorePages = false;
            }
        }

        for (Future<List<MovieDTO>> future : futures) {
            try {
                movieList.addAll(future.get()); // Tilføj hentede film til movieList
            } catch (ExecutionException e) {
                LOGGER.severe("Fejl ved udførelse af filmhentning: " + e.getMessage());
            }
        }

        // Hent detaljer om skuespillere og instruktør i parallelle tråde
        List<Future<Void>> detailFutures = new ArrayList<>();
        for (MovieDTO movie : movieList) {
            detailFutures.add(executorService.submit(() -> {
                try {
                    MovieDTO detailedMovie = fetchMovieWithDetails(movie.getImdbId());
                    movie.setActors(detailedMovie.getActors());
                    movie.setDirector(detailedMovie.getDirector());
                } catch (IOException | InterruptedException e) {
                    LOGGER.warning("Kunne ikke hente detaljer for film-ID: " + movie.getImdbId() + ": " + e.getMessage());
                }
                return null;
            }));
        }

        for (Future<Void> future : detailFutures) {
            try {
                future.get(); // Vent på at alle detaljer er hentet
            } catch (ExecutionException e) {
                LOGGER.severe("Fejl ved udførelse af filmhentning af detaljer: " + e.getMessage());
            }
        }
        executorService.shutdown();
        return movieList;
    }

    private int getTotalPages() throws IOException, InterruptedException {
        String firstPageUrl = BASE_API_URL + "1"; // Hent første side for at få det totale sideantal
        String jsonResponse = fetchApiResponse(firstPageUrl);
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        return rootNode.path("total_pages").asInt();
    }

    // Hent detaljer om en film inklusive skuespillere og instruktør
    public MovieDTO fetchMovieWithDetails(Long movieId) throws IOException, InterruptedException {
        String movieUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + API_KEY + "&language=da-DK";
        String creditsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;

        String movieJsonResponse = fetchApiResponse(movieUrl);
        String creditsJsonResponse = fetchApiResponse(creditsUrl);

        JsonNode movieJson = objectMapper.readTree(movieJsonResponse);
        JsonNode creditsJson = objectMapper.readTree(creditsJsonResponse);

        MovieDTO movieDTO = MovieDTO.builder()
                .imdbId(movieJson.get("id").asLong())
                .title(movieJson.get("title").asText())
                .overview(movieJson.get("overview").asText())
                .releaseDate(movieJson.get("release_date").asText())
                .isAdult(movieJson.get("adult").asBoolean())
                .backdropPath(movieJson.get("backdrop_path").asText())
                .posterPath(movieJson.get("poster_path").asText())
                .popularity(movieJson.get("popularity").asDouble())
                .originalLanguage(movieJson.get("original_language").asText())
                .originalTitle(movieJson.get("original_title").asText())
                .voteAverage(movieJson.get("vote_average").asDouble())
                .voteCount(movieJson.get("vote_count").asInt())
                .genreIds(parseGenreIds(movieJson.path("genres")))
                .build();

        addActorsAndDirector(movieDTO, creditsJson);

        return movieDTO;
    }

    private String fetchApiResponse(String apiUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            LOGGER.warning("Kunne ikke hente API-svaret. Statuskode: " + response.statusCode());
            throw new IOException("Kunne ikke hente API-svaret. Statuskode: " + response.statusCode());
        }
    }

    private void addActorsAndDirector(MovieDTO movieDTO, JsonNode creditsJson) {
        DirectorDTO directorDTO = null;
        for (JsonNode crewMember : creditsJson.get("crew")) {
            if ("Director".equals(crewMember.get("job").asText())) {
                directorDTO = DirectorDTO.builder()
                        .id(crewMember.get("id").asLong())
                        .name(crewMember.get("name").asText())
                        .build();
                break;
            }
        }
        movieDTO.setDirector(directorDTO);

        Set<ActorDTO> actorDTOSet = new HashSet<>();
        for (JsonNode actor : creditsJson.get("cast")) {
            ActorDTO actorDTO = ActorDTO.builder()
                    .id(actor.get("id").asLong())
                    .name(actor.get("name").asText())
                    .build();
            actorDTOSet.add(actorDTO);
        }
        movieDTO.setActors(actorDTOSet);
    }

    private List<MovieDTO> extractMovies(JsonNode resultsNode) {
        List<MovieDTO> extractedMovies = new ArrayList<>();
        for (JsonNode movieNode : resultsNode) {
            if ("da".equals(movieNode.path("original_language").asText())) {
                MovieDTO movieDTO = MovieDTO.builder()
                        .imdbId(movieNode.path("id").asLong())
                        .title(movieNode.path("title").asText())
                        .overview(movieNode.path("overview").asText())
                        .releaseDate(movieNode.path("release_date").asText())
                        .posterPath(movieNode.path("poster_path").asText())
                        .voteAverage(movieNode.path("vote_average").asDouble())
                        .voteCount(movieNode.path("vote_count").asInt())
                        .backdropPath(movieNode.path("backdrop_path").asText())
                        .genreIds(parseGenreIds(movieNode.path("genre_ids")))
                        .isAdult(movieNode.path("adult").asBoolean())
                        .originalTitle(movieNode.path("original_title").asText())
                        .popularity(movieNode.path("popularity").asDouble())
                        .originalLanguage("da")
                        .build();
                extractedMovies.add(movieDTO);
            }
        }
        return extractedMovies;
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

    public void populateGenres() throws JpaException {
        for (Map.Entry<Integer, String> entry: genreMap.entrySet()) {
            int genreId = entry.getKey();
            String name = entry.getValue();

            Genre genre = new Genre();
            genre.setGenreId(genreId);
            genre.setName(name);
            try {
                genreDAO.create(genre); // Hvis genreDAO.create accepterer Genre entiteten
            } catch (Exception e) {
                LOGGER.severe("Kunne ikke oprette genre med ID " + entry.getKey() + " og navn " + entry.getValue() + ": " + e.getMessage());
                throw new JpaException("Kunne ikke oprette genre med ID " + entry.getKey() + " og navn " + entry.getValue(), e);
            }
        }
    }

    // Henter her genre-navne baseret på genre-IDs
    public List<String> getGenreNames(Set<Integer> genreIds) {
        return genreIds.stream()
                .map(genreId -> genreMap.getOrDefault(genreId, "Unknown Genre"))
                .collect(Collectors.toList());
    }
}
