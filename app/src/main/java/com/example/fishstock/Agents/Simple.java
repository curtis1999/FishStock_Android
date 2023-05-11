package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;

import java.util.ArrayList;

public class Simple extends Agent{

  public Simple(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  @Override
  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv) {
    Board board = GameService.copyBoard(ChessBoard);
    return null;
  }
  public String getName() {
    return "Simple";
  }
}
