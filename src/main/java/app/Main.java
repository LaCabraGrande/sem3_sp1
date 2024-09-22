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
import jakarta.persistence.EntityManagerFactory;
import app.persistence.config.HibernateConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class Main {
    static final int LINE_WIDTH = 160;
    static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactoryConfig(HibernateConfigState.NORMAL, "movie");
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String WHITE = "\u001B[39m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        // Her opretter jeg instanser af mine klasser
        MovieDAO movieDAO = MovieDAO.getInstance(HibernateConfigState.NORMAL);
        GenreDAO genreDAO = GenreDAO.getInstance(HibernateConfigState.NORMAL);
        ActorDAO actorDAO = ActorDAO.getInstance(HibernateConfigState.NORMAL);
        DirectorDAO directorDAO = DirectorDAO.getInstance(HibernateConfigState.NORMAL);
        FilmFetcher filmFetcher = new FilmFetcher(genreDAO);
        //FilmService filmService = new FilmService(filmFetcher, movieDAO, genreDAO, actorDAO, directorDAO, emf);

        // Her gemmer jeg alle genre i databasen
        filmFetcher.populateGenres();

        // Opretter her en FilmService
        FilmService filmService = new FilmService(filmFetcher, movieDAO, genreDAO, actorDAO, directorDAO, emf);

        // Her henter jeg og gemmer filmene i databasen
        filmService.fetchAndSaveMovies();

        // Her optæller jeg antallet af film i databasen
         System.out.println(BLUE+"Antal film i databasen: "+ RESET + movieDAO.countMovies());

        // Eksempel: Hent og print alle film
        //List<Movie> allMovies = movieDAO.getAllMovies();
        //System.out.println("\nAntal film i databasen:\n"+allMovies.size());
        //for (Movie movie : allMovies) {
        //    printMovieDetails(movie);
        //}

        // Jeg har valgt at rense lidt ud i databasen og slette film som ikke har nogen release-dato. Dette kunne jeg have gjort da jeg
        // itererede gennem min MovieDTO-liste i FilmService men har gjort
        //int deletedMovies = movieDAO.deleteMoviesWithoutReleaseDate();
        //System.out.println(BLUE+"Slettede film uden udgivelsesdato: "+RESET + deletedMovies);

        // jeg har også valgt at slette film som har en rating på over 8.6 da det som regel er film som er blevet rated forkert
        // kunne også have gjort dette i FilmService men valgte at gøre det her
        //int deletedMovies2 = movieDAO.deleteMoviesWithRatingOver(9.0);
        //System.out.println(BLUE+"Slettede film med en rating over 9.0: "+RESET + deletedMovies2);

        // Her sletter jeg en film baseret på den angivne titel
        movieDAO.deleteByTitle("Festen");

        // Opret 'Festen' med Builder-metoden
        Movie newMovie = Movie.builder()
                .imdbId(1234567L)
                .title("Festen")
                .duration(105)
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
                .genres(new HashSet<>() {{
                    add(Genre.builder().genreId(18).name("Drama").build());
                    add(Genre.builder().genreId(53).name("Thriller").build());
                }})
                .actors(new HashSet<>() {{
                    add(Actor.builder().name("Ulrich Thomsen").build());
                    add(Actor.builder().name("Henning Moritzen").build());
                }})
                .build();

        try {
            movieDAO.createNewMovie(newMovie);
            // Bekræftelse
            System.out.println(BLUE+"Film der blev tilføjet: "+RESET + newMovie.getTitle());
        } catch (Exception e) {
            System.err.println("Der opstod en fejl under tilføjelsen af filmen: " + e.getMessage());
        }


        // Henter her film baseret på en angivet genre
        //List<Movie> specificGenre = movieDAO.getMoviesByGenre("Drama");
        //System.out.println("\nFilm med genren Drama tilknyttet:\n");
        //for (Movie movie : specificGenre) {
        //    printMovieDetails(movie);
        //}

        // Henter her film baseret på en angivet rating
        //List<Movie> topRatedMovies = movieDAO.getMoviesByRating(8.0);
        //System.out.println(BLUE+"\nFilm med en rating over 8.0:\n"+RESET);
        //for (Movie movie : topRatedMovies) {
        //    printMovieDetails(movie);
        //}

        // Jeg henter her film baseret på det angivne udgivelsesår
        //List<Movie> movies2024 = movieDAO.getMoviesByReleaseYearAndNationality(2024, "da");
        //System.out.println(BLUE+"\nFilm fra 2024:\n"+RESET);
        //for (Movie movie : movies2024) {
        //    printMovieDetails(movie);
        //}

        // Jeg henter her alle skuespillere for en angivet filmtitel
        List<Actor> actors = filmService.getActorsByMovieTitle("Retfærdighedens ryttere");
        System.out.println(BLUE+"\nSkuespillere som optræder i 'Retfærdighedens ryttere':\n"+RESET);
        for (Actor actor : actors) {
            System.out.println("Actor: " + actor.getName());
        }

        // Henter her instruktøren for en angivet film
        Director director = filmService.getDirectorByMovieTitle("Retfærdighedens ryttere");
        System.out.println("\nInstruktøren af 'Retfærdighedens ryttere': " + (director != null ? director.getName() : "Ukendt"));

        // Henter her alle film som en angivet skuespiller optræder i
        List<Movie> movies = filmService.findMoviesByActor("Anders W. Berthelsen");
        System.out.println(BLUE+"\nFilm som 'Anders W. Berthelsen' spiller med i:\n"+RESET);
        for (Movie movie : movies) {
            System.out.println("- "+movie.getTitle());
        }
        // En metode til at opdaterer release-datoen for en film
        movieDAO.updateMovieReleaseDate("Jagten", "2024-01-01");

        // En metode til at opdaterer titlen for en film
        //movieDAO.updateMovieTitle("Jagten", "Jagten 2: The Hunt Continues");

        // En metode til at søge efter film baseret på en del af titlen (case-insensitive)
        List<Movie> moviesByTitle = movieDAO.searchMoviesByTitle("Under");
        System.out.println(BLUE+"\nFilm som indeholder 'Under' i titlen:\n"+RESET);
        moviesByTitle.forEach(Main::printMovieDetails);

        // Her udregner jeg den gennemsnitlige rating for alle film i Databasen
        double averageRating = movieDAO.getTotalAverageRating();
        System.out.printf("\nGennemsnitlig rating for alle film i Databasen: %.1f%n", averageRating);

        // Få titlerne på de top-10 laveste ratede film
        List<Movie> lowestRatedMovies = movieDAO.getTop10LowestRatedMovies();
        System.out.println(BLUE+"\nTop 10 laveste ratede film:\n"+RESET);
        for (Movie movie : lowestRatedMovies) {
            System.out.println("- "+movie.getTitle() + " - Rating: " + movie.getVoteAverage());
        }

        // Få titlerne på de top-10 højeste ratede film
        List<Movie> highestRatedMovies = movieDAO.getTop10HighestRatedMovies();
        System.out.println(BLUE+"\nTop 10 højeste ratede film:\n"+RESET);
        for (Movie movie : highestRatedMovies) {
            System.out.println("- "+movie.getTitle() + " - Rating: " + movie.getVoteAverage());
        }

        // Få titlerne på de top-10 mest populære film
        List<Movie> mostPopularMovies = movieDAO.getTop10MostPopularMovies();
        System.out.println(BLUE+"\nTop 10 mest populære film:\n"+RESET);
        for (Movie movie : mostPopularMovies) {
            System.out.println("- "+movie.getTitle() + " - Popularitet: " + movie.getPopularity());
        }
    }

    private static void printMovieDetails(Movie movie) {
        System.out.println(RED + "Title: " + WHITE + movie.getTitle() + RESET);
        if (movie.getDuration() > 0) {
            System.out.println(RED + "Spilletid: " + WHITE + movie.getDuration() + " minutter" + RESET);
        } else {
            System.out.println(RED + "Spilletid: " + WHITE + "Ukendt" + RESET);
        }
        System.out.println(RED + "Udgivelsesdato: " + WHITE + movie.getReleaseDate() + RESET);
        System.out.printf(RED + "Rating på IMDB: " + WHITE + "%.1f%n" + RESET, movie.getVoteAverage());
        System.out.print(RED + "Genrer: " + WHITE);
        Set<Genre> genre = movie.getGenres();
        if (genre != null && !genre.isEmpty()) {
            System.out.println(genre.stream()
                    .map(Genre::getName)
                    .collect(Collectors.joining(", ")) + RESET);
        } else {
            System.out.println("Ingen genre tilknyttet" + RESET);
        }
        if (movie.getDirector().getName() != null) {
            System.out.println(RED + "Instruktør: " + WHITE + movie.getDirector().getName() + RESET);
        } else {
            System.out.println(RED + "Instruktør: " + WHITE + "Ukendt" + RESET);
        }
        System.out.print(RED + "Skuespiller: " + WHITE);
        Set<Actor> actors = movie.getActors();
        if (actors != null && !actors.isEmpty()) {
            System.out.println(actors.stream()
                    .map(Actor::getName)
                    .collect(Collectors.joining(", ")) + RESET);
        } else {
            System.out.println("Ingen skuespillere tilknyttet" + RESET);
        }

        String overview = movie.getOverview();
        if (overview == null || overview.length() < 11) {
            printWrappedText(RED + "Handling : " + WHITE + "ingen handling angivet" + RESET);
        } else {
            printWrappedText(RED + "Handling : " + WHITE + overview + RESET);
        }

        System.out.println("----------------------------------------------------------------------------------------------------------------------");
    }

    // Metode til at udskrive tekst med linjeskift baseret på ønsket bredde som jeg har sat til 160
    private static void printWrappedText(String text) {
        if (text == null || text.length() < 12) {
            System.out.println("Handling: Ingen handling beskrevet");
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer(text, " ");
        StringBuilder line = new StringBuilder();
        // Så længe der er flere ord i teksten så fortsætter vi med at bygge linjen
        // og udskrive den når den er fyldt med maks bredde som jeg har sat til 160
        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            if (line.length() + word.length() + 1 > Main.LINE_WIDTH) {
                System.out.println(line);
                line = new StringBuilder();
            }
            if (!line.isEmpty()) {
                line.append(" ");
            }
            line.append(word);
        }
        // Her udskriver jeg den sidste linje hvis der er noget tekst tilbage
        if (!line.isEmpty()) {
            System.out.println(line);
        }
    }
}
