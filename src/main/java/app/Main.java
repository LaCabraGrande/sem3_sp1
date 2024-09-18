package app;

import app.persistence.daos.ActorDAO;
import app.persistence.daos.DirectorDAO;
import app.persistence.daos.GenreDAO;
import app.persistence.daos.MovieDAO;
import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.enums.HibernateConfigState;
import app.persistence.fetcher.FilmFetcher;
import app.persistence.services.FilmService;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class Main {
    static DecimalFormat decimalFormat = new DecimalFormat("#.#");
    static final int LINE_WIDTH = 160; // Antal tegn pr. linje

    public static void main(String[] args) {

        // Initialiser EntityManagerFactory og EntityManager
        //EntityManagerFactory emf = Persistence.createEntityManagerFactory("your_persistence_unit_name");
        //EntityManager em = emf.createEntityManager();

        // Initialisering af DAO og Fetcher
        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);
        GenreDAO genreDAO = GenreDAO.getInstance(HibernateConfigState.NORMAL);
        ActorDAO actorDAO = ActorDAO.getInstance(HibernateConfigState.NORMAL);
        DirectorDAO directorDAO = DirectorDAO.getInstance(HibernateConfigState.NORMAL);
        FilmFetcher filmFetcher = new FilmFetcher(genreDAO);

        // Her gemmer jeg alle genre i databasen
        //filmFetcher.populateGenres();

        // Opret en FilmService
        //FilmService filmService = new FilmService(filmFetcher, movieDAO, genreDAO, actorDAO, directorDAO);

        // Fetch and save movies
        //filmService.fetchAndSaveMovies();

        // Eksempel: Hent og print alle film
        List<Movie> allMovies = movieDAO.getAllMovies();
        for (Movie movie : allMovies) {
            printMovieDetails(movie);
        }

        // Eksempel: Hent film baseret på genre
        List<Movie> actionMovies = movieDAO.getMoviesByGenre("Drama");
        System.out.println("\nFilm med genren Drama tilknyttet:\n");
        for (Movie movie : actionMovies) {
            printMovieDetails(movie);
        }

        // Eksempel: Hent film baseret på rating
        List<Movie> topRatedMovies = movieDAO.getMoviesByRating(8.0);
        System.out.println("\nFilm med en rating over 8.0:\n");
        for (Movie movie : topRatedMovies) {
            printMovieDetails(movie);
        }

        // Eksempel: Hent film baseret på udgivelsesår
        List<Movie> movies2024 = movieDAO.getMoviesByReleaseYear(2024);
        System.out.println("\nFilm fra 2024:");
        for (Movie movie : movies2024) {
            printMovieDetails(movie);
        }

        // Eksempel: Hent alle skuespillere for en film
        List<Actor> actors = movieDAO.getActorsByMovieTitle("Jagten");
        System.out.println("\nSkuespillere som optræder i 'Jagten':\n");
        for (Actor actor : actors) {
            System.out.println("Actor: " + actor.getName());
        }

        // Eksempel: Hent instruktøren for en film
        Director director = movieDAO.getDirectorByMovieTitle("Jagten");
        System.out.println("\nInstruktør i 'Jagten':\n " + (director != null ? director.getName() : "Ukendt"));

        List<Movie> movies = movieDAO.findMoviesByActor("Anders W. Berthelsen");
        System.out.println("\nFilm som 'Anders W. Berthelsen' spiller med i:\n");
        for (Movie movie : movies) {
            System.out.println("- "+movie.getTitle());
        }
    }

    private static void printMovieDetails(Movie movie) {

        System.out.println("Title: " + movie.getTitle());
        System.out.println("Release Date: " + movie.getReleaseDate());
        double voteAverage = movie.getVoteAverage();
        String formattedVoteAverage = decimalFormat.format(voteAverage);
        System.out.println("Rating på IMDB: " + formattedVoteAverage);

        // Udskriv genre-navne
        System.out.print("Genrer: ");
        Set<Genre> genres = movie.getGenres();
        if (genres != null && !genres.isEmpty()) {
            System.out.println(genres.stream()
                    .map(Genre::getName)
                    .collect(Collectors.joining(", ")));
        } else {
            System.out.println("Ingen genrer tilknyttet");
        }

        // Udskriv instruktørnavn
        System.out.println("Instruktør: " + (movie.getDirector() != null ? movie.getDirector().getName() : "Unknown"));

        // Udskriv skuespiller-navne
        System.out.print("Skuespiller: ");
        Set<Actor> actors = movie.getActors();
        if (actors != null && !actors.isEmpty()) {
            System.out.println(actors.stream()
                    .map(Actor::getName)
                    .collect(Collectors.joining(", ")));
        } else {
            System.out.println("Ingen skuespillere tilknyttet");
        }
        printWrappedText("Handling : " + movie.getOverview(), LINE_WIDTH);
        System.out.println("----------------------------------------------------------------------------------------------------------------------");
    }


    // Metode til at udskrive tekst med linjeskift baseret på ønsket bredde
    private static void printWrappedText(String text, int width) {
        if (text == null || text.isEmpty()) {
            System.out.println("No overview available.");
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer(text, " ");
        StringBuilder line = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            if (line.length() + word.length() + 1 > width) {
                System.out.println(line.toString());
                line = new StringBuilder();
            }
            if (line.length() > 0) {
                line.append(" ");
            }
            line.append(word);
        }
        // Print the last line if it has content
        if (line.length() > 0) {
            System.out.println(line.toString());
        }
    }

}
