package app.controller;

import io.javalin.http.Context;

public interface IController<T, D> {
    void read(Context ctx);
    void getAllMovies(Context ctx);
    boolean validatePrimaryKey(D d);
    T validateEntity(Context ctx);

}
