package app.persistence.services;

import app.persistence.dtos.MovieDTO;

import java.util.List;
import java.util.stream.Collectors;

public class MovieService {

    private List<MovieDTO> movies;

    public MovieService(List<MovieDTO> movies) {
        this.movies = movies;
    }

    public List<MovieDTO> getByRating(double rating) {
        // Filtrer filmene baseret på rating
        return movies.stream()
                .filter(movie -> movie.getRating() >= rating)
                .collect(Collectors.toList());
    }

    public List<MovieDTO> getSortedByReleaseDate() {
        // Sorter filmene efter udgivelsesdato i faldende rækkefølge
        return movies.stream()
                .sorted((m1, m2) -> m2.getReleaseDate().compareTo(m1.getReleaseDate()))
                .collect(Collectors.toList());
    }
}
