package app.persistence.fetcher;

import app.persistence.dtos.MovieDTO;
import app.persistence.dtos.MovieResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MovieFetcher {

    private static final String BASE_URL = "https://api.themoviedb.org/3/find/external_id?external_source=&language=danish";
    private static final String API_KEY = "8c82181ed8f9642ecd2de69b5e74dee0";





    private ObjectMapper mapper;

    public MovieFetcher() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public MovieDTO fetchMovie(int movieId) throws Exception {
        String url = BASE_URL + movieId + "?api_key=" + API_KEY;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return mapper.readValue(response.body(), MovieDTO.class);
    }

    public List<MovieDTO> fetchDanishMovies() throws Exception {
        //String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK";
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK";
        //String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK&sort_by=popularity.desc&with_original_language=da&page=1";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        System.out.println("Response Body: " + responseBody);

        if (responseBody.contains("status_code") || responseBody.contains("status_message")) {
            throw new RuntimeException("API Error: " + responseBody);
        }

        mapper.registerModule(new JavaTimeModule());

        MovieResponseDTO movieResponse = mapper.readValue(responseBody, MovieResponseDTO.class);

        return movieResponse.getResults();
    }

    public void printMovieJsonDataForAllMovies() throws Exception {
        List<MovieDTO> movies = fetchDanishMovies();
        ObjectMapper objectMapper = new ObjectMapper();

        for (MovieDTO movie : movies) {
            String json = objectMapper.writeValueAsString(movie);
            System.out.println("Movie JSON Data: " + json);
        }
    }

}
