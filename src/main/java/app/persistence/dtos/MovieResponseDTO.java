package app.persistence.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MovieResponseDTO {
    @JsonProperty("page")
    private int page;

    @JsonProperty("results")
    private List<MovieDTO> results; // Liste af film

    @JsonProperty("total_results")
    private int totalResults;

    @JsonProperty("total_pages")
    private int totalPages;
}