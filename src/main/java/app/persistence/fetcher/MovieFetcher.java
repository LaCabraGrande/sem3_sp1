package fetcher;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dtos.MovieDTO;
import dtos.MovieResponseDTO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class MovieFetcher {

    private static final String API_KEY = "8c82181ed8f9642ecd2de69b5e74dee0";
    //private static final String BASE_URL = "https://api.themoviedb.org/3/movie/";
    //private static final String BASE_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final String BASE_URL = "https://api.themoviedb.org/3/find/external_id?external_source=&language=danish";


    private ObjectMapper mapper;

    public MovieFetcher() {
        mapper = new ObjectMapper();
        // Registrer JavaTimeModule for at understøtte Java 8 tidstyper
        mapper.registerModule(new JavaTimeModule());
    }


    public MovieDTO fetchMovie(int movieId) throws Exception {
        // Byg API URL med film ID og API nøgle
        String url = BASE_URL + movieId + "?api_key=" + API_KEY;

        // Lav en HTTP anmodning
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        // Hent svaret fra API'et
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Konverter JSON svaret til MovieDTO objekt ved hjælp af ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body(), MovieDTO.class);
    }

    public List<MovieDTO> fetchDanishMovies() throws Exception {
        // Byg API URL med nødvendige parametre
        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK";


        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // Log responseBody for at kontrollere indholdet
        System.out.println("Response Body: " + responseBody);

        if (responseBody.contains("status_code") || responseBody.contains("status_message")) {
            throw new RuntimeException("API Error: " + responseBody);
        }

        // Konverter JSON svaret til MovieResponseDTO objekt ved hjælp af ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Registrer JavaTimeModule

        // Her skal du definere en DTO, der repræsenterer svaret fra API'et
        MovieResponseDTO movieResponse = mapper.readValue(responseBody, MovieResponseDTO.class);

        return movieResponse.getResults();
    }




}
