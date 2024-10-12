package app;

import app.persistence.config.ApplicationConfig;
import jakarta.persistence.EntityManagerFactory;
import app.persistence.config.HibernateConfig;

public class Main {
    private static EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory("moviedb");

    public static void main(String[] args) throws Exception {
        ApplicationConfig.startServer(7070, emf);
    }
}
