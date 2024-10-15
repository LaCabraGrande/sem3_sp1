## Moviedb

This is a Movie Database System where you can get
every information about a movie. 
It is implemented using Javalin and JPA

![Movie](./docs/bates_hotel.jpg)

### How to run

1. Create a database in your local Postgres instance called `moviedb`
2. Run the main method in the Main class to start the server on port 7070. The Database will be filled up with data From
   The Movie Database.
3. See the routes in your browser at `http://localhost:7070/routes`
4. Request the `http://localhost:7070/api/movies` endpoint in your browser to see the list of hotels and rooms.
5. Use the movies.http file to test the routes, GET requests are available.

## Docker commands

```bash
docker-compose up -d
docker compose down --rmi all
docker logs -f watchtower
docker logs watchtower
docker logs moviedb
docker logs sem3_sp1
docker container ls
docker rmi <image_id>
docker stop <container_id>
docker rm <container_id>
```
