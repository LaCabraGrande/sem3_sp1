package app;

import app.persistence.config.ApplicationConfig;
import app.persistence.daos.ActorDAO;
import app.persistence.daos.DirectorDAO;
import app.persistence.daos.GenreDAO;
import app.persistence.daos.MovieDAO;
import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Genre;
import app.persistence.entities.Movie;
import app.persistence.fetcher.FilmFetcher;
import app.persistence.services.FilmService;
import app.persistence.services.MovieService;
import jakarta.persistence.EntityManagerFactory;
import app.persistence.config.HibernateConfig;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    static final int LINE_WIDTH = 160;
    private static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory("moviedb");
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String WHITE = "\u001B[39m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) throws Exception {
        ApplicationConfig.startServer(7070, emf);
        MovieDAO movieDAO = new MovieDAO(emf);
        GenreDAO genreDAO = new GenreDAO(emf);
        ActorDAO actorDAO = new ActorDAO(emf);
        DirectorDAO directorDAO = new DirectorDAO(emf);
        FilmFetcher filmFetcher = new FilmFetcher(genreDAO);
        FilmService filmService = new FilmService(filmFetcher, movieDAO, genreDAO, actorDAO, directorDAO, emf);
        MovieService movieService = new MovieService(movieDAO);
    //    Scanner scanner = new Scanner(System.in);

        // Her spørger jeg om databasen er tom, og hvis den er, henter jeg film fra API'en
//        if (movieDAO.getAllMovies().isEmpty()) {
//            System.out.println("Ingen film i databasen. Henter film fra API...");
//            filmFetcher.populateGenres();
//            filmService.fetchAndSaveMovies();
//        }

    //    showMenu(movieDAO, genreDAO, actorDAO, directorDAO, filmService, scanner);

    //    scanner.close();
    }

    private static void showMenu(MovieDAO movieDAO, GenreDAO genreDAO, ActorDAO actorDAO, DirectorDAO directorDAO, FilmService filmService, Scanner scanner) throws Exception {
        int choice;
        do {
            System.out.println(BLUE + "\n--- Menu ---" + RESET);
            System.out.println("1. Tilføj ny film");
            System.out.println("2. Vis film detaljer");
            System.out.println("3. Hent film fra bestemt årstal");
            System.out.println("4. Hent skuespillere for en film");
            System.out.println("5. Hent instruktør for en film");
            System.out.println("6. Hent film af en skuespiller");
            System.out.println("7. Hent film af en instruktør");
            System.out.println("8. Opdater udgivelsesdato for en film");
            System.out.println("9. Opdater titel for en film");
            System.out.println("10. Søg efter film baseret på titel");
            System.out.println("11. Vis gennemsnitlig rating for alle film");
            System.out.println("12. Vis top 10 laveste ratede film");
            System.out.println("13. Vis top 10 højeste ratede film");
            System.out.println("14. Vis top 10 mest populære film");
            System.out.println("0. Afslut");
            System.out.print("Vælg en handling: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Capture newline

            switch (choice) {
                case 1:
                    tilfoejNyFilm(movieDAO, genreDAO, actorDAO, directorDAO, scanner);
                    break;
                case 2:
                    System.out.print("Indtast filmtitel for at se alle detaljer: ");
                    String title = scanner.nextLine();
                    Movie movieDetails = movieDAO.findByTitle(title);
                    if (movieDetails != null) {
                        printMovieDetails(movieDetails);
                    } else {
                        System.out.println("Film ikke fundet.");
                    }
                    break;
                case 3:
                    System.out.println("Indtast det årstal du ønsker at se film fra: ");
                    int year = scanner.nextInt();
                    List<Movie> moviesYear = movieDAO.getMoviesByReleaseYearAndNationality(year, "da");
                    if(moviesYear != null && !moviesYear.isEmpty()) {
                        System.out.println(BLUE + "\nFilm fra: " + RESET + year+"\n");
                        for(Movie movie : moviesYear) {
                            printMovieDetails(movie);
                        }
                    } else {
                        System.out.println("Ingen film fundet fra " + year);
                    }
                    break;
                case 4:
                    System.out.print("Indtast original filmtitel for at hente skuespillere: ");
                    String movieTitle = scanner.nextLine();
                    List<Actor> actors = filmService.getActorsByMovieTitle(movieTitle);
                    System.out.println(BLUE + "\nSkuespillere i '" + movieTitle + "':\n" + RESET);
                    for (Actor actor : actors) {
                        System.out.println("Actor: " + actor.getName());
                    }
                    break;
                case 5:
                    System.out.print("Indtast original filmtitel for at hente instruktør: ");
                    String directorMovieTitle = scanner.nextLine();
                    Director director = filmService.getDirectorByMovieTitle(directorMovieTitle);
                    System.out.println("\nInstruktøren af '" + directorMovieTitle + "': " + (director != null ? director.getName() : "Ukendt"));
                    break;
                case 6:
                    System.out.print("Indtast skuespillerens navn for at finde deres film: ");
                    String actorName = scanner.nextLine();
                    List<Movie> moviesByActor = filmService.findMoviesByActor(actorName);
                    System.out.println(BLUE + "\nFilm som '" + actorName + "' spiller med i:\n" + RESET);
                    for (Movie movie : moviesByActor) {
                        System.out.println("- " + movie.getOriginalTitle());
                    }
                    break;
                case 7:
                    System.out.print("Indtast navn på instruktør for at finde deres film: ");
                    String directorName = scanner.nextLine();
                    List<Movie> moviesByDirector = filmService.getMoviesByDirector(directorName);
                    System.out.println(BLUE + "\nFilm som '" + directorName + "' har instrueret:\n" + RESET);
                    for (Movie movie : moviesByDirector) {
                        System.out.println("- " + movie.getOriginalTitle());
                    }
                    break;
                case 8:
                    System.out.print("Indtast filmtitel for at opdatere udgivelsesdato: ");
                    String updateTitleDate = scanner.nextLine();
                    System.out.print("Indtast ny udgivelsesdato (YYYY-MM-DD): ");
                    String newReleaseDate = scanner.nextLine();
                    movieDAO.updateMovieReleaseDate(updateTitleDate, newReleaseDate);
                    System.out.println("Udgivelsesdato opdateret for '" + updateTitleDate + "'.");
                    break;
                case 9:
                    System.out.print("Indtast filmtitel for at opdatere titel: ");
                    String updateTitleOld = scanner.nextLine();
                    System.out.print("Indtast ny titel: ");
                    String newTitle = scanner.nextLine();
                    movieDAO.updateMovieTitle(updateTitleOld, newTitle);
                    System.out.println("Titel opdateret for '" + updateTitleOld + "'.");
                    break;
                case 10:
                    System.out.print("Indtast del af titlen for at søge: ");
                    String searchTitle = scanner.nextLine();
                    List<Movie> moviesByTitle = movieDAO.searchMoviesByTitle(searchTitle);
                    System.out.println(BLUE + "\nFilm som indeholder '" + searchTitle + "' i titlen:\n" + RESET);
                    moviesByTitle.forEach(Main::printMovieDetails);
                    break;
                case 110:
                    double averageRating = movieDAO.getTotalAverageRating();
                    System.out.printf("\nGennemsnitlig rating for alle film i Databasen: %.1f%n", averageRating);
                    break;
                case 12:
                    List<Movie> lowestRatedMovies = movieDAO.getTop10LowestRatedMovies();
                    System.out.println(BLUE + "\nTop 10 laveste ratede film:\n" + RESET);
                    for (Movie movie : lowestRatedMovies) {
                        System.out.println("- " + movie.getOriginalTitle() + " - Rating: " + movie.getVoteAverage());
                    }
                    break;
                case 13:
                    List<Movie> highestRatedMovies = movieDAO.getTop10HighestRatedMovies();
                    System.out.println(BLUE + "\nTop 10 højeste ratede film:\n" + RESET);
                    for (Movie movie : highestRatedMovies) {
                        System.out.println("- " + movie.getOriginalTitle() + " - Rating: " + movie.getVoteAverage());
                    }
                    break;
                case 14:
                    List<Movie> mostPopularMovies = movieDAO.getTop10MostPopularMovies();
                    System.out.println(BLUE + "\nTop 10 mest populære film:\n" + RESET);
                    for (Movie movie : mostPopularMovies) {
                        System.out.println("- " +movie.getOriginalTitle() + " - Popularitet: " + movie.getPopularity());
                    }
                    break;
                case 0:
                    System.out.println("Afslutter programmet.");
                    break;
                default:
                    System.out.println("Ugyldigt valg, prøv igen.");
                    break;
            }
        } while (choice != 0);
    }


        private static void visAlleFilm(MovieDAO movieDAO) {
        List<Movie> allMovies = movieDAO.getAllMovies();
        System.out.println("\nAntal film i databasen: " + allMovies.size());
        for (Movie movie : allMovies) {
            printMovieDetails(movie);
        }
    }

    private static void visFilmEfterGenre(MovieDAO movieDAO, String genre) {
        List<Movie> moviesByGenre = movieDAO.getMoviesByGenre(genre);
        System.out.println("\nFilm med genren '" + genre + "':");
        for (Movie movie : moviesByGenre) {
            printMovieDetails(movie);
        }
    }

    private static void visFilmEfterRating(MovieDAO movieDAO, double rating) {
        List<Movie> moviesByRating = movieDAO.getMoviesByRating(rating);
        System.out.println("\nFilm med en rating over " + rating + ":");
        for (Movie movie : moviesByRating) {
            printMovieDetails(movie);
        }
    }

    private static void tilfoejNyFilm(MovieDAO movieDAO, GenreDAO genreDAO, ActorDAO actorDAO, DirectorDAO directorDAO, Scanner scanner) {
        System.out.println("Tilføj ny film:");

        // Titel
        System.out.print("Titel: ");
        String title = scanner.nextLine();

        // IMDB ID
        System.out.print("IMDB ID: ");
        long imdbId = scanner.nextLong();
        scanner.nextLine();  // Capture the newline

        // Udgivelsesdato
        System.out.print("Udgivelsesdato (YYYY-MM-DD): ");
        String releaseDate = scanner.nextLine();

        // Varighed
        System.out.print("Varighed i minutter: ");
        int duration = scanner.nextInt();
        scanner.nextLine();  // Capture the newline

        // Vurdering
        System.out.print("Vurdering (0-10): ");
        double rating = scanner.nextDouble();
        scanner.nextLine();  // Capture the newline

        // Popularitet
        System.out.print("Popularitet: ");
        double popularity = scanner.nextDouble();
        scanner.nextLine();  // Capture the newline

        // Stemmer
        System.out.print("Antal stemmer: ");
        int voteCount = scanner.nextInt();
        scanner.nextLine();  // Capture the newline

        // Voksenfilm (true/false)
        System.out.print("Er det en voksenfilm (true/false): ");
        boolean adult = scanner.nextBoolean();
        scanner.nextLine();  // Capture the newline

        // Originalt sprog
        System.out.print("Originalt sprog: ");
        String originalLanguage = scanner.nextLine();

        // Originaltitel
        System.out.print("Originaltitel: ");
        String originalTitle = scanner.nextLine();

        // Oversigt
        System.out.print("Kort beskrivelse (overview): ");
        String overview = scanner.nextLine();

        // Backdrop path
        System.out.print("Sti til backdrop-billede: ");
        String backdropPath = scanner.nextLine();

        // Poster path
        System.out.print("Sti til poster-billede: ");
        String posterPath = scanner.nextLine();

        // Director input
        System.out.print("Instruktørens navn: ");
        String directorName = scanner.nextLine();
        Director director = directorDAO.findDirectorByName(directorName);
        if (director == null) {
            director = Director.builder()
                    .name(directorName)
                    .build();
            directorDAO.create(director);
        }

        // Genre input
        System.out.print("Genre (indtast genre-id'er adskilt med kommaer): ");
        String genreInput = scanner.nextLine();
        String[] genreIds = genreInput.split(",");
        Set<Genre> genres = new HashSet<>();
        for (String genreId : genreIds) {
            Genre genre = genreDAO.findById(Long.parseLong(genreId.trim()));
            if (genre != null) {
                genres.add(genre);
            }
        }

        // Skuespillere input
        System.out.print("Skuespillere (indtast skuespiller-navne adskilt med kommaer): ");
        String actorInput = scanner.nextLine();
        String[] actorNames = actorInput.split(",");
        Set<Actor> actors = new HashSet<>();
        for (String actorName : actorNames) {
            Actor actor = actorDAO.findByName(actorName.trim());
            if (actor != null) {
                actors.add(actor);
            } else {
                Actor newActor = Actor.builder()
                        .name(actorName.trim())
                        .build();
                actorDAO.create(newActor);
                actors.add(newActor);
            }
        }

        Movie newMovie = Movie.builder()
                .imdbId(imdbId)
                .title(title)
                .releaseDate(releaseDate)
                .duration(duration)
                .voteAverage(rating)
                .voteCount(voteCount)
                .popularity(popularity)
                .originalLanguage(originalLanguage)
                .originalTitle(originalTitle)
                .overview(overview)
                .backdropPath(backdropPath)
                .posterPath(posterPath)
                .adult(adult)
                .director(director)
                .genres(genres)
                .actors(actors)
                .build();

        try {
            movieDAO.createNewMovie(newMovie);
            System.out.println("Film der blev tilføjet til databasen: " + newMovie.getTitle());
        } catch (Exception e) {
            System.err.println("Der opstod en fejl under tilføjelsen af filmen: " + e.getMessage());
        }
    }


    private static void opdaterFilm(MovieDAO movieDAO, Scanner scanner) {
        System.out.print("Indtast filmtitel for at opdatere: ");
        String title = scanner.nextLine();

        System.out.print("Ny titel: ");
        String newTitle = scanner.nextLine();

        movieDAO.updateMovieTitle(title, newTitle);
        System.out.println("Filmen '" + title + "' blev opdateret til '" + newTitle + "'.");
    }

    private static void printMovieDetails(Movie movie) {
        System.out.println(RED + "Title: " + WHITE + movie.getTitle() + RESET);

        // Spilletid
        if (movie.getDuration() > 0) {
            System.out.println(RED + "Spilletid: " + WHITE + movie.getDuration() + " minutter" + RESET);
        } else {
            System.out.println(RED + "Spilletid: " + WHITE + "Ukendt" + RESET);
        }

        // Udgivelsesdato
        System.out.println(RED + "Udgivelsesdato: " + WHITE + movie.getReleaseDate() + RESET);

        // Rating
        System.out.printf(RED + "Rating på IMDB: " + WHITE + "%.1f%n" + RESET, movie.getVoteAverage());

        // Genrer
        System.out.print(RED + "Genrer: " + WHITE);
        Set<Genre> genre = movie.getGenres();
        if (genre != null && !genre.isEmpty()) {
            System.out.println(genre.stream()
                    .map(Genre::getName)
                    .collect(Collectors.joining(", ")) + RESET);
        } else {
            System.out.println("Ingen genre tilknyttet" + RESET);
        }

        // Instruktør
        if (movie.getDirector() != null && movie.getDirector().getName() != null) {
            System.out.println(RED + "Instruktør: " + WHITE + movie.getDirector().getName() + RESET);
        } else {
            System.out.println(RED + "Instruktør: " + WHITE + "Ukendt" + RESET);
        }

        // Skuespillere
        System.out.print(RED + "Skuespiller: " + WHITE);
        Set<Actor> actors = movie.getActors();
        if (actors != null && !actors.isEmpty()) {
            System.out.println(actors.stream()
                    .map(Actor::getName)
                    .collect(Collectors.joining(", ")) + RESET);
        } else {
            System.out.println("Ingen skuespillere tilknyttet" + RESET);
        }

        // Handling
        String overview = movie.getOverview();
        if (overview == null || overview.isEmpty()) {
            printWrappedText(RED + "Handling: " + WHITE + "Ingen handling angivet" + RESET);
        } else {
            printWrappedText(RED + "Handling: " + WHITE + overview + RESET);
        }

        // Afslutning af film detaljer
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
        // Så længe der er flere ord i teksten, fortsætter vi med at bygge linjen
        // og udskriver den, når den er fyldt med maks bredde på 160
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
        // Her udskrives den sidste linje, hvis der er noget tekst tilbage
        if (!line.isEmpty()) {
            System.out.println(line);
        }
    }

}
