package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Move;
import java.util.ArrayList;

public class Human extends Agent{

  public Human(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv){
    return null;
  }
  public String getName() {
    return "Human";
  }
}
