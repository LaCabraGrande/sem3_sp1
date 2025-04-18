// Opdateret FilmFetcher med fix: fjernet "Connection" header
package app.fetcher;

import app.dtos.ActorDTO;
import app.dtos.DirectorDTO;
import app.dtos.MovieDTO;
import app.entities.Genre;
import app.daos.GenreDAO;
import app.exceptions.JpaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.util.concurrent.atomic.AtomicInteger;


public class FilmFetcher {
    private static final Logger LOGGER = Logger.getLogger(FilmFetcher.class.getName());
    private static final String API_KEY = System.getenv("API_KEY");
    private static final LocalDate today = LocalDate.now();
    private static final String BASE_API_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final LocalDate fiftyfiveYearsAgo = today.minusYears(55);
    private static final LocalDate fiveYearsAgo = today.minusYears(5);
    private static final LocalDate oneYearAgo = today.minusYears(1);

    private static final String BASE_API_URL_DANISH_MOVIES_RELEASED_LAST_5_YEARS = BASE_API_URL
            + "?api_key=" + API_KEY
            + "&with_origin_country=DK"
            + "&primary_release_date.gte=" + fiveYearsAgo
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
    private final ExecutorService executorService = Executors.newFixedThreadPool(15);

    public FilmFetcher(GenreDAO genreDAO) {
        this.genreDAO = genreDAO;
        initializeGenreMap();
    }

    public List<MovieDTO> fetchMoviesFromLastTenYears() throws IOException, InterruptedException {
        List<MovieDTO> allMovies = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 55;

        for (int year = startYear; year <= currentYear; year++) {
            LocalDate fromDate = LocalDate.of(year, 1, 1);
            LocalDate toDate = LocalDate.of(year, 12, 31);

            System.out.println("\n🔽 Henter film udgivet fra " + fromDate + " til " + toDate + " ...");

            String baseUrl = BASE_API_URL
                    + "?api_key=" + API_KEY
                    + "&without_genres=99"
                    + "&with_runtime.gte=80"
                    + "&vote_average.gte=3"
                    + "&vote_count.gte=20"
                    + "&with_poster=true"
                    + "&primary_release_date.gte=" + fromDate
                    + "&primary_release_date.lte=" + toDate
                    + "&with_release_type=3%7C6"
                    + "&page=";

            int page = 1;
            boolean hasMorePages = true;
            List<Future<List<MovieDTO>>> futures = new ArrayList<>();

            while (hasMorePages) {
                int currentPage = page;
                String paginatedUrl = baseUrl + currentPage;

                Future<List<MovieDTO>> future = executorService.submit(() -> {
                    String jsonResponse = fetchApiResponseWithRetry(paginatedUrl);
                    JsonNode rootNode = objectMapper.readTree(jsonResponse);
                    JsonNode resultsNode = rootNode.path("results");
                    return resultsNode.isEmpty() ? Collections.emptyList() : extractMovies(resultsNode);
                });

                futures.add(future);
                page++;
                if (page > 500) hasMorePages = false;
            }

            for (Future<List<MovieDTO>> future : futures) {
                try {
                    allMovies.addAll(future.get());
                } catch (ExecutionException e) {
                    LOGGER.severe("Fejl ved udførelse af filmhentning: " + e.getMessage());
                }
            }

            System.out.println("✅ Færdig med at hente film fra " + year + " (" + allMovies.size() + " film i alt indtil nu)");
        }

        System.out.println("⏳ Venter 20 sekunder før vi henter filmdetaljer...");
        Thread.sleep(20000);

        List<Future<Void>> detailFutures = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        for (MovieDTO movie : allMovies) {
            detailFutures.add(executorService.submit(() -> {
                int index = counter.incrementAndGet();
                try {

                    Thread.sleep(100);

                    MovieDTO detailedMovie = fetchMovieWithDetails(movie.getImdbId());
                    movie.setActors(detailedMovie.getActors());
                    movie.setDirector(detailedMovie.getDirector());
                    movie.setDuration(detailedMovie.getDuration());

                    if (index % 1000 == 0) {
                        String time = LocalTime.now().withNano(0).toString();
                        System.out.println("🔍 [" + time + "] Henter detaljer for film " + index + " / " + allMovies.size());
                    }
                    if (index % 1000 == 0) {
                        System.out.println("⏸️ Pause 5 sekunder efter " + index + " detaljer...");
                        Thread.sleep(5000);
                    }
                    Thread.sleep(20); // buffer
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
        System.out.println("\n🎬 Hentede i alt " + allMovies.size() + " film fra perioden " + startYear + "–" + currentYear + "\n");
        return allMovies;
    }


    private String fetchApiResponseWithRetry(String apiUrl) throws IOException, InterruptedException {
        try {
            return fetchApiResponse(apiUrl);
        } catch (IOException e) {
            LOGGER.warning("Første forsøg fejlede, forsøger igen: " + apiUrl);
            Thread.sleep(500);
            return fetchApiResponse(apiUrl);
        }
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
            throw new IOException("Kunne ikke hente API-svaret. Statuskode: " + response.statusCode());
        }
    }

    private MovieDTO fetchMovieWithDetails(Long movieId) throws IOException, InterruptedException {
        String movieUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + API_KEY;
        String creditsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + API_KEY;

        String movieJsonResponse = fetchApiResponseWithRetry(movieUrl);
        String creditsJsonResponse = fetchApiResponseWithRetry(creditsUrl);

        JsonNode movieJson = objectMapper.readTree(movieJsonResponse);
        JsonNode creditsJson = objectMapper.readTree(creditsJsonResponse);

        MovieDTO movieDTO = MovieDTO.builder()
                .imdbId(movieJson.get("id").asLong())
                .title(movieJson.has("title") ? movieJson.get("title").asText() : "Ukendt titel")
                .duration(movieJson.has("runtime") ? movieJson.get("runtime").asInt() : 0)
                .overview(movieJson.has("overview") ? movieJson.get("overview").asText() : "Ingen beskrivelse")
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

        addActorsAndDirector(movieDTO, creditsJson);
        return movieDTO;
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
                    .originalLanguage(movieNode.path("original_language").asText())
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
        for (Map.Entry<Integer, String> entry : genreMap.entrySet()) {
            Long genreId = entry.getKey().longValue();
            String name = entry.getValue();
            try {
                Genre existingGenre = genreDAO.findById(genreId);
                if (existingGenre == null) {
                    Genre genre = new Genre();
                    genre.setGenreId(Math.toIntExact(genreId));
                    genre.setName(name);
                    genreDAO.create(genre);
                } else {
                    System.out.println("Genre med ID " + genreId + " eksisterer allerede: " + existingGenre.getName());
                }
            } catch (Exception e) {
                throw new JpaException("Kunne ikke oprette eller tjekke genre med ID " + genreId + " og navn " + name, e);
            }
        }
    }

    public List<String> getGenreNames(Set<Integer> genreIds) {
        return genreIds.stream()
                .map(genreId -> genreMap.getOrDefault(genreId, "Unknown Genre"))
                .collect(Collectors.toList());
    }
}