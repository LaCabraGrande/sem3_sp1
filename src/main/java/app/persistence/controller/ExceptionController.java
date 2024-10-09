package app.persistence.controller;

import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.persistence.entities.Message;
import app.persistence.exceptions.ApiException;


public class ExceptionController {
    private final Logger log = LoggerFactory.getLogger(ExceptionController.class);

    public void apiExceptionsHandler(ApiException e, Context ctx) {
        log.error ("{} {}", ctx.res().getStatus(), e.getMessage());
        ctx.status(e.getStatusCode());
        ctx.json(new Message(e.getStatusCode(), e.getMessage()));
    }


}
