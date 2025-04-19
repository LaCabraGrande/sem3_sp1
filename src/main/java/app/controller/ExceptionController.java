package app.controller;

import app.exceptions.ApiException;
import app.entities.Message;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionController {
    private final Logger log = LoggerFactory.getLogger(ExceptionController.class);

    public void apiExceptionHandler(ApiException e, Context ctx) {
        log.error("{} {} - {}", ctx.attribute("requestInfo"), ctx.res().getStatus(), e.getMessage());
        ctx.status(e.getStatusCode());
        ctx.json(new Message(e.getStatusCode(), e.getMessage()));
    }

    public void exceptionHandler(Exception e, Context ctx) {
        log.error("{} {} - {}", ctx.attribute("requestInfo"), ctx.res().getStatus(), e.getMessage());
        ctx.status(500);
        ctx.json(new Message(500, e.getMessage()));
    }
}

