/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.db.jdbc;

import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;

import io.helidon.common.OptionalHelper;
import io.helidon.common.http.Http;
import io.helidon.messagingclient.DbClient;
import io.helidon.messagingclient.DbRow;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * Example service using a database.
 */
public class PokemonService implements Service {

    /**
     * Local logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(PokemonService.class.getName());

    private final DbClient dbClient;

    PokemonService(DbClient dbClient) {
        this.dbClient = dbClient;

        // TODO dirty hack to prepare database for our POC
        // MySQL init
        dbClient.execute(handle -> handle.namedDml("create-table"))
                .thenAccept(System.out::println)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to create table, maybe it already exists?", throwable);
                    return null;
                });
    }

    /**
     * A service registers itself by updating the routine rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::listPokemons)
                // create new
                .post("/", Handler.create(Pokemon.class, this::insertPokemon))
                .post("/{name}/type/{type}", this::insertPokemonSimple)
                // delete all
                .delete("/", this::deleteAllPokemons)
                // get one
                .get("/{name}", this::getPokemon)
                // delete one
                .delete("/{name}", this::deletePokemon)
                // update one (TODO this is intentionally wrong - should use JSON request, just to make it simple we use path)
                .put("/{name}/type/{type}", this::updatePokemonType);
    }

    /**
     * Insert new pokemon with specified name.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void insertPokemon(ServerRequest request, ServerResponse response, Pokemon pokemon) {
        dbClient.execute(exec -> exec
                .createNamedInsert("insert2")
                .namedParam(pokemon)
                .execute())
                .thenAccept(count -> response.send("Inserted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    /**
     * Insert new pokemon with specified name.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void insertPokemonSimple(ServerRequest request, ServerResponse response) {
        // Test Pokemon POJO mapper
        Pokemon pokemon = new Pokemon(request.path().param("name"), request.path().param("type"));
        LOGGER.log(Level.INFO,
                   String.format("Running insertPokemonSimple for name=%s type=%s", pokemon.getName(), pokemon.getType()));
        dbClient.execute(exec -> exec
                .createNamedInsert("insert2")
                .namedParam(pokemon)
                .execute())
                .thenAccept(count -> response.send("Inserted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    /**
     * Get a single pokemon by name.
     *
     * @param request  server request
     * @param response server response
     */
    private void getPokemon(ServerRequest request, ServerResponse response) {
        dbClient.execute(exec -> exec.namedGet("select-one", request.path().param("name")))
                .thenAccept(maybeRow -> OptionalHelper.from(maybeRow)
                        .ifPresentOrElse(row -> sendRow(row, response),
                                         () -> sendNotFound(response, "Pokemon "
                                                 + request.path().param("name")
                                                 + " not found")))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    /**
     * Return JsonArray with all stored pokemons or pokemons with matching attributes.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void listPokemons(ServerRequest request, ServerResponse response) {
        //db.listenForMessages(exec -> exec.query("select-all", "SELECT * FROM TABLE"));
        dbClient.execute(exec -> exec.namedQuery("select-all"))
                .consume(response::send)
                .exceptionally(throwable -> sendError(throwable, response));
    }

    /**
     * Delete pokemon with specified name (key).
     *
     * @param request  the server request
     * @param response the server response
     */
    private void updatePokemonType(ServerRequest request, ServerResponse response) {
        final String name = request.path().param("name");
        final String type = request.path().param("type");
        LOGGER.log(Level.INFO, "Running updatePokemonType for {0}", name);
        dbClient.execute(exec -> exec
                .createNamedUpdate("update")
                .addParam("name", name)
                .addParam("type", type)
                .execute())
                .thenAccept(count -> response.send("Updated: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    /**
     * Delete all pokemons.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void deleteAllPokemons(ServerRequest request, ServerResponse response) {
        LOGGER.info("Running deleteAllPokemons");
        dbClient.execute(exec -> exec
                // this is to show how ad-hoc statements can be executed (and their naming in Tracing and Metrics)
                .createDelete("DELETE FROM pokemons")
                .execute())
                .thenAccept(count -> response.send("Deleted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    /**
     * Delete pokemon with specified name (key).
     *
     * @param request  the server request
     * @param response the server response
     */
    private void deletePokemon(ServerRequest request, ServerResponse response) {
        final String name = request.path().param("name");
        LOGGER.log(Level.INFO, "Running deletePokemon for {0}", name);
        dbClient.execute(exec -> exec.namedDelete("delete", name))
                .thenAccept(count -> response.send("Deleted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    private void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
    }

    private void sendRow(DbRow row, ServerResponse response) {
        response.send(row.as(JsonObject.class));
    }

    private Void sendError(Throwable throwable, ServerResponse response) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        response.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        LOGGER.log(Level.WARNING, "Failed to process request", throwable);
        return null;
    }

}
