package app.utility;

import app.apis.MovieAPI;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class JsonUtil {
    ObjectMapper mapper = new ObjectMapper();

    // Konverterer en liste af MovieAPIs til en JSON-String som returneres
    public String convertMovieToJson(MovieAPI movieAPIs) throws Exception {
        try {
            return mapper.writeValueAsString(movieAPIs);
        } catch (Exception e) {
            throw new Exception("Fejl ved konvertering til JSON: " + e.getMessage());
        }
    }

    // Konverterer en liste af MovieAPIs til en JSON-String som returneres
    public String convertListOfMoviesToJson(List<MovieAPI> movieAPIs) throws Exception {
        try {
            return mapper.writeValueAsString(movieAPIs);
        } catch (Exception e) {
            throw new Exception("Fejl ved konvertering til JSON: " + e.getMessage());
        }
    }
}
