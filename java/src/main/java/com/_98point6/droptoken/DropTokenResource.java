package com._98point6.droptoken;

import com._98point6.droptoken.db.DB;
import com._98point6.droptoken.model.CreateGameRequest;
import com._98point6.droptoken.model.CreateGameResponse;
import com._98point6.droptoken.model.GameStatusResponse;
import com._98point6.droptoken.model.GetGamesResponse;
import com._98point6.droptoken.model.GetMoveResponse;
import com._98point6.droptoken.model.GetMovesResponse;
import com._98point6.droptoken.model.PostMoveRequest;
import com._98point6.droptoken.model.PostMoveResponse;
import com._98point6.droptoken.resource.GameNotFound;
import com._98point6.droptoken.resource.StatusException;
import com._98point6.droptoken.resource.TurnException;
import com._98point6.droptoken.service.Game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/drop_token")
@Produces(MediaType.APPLICATION_JSON)
public class DropTokenResource {
    private static final Logger logger = LoggerFactory.getLogger(DropTokenResource.class);
    private DB database = new DB();
    private static final String QUIT = "QUIT";
    private static final String MOVE = "MOVE";
    private static final String COMPLETE = "COMPLETE";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    public DropTokenResource() {

    }

    @GET
    public Response getGames() {
        List<Game> games = this.database.getActiveGames();
        GetGamesResponse response = new GetGamesResponse.Builder()
        .games(games.stream().map(Game::getId).collect(Collectors.toList()))
        .build();
        System.out.print("haha0");
        return Response.ok(response).build();
    }

    @POST
    public Response createNewGame(CreateGameRequest request) {
        logger.info("request={}", request);
        System.out.print("haha1");
        //TODO VALIDATE 400
        Game newGame = this.database.createGame(request);
        CreateGameResponse response = new CreateGameResponse.Builder()
                    .gameId(newGame.getId())
                    .build();
        System.out.print(newGame.getId());
        return Response.ok(response).build();
    }

    @Path("/{id}")
    @GET
    public Response getGameStatus(@PathParam("id") String gameId) {
        logger.info("gameId = {}", gameId);
        Game cur = this.database.getGame(gameId);
        if (cur == null) {
            return Response.status(404).build();
        }
        GameStatusResponse response = new GameStatusResponse.Builder()
                .players(cur.getPlayers())
                .state(cur.isComplete() ? COMPLETE : IN_PROGRESS)
                .winner(cur.getWinner())
                .moves(cur.getMoveCount())
                .build();

        return Response.ok(response).build();
        
    }

    @Path("/{id}/{playerId}")
    @POST
    public Response postMove(@PathParam("id")String gameId, @PathParam("playerId") String playerId, PostMoveRequest request) {
        logger.info("gameId={}, playerId={}, move={}", gameId, playerId, request);
        Game cur = this.database.getGame(gameId);
        if (cur == null || !cur.getPlayers().contains(playerId)) {
            return Response.status(404).build();
        }
        // todo: check col row on the boundary 
        GetMoveResponse move = new GetMoveResponse.Builder()
            .player(playerId)
            .type(MOVE)
            .column(request.getColumn())
            .build();
        
        try{
            int moveId = cur.addMove(move);
            PostMoveResponse response = new PostMoveResponse.Builder()
            .moveLink(String.format("%s/moves/%s", gameId, moveId))
            .build();
        } catch (StatusException e) {
            return Response.status(410).build();
        } catch (GameNotFound e) {
            return Response.status(404).build();
        } catch (TurnException e ) {
            return Response.status(409).build();
        }
        return Response.ok(response).build();
    }

    @Path("/{id}/{playerId}")
    @DELETE
    public Response playerQuit(@PathParam("id")String gameId, @PathParam("playerId") String playerId) {
        logger.info("gameId={}, playerId={}", gameId, playerId);
        Game game = this.database.getGame(gameId);
        if (game == null || !game.getPlayers().contains(playerId)) {
            return Response.status(404).build();
        }
        GetMoveResponse quitMove = new GetMoveResponse.Builder()
                .type(QUIT)
                .player(playerId)
                .build();

        try {
            game.addMove(quitMove);
            return Response.status(202).build();
        } catch (StatusException e) {
            return Response.status(410).build();
        } catch (GameNotFound e) {
            return Response.status(404).build();
        }
    }
    // GET /drop_token/{gameId}/moves- Get (sub) list of the moves played.
    @Path("/{id}/moves")
    @GET
    public Response getMoves(@PathParam("id") String gameId, @QueryParam("start") Integer start, @QueryParam("until") Integer until) {
        logger.info("gameId={}, start={}, until={}", gameId, start, until);
        Game game = this.database.getGame(gameId);
        if (game == null) {
            return Response.status(404).build();
        }
        List<GetMoveResponse> moves = game.getMoves();
        start = start == null ? 0 : start;
        until = until == null ? moves.size() : until;

        try {
            GetMovesResponse response = new GetMovesResponse.Builder()
                    .moves(moves.subList(start, until))
                    .build();
            return Response.ok(response).build();
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            return Response.status(400).build();
        }
    }

    // GET /drop_token/{gameId}/moves/{move_number} - Return the move.
    @Path("/{id}/moves/{moveId}")
    @GET
    public Response getMove(@PathParam("id") String gameId, @PathParam("moveId") Integer moveId) {
        logger.info("gameId={}, moveId={}", gameId, moveId);
        if (moveId < 0) {
            return Response.status(400).build();
        }
        Game cur = this.database.getGame(gameId);
        if (cur == null) {
            return Response.status(404).build();
        }
        GetMoveResponse response = cur.getMoves().get(moveId);
        return Response.ok(response).build();
    }

}
