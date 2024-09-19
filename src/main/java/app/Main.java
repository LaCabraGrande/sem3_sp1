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
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class Main {
    static final int LINE_WIDTH = 160;

    public static void main(String[] args) {
        // Opretter DAO-objekter og FilmFetcher til brug i FilmService
        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);
        GenreDAO genreDAO = GenreDAO.getInstance(HibernateConfigState.NORMAL);
        ActorDAO actorDAO = ActorDAO.getInstance(HibernateConfigState.NORMAL);
        DirectorDAO directorDAO = DirectorDAO.getInstance(HibernateConfigState.NORMAL);
        FilmFetcher filmFetcher = new FilmFetcher(genreDAO);

        // Her gemmer jeg alle genre i databasen
        //filmFetcher.populateGenres();

        // Opretter her en FilmService
        //FilmService filmService = new FilmService(filmFetcher, movieDAO, genreDAO, actorDAO, directorDAO);

        // Her henter jeg og gemmer filmene i databasen
        //filmService.fetchAndSaveMovies();

        // Her optæller jeg antallet af film i databasen
         System.out.println("Antal film i databasen: " + movieDAO.countMovies());

        // Her sletter jeg en film film baseret på titlen
        movieDAO.deleteByTitle("Festen");

        // Her opretter jeg 'Festen' igen med alle data og relaterede entiteter med Builder-metoden
        Movie newMovie = Movie.builder()
                .imdbId(1234567L)
                .title("Festen")
                .overview("Familie og venner er samlede for at fejre Helges 60 års fødselsdag. Under middagen holder den ældste søn en tale, der afslører en forfærdelig familiehemmelighed. I løbet af aftenen oprulles lag på lag af den grufulde fortid.")
                .releaseDate("1998-08-21")
                .voteAverage(8.0)
                .voteCount(43000)
                .popularity(32.432)
                .originalLanguage("da")
                .originalTitle("Festen")
                .backdropPath("/path/to/backdrop.jpg")
                .posterPath("/path/to/poster.jpg")
                .adult(false)
                .director(Director.builder()
                        .name("Thomas Vinterberg")
                        .build())
                .genres(new HashSet<>(Set.of(
                        Genre.builder()
                                .genreId(18)
                                .name("Drama")
                                .build(),
                        Genre.builder()
                                .genreId(53)
                                .name("Thriller")
                                .build())))
                .actors(new HashSet<>(Set.of(
                        Actor.builder()
                                .name("Ulrich Thomsen")
                                .build(),
                        Actor.builder()
                                .name("Henning Moritzen")
                                .build())))
                .build();

        // Tilføj filmen til databasen
        movieDAO.createNewMovie(newMovie);

        // Bekræftelse
        System.out.println("Filmen blev tilføjet: " + newMovie.getTitle());

        // Eksempel: Hent og print alle film
        //List<Movie> allMovies = movieDAO.getAllMovies();
        //for (Movie movie : allMovies) {
        //    printMovieDetails(movie);
        //}

        // Henter her film baseret på en genre
        List<Movie> actionMovies = movieDAO.getMoviesByGenre("Drama");
        System.out.println("\nFilm med genren Drama tilknyttet:\n");
        for (Movie movie : actionMovies) {
            printMovieDetails(movie);
        }

        // Henter her film baseret på rating
        List<Movie> topRatedMovies = movieDAO.getMoviesByRating(8.0);
        System.out.println("\nFilm med en rating over 8.0:\n");
        for (Movie movie : topRatedMovies) {
            printMovieDetails(movie);
        }

        // Hent her film baseret på udgivelsesår
        List<Movie> movies2024 = movieDAO.getMoviesByReleaseYear(2020);
        System.out.println("\nFilm fra 2020:");
        for (Movie movie : movies2024) {
            printMovieDetails(movie);
        }

        // Henter her alle skuespillere for en film
        List<Actor> actors = movieDAO.getActorsByMovieTitle("Jagten");
        System.out.println("\nSkuespillere som optræder i 'Jagten':\n");
        for (Actor actor : actors) {
            System.out.println("Actor: " + actor.getName());
        }

        // Henter her instruktøren for en film
        Director director = movieDAO.getDirectorByMovieTitle("Jagten");
        System.out.println("\nInstruktør i 'Jagten':\n " + (director != null ? director.getName() : "Ukendt"));

        // Henter her alle film som en skuespiller optræder i
        List<Movie> movies = movieDAO.findMoviesByActor("Anders W. Berthelsen");
        System.out.println("\nFilm som 'Anders W. Berthelsen' spiller med i:\n");
        for (Movie movie : movies) {
            System.out.println("- "+movie.getTitle());
        }
        // En metode til at opdaterer release-datoen for en film
        movieDAO.updateMovieReleaseDate("Jagten", "2024-01-01");

        // En metode til at søge efter film baseret på en del af titlen (case-insensitive)
        List<Movie> moviesByTitle = movieDAO.searchMoviesByTitle("Under sandet");
        System.out.println("\nFilm som indeholder 'Under sandet' i titlen:\n");
        moviesByTitle.forEach(Main::printMovieDetails);

        // Her udregner jeg den gennemsnitlige rating for alle film
        double averageRating = movieDAO.getTotalAverageRating();
        System.out.printf("\nGennemsnitlig rating for alle film i Databasen: %.1f%n", averageRating);

        // Få titlerne på de top-10 laveste ratede film
        List<Movie> lowestRatedMovies = movieDAO.getTop10LowestRatedMovies();
        System.out.println("\nTop 10 laveste ratede film:\n");
        for (Movie movie : lowestRatedMovies) {
            System.out.println(movie.getTitle() + " - Rating: " + movie.getVoteAverage());
        }

        // Få titlerne på de top-10 højeste ratede film
        List<Movie> highestRatedMovies = movieDAO.getTop10HighestRatedMovies();
        System.out.println("\nTop 10 højeste ratede film:\n");
        for (Movie movie : highestRatedMovies) {
            System.out.println(movie.getTitle() + " - Rating: " + movie.getVoteAverage());
        }

        // Få titlerne på de top-10 mest populære film
        List<Movie> mostPopularMovies = movieDAO.getTop10MostPopularMovies();
        System.out.println("\nTop 10 mest populære film:\n");
        for (Movie movie : mostPopularMovies) {
            System.out.println(movie.getTitle() + " - Popularitet: " + movie.getPopularity());
        }
    }

    private static void printMovieDetails(Movie movie) {
        System.out.println("Title: " + movie.getTitle());
        System.out.println("Udgivelsesdato: " + movie.getReleaseDate());
        System.out.println("Rating på IMDB: %.1f%n" + movie.getVoteAverage());

        // Udskriver her genre-navne
        System.out.print("Genrer: ");
        Set<Genre> genres = movie.getGenres();
        if (genres != null && !genres.isEmpty()) {
            System.out.println(genres.stream()
                    .map(Genre::getName)
                    .collect(Collectors.joining(", ")));
        } else {
            System.out.println("Ingen genrer tilknyttet");
        }

        // Udskriver her navnet på  instruktøren
        if (movie.getDirector().getName() != null) {
            System.out.println("Instruktør: " + movie.getDirector().getName());
        } else {
            System.out.println("Instruktør: Ukendt");
        }

        // Udskriver her navnet på skuespillerne
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
        if (text == null || text.length() < 12) {
            System.out.println("Handling: Ingen handling beskrevet");
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
