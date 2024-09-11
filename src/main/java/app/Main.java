package app;

import app.persistence.daos.GenreDAO;
import app.persistence.daos.MovieDAO;
import app.persistence.dtos.MovieDTO;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.fetcher.FilmFetcher;
import app.persistence.enums.HibernateConfigState;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);
        GenreDAO genreDAO = GenreDAO.getInstance(HibernateConfigState.NORMAL);
        FilmFetcher fetcher = new FilmFetcher(genreDAO);
        List<MovieDTO> danishMovies;

        try{
            // Opretter alle genrer i tabellen genre i databasen
            fetcher.populateGenres();
            danishMovies = fetcher.fetchDanishMovies();

            // Opretter alle film en ad gangen i databasen
            System.out.println("Saving movies...");
            for (MovieDTO movie : danishMovies) {
                movieDAO.create(movie);
            }
            System.out.println("Movies saved.");

            // Udskriver alle film der ligger i Databasen
            //printAllMoviesWithGenres(movieDAO);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            GenreDAO.close();  // Luk EntityManagerFactory, når du er færdig
            MovieDAO.close();  // Sørg for at lukke MovieDAO også
        }
    }

    private static void printAllMoviesWithGenres(MovieDAO movieDAO) {
        List<Movie> movies = movieDAO.getAllMovies();
        for (Movie movie : movies) {
            System.out.println(formatMovieDetails(movie));
        }
    }

    private static String formatMovieDetails(Movie movie) {
        StringBuilder sb = new StringBuilder();

        sb.append("ID: ").append(movie.getId()).append("\n");
        sb.append("IMDb ID: ").append(movie.getImdbId() != null ? movie.getImdbId() : "Ingen IMDb ID").append("\n");
        sb.append("Titel: ").append(movie.getTitle() != null ? movie.getTitle() : "Ingen titel").append("\n");
        sb.append("Originaltitel: ").append(movie.getOriginalTitle() != null ? movie.getOriginalTitle() : "Ingen originaltitel").append("\n");
        sb.append("Oversigt: ").append(movie.getOverview() != null ? movie.getOverview() : "Ingen oversigt").append("\n");
        sb.append("Udgivelsesdato: ").append(movie.getReleaseDate() != null ? movie.getReleaseDate() : "Ingen udgivelsesdato").append("\n");
        sb.append("Rating: ").append(movie.getVoteAverage()).append("\n");
        sb.append("Antal stemmer: ").append(movie.getVoteCount()).append("\n");
        sb.append("Voksenindhold: ").append(movie.isAdult() ? "Ja" : "Nej").append("\n");
        sb.append("Baggrundsbillede: ").append(movie.getBackdropPath() != null ? movie.getBackdropPath() : "Ingen baggrundsbillede").append("\n");
        sb.append("Plakatbillede: ").append(movie.getPosterPath() != null ? movie.getPosterPath() : "Ingen plakatbillede").append("\n");
        sb.append("Popularitet: ").append(movie.getPopularity()).append("\n");
        sb.append("Originalsprog: ").append(movie.getOriginalLanguage() != null ? movie.getOriginalLanguage() : "Ingen originalsprog").append("\n");

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
        sb.append("---------------------------------------------------------------------");
        return sb.toString();
    }

    private static void printGenres(List<Genre> genres) {
        System.out.println("Genres List:");
        for (Genre genre : genres) {
            System.out.println("Genre ID: " + genre.getGenreId() + ", Name: " + genre.getName());
        }
    }
}
