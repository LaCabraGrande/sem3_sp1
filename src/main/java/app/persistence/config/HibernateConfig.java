package app.persistence.config;

import app.persistence.entities.Actor;
import app.persistence.entities.Director;
import app.persistence.entities.Movie;
import app.persistence.entities.Genre;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import java.util.Properties;

/**
 * Purpose: This class is used to configure Hibernate and create an EntityManagerFactory.
 * Author: Thomas Hartmann
 */
public class HibernateConfig {

    private static EntityManagerFactory emf;
    private static EntityManagerFactory emfTest;
    private static boolean isTest = false;

    public static EntityManagerFactory getEntityManagerFactory(String DBName) {
        if (emf == null)
            if (isTest)
                emf = createEMF(true, DBName);
            else
                emf = createEMF(false, DBName);
        return emf;
    }

    public static void setTest(boolean test) {
        isTest = test;
    }

    public static boolean isTest() {
        return isTest;
    }

    public static EntityManagerFactory getEntityManagerFactoryForTest() {
        if (emfTest == null)
            emfTest = createEMF(true,""); // No DB needed for test
        return emfTest;
    }
    // TODO: IMPORTANT: Add Entity classes here for them to be registered with Hibernate
    private static void getAnnotationConfiguration(Configuration configuration) {
        configuration.addAnnotatedClass(Movie.class);
        configuration.addAnnotatedClass(Genre.class);
        configuration.addAnnotatedClass(Actor.class);
        configuration.addAnnotatedClass(Director.class);
    }

    private static EntityManagerFactory createEMF(boolean forTest, String DBName) {
        try {
            Configuration configuration = new Configuration();
            Properties props = new Properties();
            // Set the properties
            setBaseProperties(props);
            if(forTest) {
                props = setTestProperties(props);
            }
            else if(System.getenv("DEPLOYED") != null) {
                setDeployedProperties(props, DBName);
            }
            else {
                props = setDevProperties(props, DBName);
            }
            configuration.setProperties(props);
            getAnnotationConfiguration(configuration);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();
            SessionFactory sf = configuration.buildSessionFactory(serviceRegistry);
            EntityManagerFactory emf = sf.unwrap(EntityManagerFactory.class);
            return emf;
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Properties setBaseProperties(Properties props){
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.current_session_context_class", "thread");
        props.put("hibernate.show_sql", "false"); // Deaktiver SQL visning
        props.put("hibernate.format_sql", "false"); // Deaktiver formattering af SQL
        props.put("hibernate.use_sql_comments", "false"); // Deaktiver SQL kommentarer
        props.put("hibernate.logging.level", "WARN"); // Justerer logniveauet fra DEBUG til WARN
        props.put("org.hibernate.SQL", "WARN"); // Sæt logniveauet til WARN for SQL output
        props.put("org.hibernate.type.descriptor.sql.BasicBinder", "WARN"); // Undgå debug output fra type binder
        return props;
    }

    private static Properties setDeployedProperties(Properties props, String DBName){
        props.setProperty("hibernate.connection.url", System.getenv("CONNECTION_STR") + DBName);
        props.setProperty("hibernate.connection.username", System.getenv("DB_USERNAME"));
        props.setProperty("hibernate.connection.password", System.getenv("DB_PASSWORD"));
        return props;
    }
    private static Properties setDevProperties(Properties props, String DBName){
        props.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/"+ DBName);
        props.put("hibernate.connection.username", "postgres");
        props.put("hibernate.connection.password", "postgres");
        return props;
    }
    private static Properties setTestProperties(Properties props){
        // props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.connection.driver_class", "org.testcontainers.jdbc.ContainerDatabaseDriver");
        props.put("hibernate.connection.url", "jdbc:tc:postgresql:15.3-alpine3.18:///test_db");
        props.put("hibernate.connection.username", "postgres");
        props.put("hibernate.connection.password", "postgres");
        props.put("hibernate.archive.autodetection", "class");
        props.put("hibernate.show_sql", "true");
        props.put("hibernate.hbm2ddl.auto", "create-drop"); // update for production
        return props;
    }
}