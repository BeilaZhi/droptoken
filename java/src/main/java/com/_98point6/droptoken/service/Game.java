package com._98point6.droptoken.service;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com._98point6.droptoken.model.CreateGameRequest;
import com._98point6.droptoken.model.GetMoveResponse;
import com._98point6.droptoken.resource.GameNotFound;
import com._98point6.droptoken.resource.StatusException;
import com._98point6.droptoken.resource.TurnException;
import com._98point6.droptoken.resource.MoveException;


//provide game services and the business logic
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Game {

    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private String id;
    private String winner;
    private String[][] board;
    private List<String> players;
    //not matter how big the board is: winning condition is always 4
    private final Integer winLen = 4;
    //remember the moves
    private int moveCount;
    //variable for 2+ player, but with two players is not nessceary
    private List<String> activePlayer;
    //order will remember whose turn is it
    private int order;
    private ArrayList<GetMoveResponse> moves = new ArrayList<>();


    public Game(CreateGameRequest request) {
        this.id = UUID.randomUUID().toString();
        this.players = request.getPlayers();
        this.board = new String[request.getColumns()][request.getRows()];
        this.order = 0;
        this.activePlayer = new ArrayList<>(request.getPlayers());
        this.moveCount = 0;
    } 

    public Integer addMove(GetMoveResponse move) {
        //if complete 410 
        if (isComplete()) {
            throw new StatusException();
        }

        // Get the info from the move
        String player = move.getPlayer();
        String type = move.getType();
        Integer column = move.getColumn().orElse(4);

        // Validate if player is in the Game
        if(!player.contains(player)) {
            throw new GameNotFound();
        }

        // Check if the player wants to quit
        if ("QUIT".equals(type)) {
            activePlayer.remove(player);
            order = order % activePlayer.size();
            moves.add(move);
            return -1;
        }

        //check if this is his turn
        String nextPlayer = activePlayer.get(order);
        if (!player.equals(nextPlayer)) {
            throw new TurnException();
        }

        //check available slot
        int row = availableRow(column);
        board[column][row] = player;
        checkForWin(column, row);
        order++;
        order = order % activePlayer.size();
        moves.add(move);
        return moveCount++;
    }
    //helper
    public int getMoveCount() {
        return moveCount;
    }
    //helper
    public List<String> getPlayers() {
        return players;
    }
    //helper
    public String getWinner() {
        //incase there is more player
        if (activePlayer.size() == 1) {
            return activePlayer.get(0);
        }
        return winner;
    }
    //helper
    public ArrayList<GetMoveResponse> getMoves(){
        return moves;
    }
    //private helper
    private int availableRow(int col) {
        try {
            String[] row = board[col];
            for (int i = 0; i< row.length; i++) {
                if (row[i] == null) {
                    return i;
                }
            }
        } catch (IndexOutOfBoundsException e ) {
            throw new MoveException("column is invalid");
        }

        throw new MoveException("row not available");
    }
    //helper
    public String getId() {
        System.out.print("haha4" + id);
        return this.id;
    }
    //helper
    public boolean isComplete() {
        return winner!=null || activePlayer.size() == 1;
    }

    private void checkForWin(int column, int row) {
        String player = this.board[column][row];
        if (checkUD(column, row) || checkLR(column, row) || checkPosDiag(column, row) || checkNegDiag(column, row)) {
            winner = player;
        }
    }
    // check up and down
    private boolean checkUD(int column, int row) {
        return 1 + checkDirection(column, row, 0, 1) + checkDirection(column, row, 0, -1) >= winLen;

    }
    //check left and right
    private boolean checkLR(int column, int row) {
        return 1 + checkDirection(column, row, 1, 0) + checkDirection(column, row, -1, 0) >= winLen;
        
    }
    //check from the diag
    private boolean checkPosDiag(int column, int row) {
        return 1 + checkDirection(column, row, 1, 1) + checkDirection(column, row, -1, -1) >= winLen;
    }
    // check neg diag
    private boolean checkNegDiag(int column, int row) {
        return 1 + checkDirection(column, row, -1, -1) + checkDirection(column, row, 1, -1) >= winLen;
    }

    private int checkDirection (int colStart, int rowStart, int colDirection, int rowDirection) {
        String player = this.board[colStart][rowStart];
        int nextColumn = colStart;
        int nextRow = rowStart;

        int depth = 0;
        while (depth < winLen - 1) {
            nextColumn += colDirection;
            nextRow += rowDirection;
            try {
                if (player.equals(this.board[nextColumn][nextRow])) {
                    depth++;
                    continue;
                }
                break;
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
        return depth;
    }
}