package com.bookabase.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class APIApplication {
    /**
     * UserController
     *  - "/create"
     *      - Body is new user
     *  - "/login"
     *  - "/logout"
     *  - "/collection": Gets all the collections
     *  - "/collection/create": Creates a collection
     *     - Body is collection details
     *  - "/collection/delete/:id": Deletes a collection
     *  - "/collection/update/:id": Updates a collection
     *     - Body is update
     *  - "/rates/:book_id/:rating"
     *  - "/read/:book_id"
     *      - Body is start_time, end_time, start_page, end_page
     *  - "/search/:email"
     *  - "/follow/:user_id"
     *  - "/unfollow/:user_id"
     *
     * BookController
     *  - "/search?name="
     *  - "/search?release_date="
     *  - "/search?authors=nth,nth,..."
     *  - "/search?publishers=nth,nth,..."
     *  - "/search?genre=..."
     */
    public static void main(String[] args) {
        SpringApplication.run(APIApplication.class, args);
    }
}
