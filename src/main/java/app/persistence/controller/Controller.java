package app.persistence.controller;

import io.javalin.http.Context;

public interface Controller {
    public void getHotelById(Context ctx);
    public void createHotel(Context ctx);
    public void updateHotel(Context ctx);
    public void deleteHotel(Context ctx);
    public void getAllHotels(Context ctx);
    public void getRoomsForHotel(Context ctx);
}
