package app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dtos.FilmDTO;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class FilmFetcher {
    private static final String API_KEY = "8c82181ed8f9642ecd2de69b5e74dee0";
    private static final String BASE_API_URL = "https://api.themoviedb.org/3/discover/movie?api_key=" + API_KEY + "&language=da-DK&sort_by=popularity.desc&with_original_language=da&page=";

    @Getter
    private List<FilmDTO> filmListe = new ArrayList<>();

    public static void main(String[] args) {
        try {
            FilmFetcher filmFetcher = new FilmFetcher();

            // Loop for at hente flere sider
            for (int side = 1; side <= 5; side++) {  // Henter de første 5 sider, kan justeres
                String apiUrl = BASE_API_URL + side;
                String jsonResponse = fetchApiResponse(apiUrl);

                filmFetcher.hentFilm(jsonResponse);
            }

            // Nu kan du tilgå listen af film via filmFetcher.getFilmListe()
            filmFetcher.getFilmListe().forEach(System.out::println);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fetchApiResponse(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection forbindelse = (HttpURLConnection) url.openConnection();
        forbindelse.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(forbindelse.getInputStream()))) {
            StringBuilder svar = new StringBuilder();
            String linje;
            while ((linje = reader.readLine()) != null) {
                svar.append(linje);
            }
            return svar.toString();
        }
    }

    public void hentFilm(String jsonSvar) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rodNode = mapper.readTree(jsonSvar);
            JsonNode resultaterNode = rodNode.path("results");

            for (JsonNode filmNode : resultaterNode) {
                String originalSprog = filmNode.path("original_language").asText();

                if ("da".equals(originalSprog)) { // Filtrerer danske film
                    FilmDTO film = new FilmDTO();
                    film.setTitel(filmNode.path("title").asText());
                    film.setBeskrivelse(filmNode.path("overview").asText());
                    film.setUdgivelsesdato(filmNode.path("release_date").asText());
                    film.setPlakatSti(filmNode.path("poster_path").asText());
                    film.setGennemsnitligStemme(filmNode.path("vote_average").asDouble());
                    film.setAntalStemmer(filmNode.path("vote_count").asInt());
                    film.setBaggrundSti(filmNode.path("backdrop_path").asText());

                    // Håndtering af genre-IDs
                    List<Integer> genreIds = new ArrayList<>();
                    filmNode.path("genre_ids").forEach(genre -> genreIds.add(genre.asInt()));
                    film.setGenreIds(genreIds);

                    // Tilføj filmen til listen
                    filmListe.add(film);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
