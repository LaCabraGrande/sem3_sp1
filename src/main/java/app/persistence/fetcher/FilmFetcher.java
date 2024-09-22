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
import java.time.LocalDate;
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
    private static final LocalDate today = LocalDate.now();
    private static final LocalDate startDate = today.minusYears(5);
    private static final LocalDate secondStartDate = today.minusMonths(13);
    private static final LocalDate thirdStartDate = today.minusDays(7);
    private static final String BASE_API_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final String BASE_API_URL_OLD = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK&sort_by=popularity.desc&with_original_language=da&page=";

    private static final String BASE_API_URL_DANISH_MOVIES_SPECIFIC_PERIOD = BASE_API_URL
            + "?api_key="
            + API_KEY
            + "&language=da-DK"
            + "&with_original_language=da"
            + "&primary_release_date.gte="
            + startDate
            + "&primary_release_date.lte="
            + today
            + "&sort_by=popularity.desc"
            + "&page=";

    private static final String BASE_API_URL_ALL_DANISH_MOVIES = BASE_API_URL
            + "?api_key="
            + API_KEY
            + "&language=da-DK"
            + "&with_original_language=da"
            + "&sort_by=popularity.desc"
            + "&page=";

    private static final String BASE_API_URL_ALL_NATIONALITIES = BASE_API_URL
            + "?api_key="
            + API_KEY
            + "&without_genres=99"
            + "&with_runtime.gte=60"
            + "&vote_average.gte=5"
            + "&with_poster=true"
            + "&primary_release_date.gte="
            + secondStartDate
            + "&primary_release_date.lte="
            + today
            + "&page=";

    private static final String BASE_API_URL_DANISH_MOVIES_LAST_5_YEARS = BASE_API_URL
            + "?api_key=" + API_KEY
            + "&language=da-DK"
            + "&with_original_language=da"
            + "&primary_release_date.gte=" + startDate
            + "&primary_release_date.lte=" + today
            + "&sort_by=popularity.desc"
            + "&page=";

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
            String apiUrl1 = BASE_API_URL_DANISH_MOVIES_SPECIFIC_PERIOD + page;
            String apiUrl2 = BASE_API_URL_ALL_DANISH_MOVIES + page;
            String apiUrl3 = BASE_API_URL_ALL_NATIONALITIES + page;
            String apiUrl4 = BASE_API_URL_OLD + page;
            String apiUrl5 = BASE_API_URL_DANISH_MOVIES_LAST_5_YEARS + page;
            LOGGER.info("Fetching URL: " + apiUrl5);

            Future<List<MovieDTO>> future = executorService.submit(() -> {
                String jsonResponse = fetchApiResponse(apiUrl5);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode resultsNode = rootNode.path("results");
                if (!resultsNode.isEmpty()) {
                    return extractMovies(resultsNode);
                }
                return Collections.emptyList();
            });

            futures.add(future);

            page++;
            if (page > getTotalPages()) {
                hasMorePages = false;
            }
        }

        for (Future<List<MovieDTO>> future : futures) {
            try {
                movieList.addAll(future.get());
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
                    movie.setDuration(detailedMovie.getDuration());
                } catch (IOException | InterruptedException e) {
                    LOGGER.warning("Kunne ikke hente detaljer for film-ID: " + movie.getImdbId() + ": " + e.getMessage());
                }
                return null;
            }));
        }

        for (Future<Void> future : detailFutures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                LOGGER.severe("Fejl ved udførelse af filmhentning af detaljer: " + e.getMessage());
            }
        }
        executorService.shutdown();
        return movieList;
    }

    private int getTotalPages() throws IOException, InterruptedException {
        String firstPageUrl1 = BASE_API_URL_ALL_NATIONALITIES + "1";
        String firstPageUrl2 = BASE_API_URL_ALL_DANISH_MOVIES + "1";
        String firstPageUrl3 = BASE_API_URL_DANISH_MOVIES_SPECIFIC_PERIOD + "1";
        String firstPageUrl4 = BASE_API_URL_DANISH_MOVIES_LAST_5_YEARS + "1";
        String firstPageUrl5 = BASE_API_URL_OLD + "1";
        String jsonResponse = fetchApiResponse(firstPageUrl4);
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        return rootNode.path("total_pages").asInt();
    }

    public MovieDTO fetchMovieWithDetails(Long movieId) throws IOException, InterruptedException {
        // URL for at hente filmdetaljer
        String movieUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + API_KEY;
        // URL for at hente credits (skuespillere og instruktør)
        String creditsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;

        // Hent JSON-respons for filmen og credits
        String movieJsonResponse = fetchApiResponse(movieUrl);
        String creditsJsonResponse = fetchApiResponse(creditsUrl);

        // Læs JSON-responsen ind i JsonNode-objekter
        JsonNode movieJson = objectMapper.readTree(movieJsonResponse);
        JsonNode creditsJson = objectMapper.readTree(creditsJsonResponse);

        // Tjek og udskriv runtime
        int runtime = movieJson.has("runtime") ? movieJson.get("runtime").asInt() : 0;

        // Opret MovieDTO ved at bruge builder-patternet
        MovieDTO movieDTO = MovieDTO.builder()
                .imdbId(movieJson.get("id").asLong())
                .title(movieJson.has("title") ? movieJson.get("title").asText() : "Ukendt titel")
                .duration(movieJson.has("runtime") ? movieJson.get("runtime").asInt() : 0)
                .overview(movieJson.has("overview") ? movieJson.get("overview").asText() : "Ingen beskrivelse tilgængelig")
                .releaseDate(movieJson.has("release_date") ? movieJson.get("release_date").asText() : "Ukendt dato")
                .isAdult(movieJson.has("adult") && movieJson.get("adult").asBoolean())
                .backdropPath(movieJson.has("backdrop_path") ? movieJson.get("backdrop_path").asText() : null)
                .posterPath(movieJson.has("poster_path") ? movieJson.get("poster_path").asText() : null)
                .popularity(movieJson.has("popularity") ? movieJson.get("popularity").asDouble() : 0.0)
                .originalLanguage(movieJson.has("original_language") ? movieJson.get("original_language").asText() : "Ukendt")
                .originalTitle(movieJson.has("original_title") ? movieJson.get("original_title").asText() : "Ukendt")
                .voteAverage(movieJson.has("vote_average") ? movieJson.get("vote_average").asDouble() : 0.0)
                .voteCount(movieJson.has("vote_count") ? movieJson.get("vote_count").asInt() : 0)
                .genreIds(parseGenreIds(movieJson.path("genres")))
                .build();
        // Tilføj skuespillere og instruktør til movieDTO ved hjælp af credits JSON
        addActorsAndDirector(movieDTO, creditsJson);
        //System.out.println("MovieDTO lige inden den returneres i fetchMovieWithDetails: " + movieDTO.getDuration());
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
            MovieDTO movieDTO = MovieDTO.builder()
                        .imdbId(movieNode.path("id").asLong())
                        .title(movieNode.path("title").asText())
                        .duration(movieNode.path("runtime").asInt())
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
