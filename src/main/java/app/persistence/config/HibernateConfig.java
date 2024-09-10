import app.persistence.entities.Location;
import app.persistence.entities.Package;
import app.persistence.entities.Shipment;
import app.persistence.enums.HibernateConfigState;
import jakarta.persistence.EntityManagerFactory;
import lombok.NoArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class HibernateConfig {

    private static EntityManagerFactory entityManagerFactory;
    private static String dbName = "";

    public static EntityManagerFactory getEntityManagerFactoryConfig(HibernateConfigState state, String db) {
        dbName = db;
        if (entityManagerFactory == null) {
            if (state == HibernateConfigState.TEST) {
                entityManagerFactory = setupHibernateConfigTest();
            } else {
                entityManagerFactory = buildEntityFactoryConfig();
            }
        }
        return entityManagerFactory;
    }

    private static void getAnnotationConfiguration(Configuration configuration) {
        // add annotated classes --- remember to add new entities here
        configuration.addAnnotatedClass(Package.class);
        configuration.addAnnotatedClass(Location.class);
        configuration.addAnnotatedClass(Shipment.class);
    }

    private static EntityManagerFactory buildEntityFactoryConfig() {
        try {
            Configuration configuration = new Configuration();
            Properties props = new Properties();
            String connctionURL = String.format("jdbc:postgresql://localhost:5432/%s?currentSchema=public", dbName);
            props.put("hibernate.connection.url", connctionURL);
            props.put("hibernate.connection.username", "postgres");
            props.put("hibernate.connection.password", "postgres");
//            props.put("hibernate.show_sql", "false"); // show sql in console
//            props.put("hibernate.format_sql", "false"); // format sql in console
//            props.put("hibernate.use_sql_comments", "false"); // show sql comments in console

            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"); // dialect for postgresql
            props.put("hibernate.connection.driver_class", "org.postgresql.Driver"); // driver class for postgresql
            props.put("hibernate.archive.autodetection", "class"); // hibernate scans for annotated classes
            props.put("hibernate.current_session_context_class", "thread"); // hibernate current session context
            props.put("hibernate.hbm2ddl.auto", "create"); // hibernate creates tables based on entities
            return getEntityManagerFactory(configuration, props);
        }
        catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }




    private static EntityManagerFactory setupHibernateConfigTest() {
        try {
            Configuration configuration = new Configuration();
            Properties props = new Properties();
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("hibernate.connection.driver_class", "org.testcontainers.jdbc.ContainerDatabaseDriver");
            props.put("hibernate.connection.url", "jdbc:tc:postgresql:16.2-alpine3.18:///test-db");
            props.put("hibernate.connection.username", "postgres");
            props.put("hibernate.connection.password", "postgres");
            props.put("hibernate.archive.autodetection", "class");
            props.put("hibernate.show_sql", "true");
            props.put("hibernate.hbm2ddl.auto", "create-drop");
            return getEntityManagerFactory(configuration, props);
        }
        catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static EntityManagerFactory getEntityManagerFactory(Configuration configuration, Properties props) {
        configuration.setProperties(props);
        getAnnotationConfiguration(configuration);

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        System.out.println("Hibernate Java Config serviceRegistry created");

        SessionFactory sf = configuration.buildSessionFactory(serviceRegistry);

        return sf.unwrap(EntityManagerFactory.class);
    }
}