package app.exceptions;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class ApiException extends RuntimeException {

    private static final Logger logger = LoggerFactory.getLogger(ApiException.class);

    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        logError(message, null);
    }

    public ApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        logError(message, cause);
    }

    private void logError(String message, Throwable cause) {
        if (cause != null) {
            logger.error("🌐 API-fejl ({}): {}", statusCode, message, cause);
        } else {
            logger.error("🌐 API-fejl ({}): {}", statusCode, message);
        }
    }
}
