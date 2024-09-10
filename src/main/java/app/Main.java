package app;

import app.persistence.daos.GenreDAO;
import app.persistence.daos.MovieDAO;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.fetcher.FilmFetcher;
import app.persistence.enums.HibernateConfigState;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {

        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);
        GenreDAO genreDAO = GenreDAO.getInstance(HibernateConfigState.NORMAL);
        FilmFetcher fetcher = new FilmFetcher(genreDAO);
        List<MovieDTO> danishMovies;

        try{
            // Kalde metoden for at oprette genrer i databasen
            fetcher.populateGenres();

            danishMovies = fetcher.fetchDanishMovies();
            printDanishMovies();


              // Opret en ny film i databasen
            System.out.println("Saving movies...");
            for (MovieDTO movie : danishMovies) {
                movieDAO.create(movie);
            }
            System.out.println("Movies saved.");

            // Udskriv film med deres genrer
            printAllMoviesWithGenres(movieDAO);
//
//            // Udskriv alle film fra databasen
//            System.out.println("Fetching and printing all movies...");
//            List<Movie> movies = movieDAO.getAllMovies();
//            for (Movie movie : movies) {
//                System.out.println(formatMovieDetails(movie));
//                System.out.println("--------------------------------------------------");
//            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            GenreDAO.close();  // Luk EntityManagerFactory, når du er færdig
            MovieDAO.close();  // Sørg for at lukke MovieDAO også
        }
    }




    private static void printAllMoviesWithGenres(MovieDAO movieDAO) {
        // Hent alle film fra databasen
        List<Movie> movies = movieDAO.getAllMovies(); // Forudsætter en findAll-metode i MovieDAO

        // Udskriv detaljer for hver film
        for (Movie movie : movies) {
            System.out.println(formatMovieDetails(movie));
        }
    }

    private static String formatMovieDetails(Movie movie) {
        StringBuilder sb = new StringBuilder();

        sb.append("ID: ").append(movie.getId()).append("\n");
        sb.append("Titel: ").append(movie.getTitle() != null ? movie.getTitle() : "Ingen titel").append("\n");
        sb.append("Oversigt: ").append(movie.getOverview() != null ? movie.getOverview() : "Ingen oversigt").append("\n");
        sb.append("Udgivelsesdato: ").append(movie.getReleaseDate() != null ? movie.getReleaseDate() : "Ingen udgivelsesdato").append("\n");
        sb.append("Rating: ").append(movie.getVoteAverage()).append("\n");
        sb.append("Voksenindhold: ").append(movie.isAdult() ? "Ja" : "Nej").append("\n");
        sb.append("Baggrundsbillede: ").append(movie.getBackdropPath() != null ? movie.getBackdropPath() : "Ingen baggrundsbillede").append("\n");
        sb.append("Plakatbillede: ").append(movie.getPosterPath() != null ? movie.getPosterPath() : "Ingen plakatbillede").append("\n");
        sb.append("Gennemsnitlig bedømmelse: ").append(movie.getRating()).append("\n");
        sb.append("Antal stemmer: ").append(movie.getVoteCount()).append("\n");

        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            sb.append("Genrer: ");
            movie.getGenres().forEach(genre -> sb.append(genre.getName()).append(", "));
            // Fjern den sidste komma og mellemrum
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 2);
            }
            sb.append("\n");
        } else {
            sb.append("Genrer: Ingen genrer\n");
        }

        return sb.toString();
    }




    private static void printGenreMap(Map<Integer, String> genreMap) {
        System.out.println("Genre Map:");
        for (Map.Entry<Integer, String> entry : genreMap.entrySet()) {
            System.out.println("ID: " + entry.getKey() + ", Name: " + entry.getValue());
        }
    }

    private static void printGenres(List<Genre> genres) {
        System.out.println("Genres List:");
        for (Genre genre : genres) {
            System.out.println("Genre ID: " + genre.getGenreId() + ", Name: " + genre.getName());
        }
    }

    private static Map<Integer, String> getGenreMap() {
        Map<Integer, String> genreMap = new HashMap<>();
        // Her kan du oprette en hardkodet map eller hente den fra en database eller en konfigurationsfil
        genreMap.put(18, "Drama");
        genreMap.put(27, "Horror");
        genreMap.put(28, "Action");
        genreMap.put(36, "History");
        genreMap.put(53, "Thriller");
        genreMap.put(80, "Crime");
        genreMap.put(9648, "Mystery");
        genreMap.put(10752, "War");
        genreMap.put(35, "Comedy");
        // Tilføj flere genre ID'er og navne efter behov
        return genreMap;
    }

    private static void printDanishMovies() throws IOException {
        GenreDAO genreDAO = null;
        FilmFetcher fetcher = new FilmFetcher(genreDAO);
        List<MovieDTO> danishMovies = fetcher.fetchDanishMovies();
        Map<Integer, String> genreMap = getGenreMap(); // Hent genre map

        if (danishMovies.isEmpty()) {
            System.out.println("Ingen danske film fundet.");
        } else {
            System.out.println("Liste over danske film:");
            for (MovieDTO movie : danishMovies) {
                System.out.println("--------------------------------------------------");
                System.out.println("ID: " + movie.getId());
                System.out.println("Titel: " + movie.getTitle());
                System.out.println("Oversigt: " + movie.getOverview());
                System.out.println("Udgivelsesdato: " + movie.getReleaseDate());
                System.out.println("Poster: " + movie.getPosterPath());
                System.out.println("Vurdering: " + movie.getVoteAverage());
                System.out.println("Vurdering antal: " + movie.getVoteCount());
                System.out.println("Baggrundsbillede: " + movie.getBackdropPath());

                // Udskriv genre-navne
                Set<Integer> genreIds = movie.getGenreIds();
                if (genreIds != null && !genreIds.isEmpty()) {
                    String genreNames = genreIds.stream()
                            .map(id -> genreMap.getOrDefault(id, "Ukendt"))
                            .collect(Collectors.joining(", "));
                    System.out.println("Genre: " + genreNames);
                } else {
                    System.out.println("Genre: Ingen angivet");
                }

                System.out.println("Er voksen: " + movie.getAdult());
                System.out.println("Popularitet: " + movie.getPopularity());
                System.out.println("Originalsprog: " + movie.getOriginalLanguage());
                System.out.println("--------------------------------------------------");
            }
        }
    }





}
