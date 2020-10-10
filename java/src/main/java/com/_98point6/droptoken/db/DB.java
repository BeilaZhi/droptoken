package com._98point6.droptoken.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com._98point6.droptoken.model.CreateGameRequest;
import com._98point6.droptoken.service.Game;
//controller: 
public class DB {
    private Map<String, Game> games = new HashMap<>();

    public Game createGame(CreateGameRequest request){
        System.out.print(request);
        Game newGame = new Game(request);
        System.out.print(newGame);
        games.put(newGame.getId(), newGame);
        return newGame;
    }

    public Game getGame(String id) {
        return games.get(id);

    }

    public List<Game> getActiveGames() {
        return games.values().stream().filter(game -> !game.isComplete()).collect(Collectors.toList());
    }
    
}
