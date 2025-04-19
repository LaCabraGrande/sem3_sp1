package app.exceptions;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class JpaException extends RuntimeException {

    private static final Logger logger = LoggerFactory.getLogger(JpaException.class);

    public JpaException(String message, Exception e) {
        super(message, e);
        writeToLog(message, e);
    }

    private void writeToLog(String message, Exception e) {
        logger.error("❌ JPA-fejl: {}", message, e);  // Logger både besked og stacktrace
    }
}
