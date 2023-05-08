package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Move;
import java.util.ArrayList;

public abstract class Agent {
  public AgentType type;
  public boolean isWhite;
  public abstract Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv);
  public Agent(AgentType type, boolean isWhite) {
    this.type = type;
    this.isWhite = isWhite;
  }
  public String getName() {
    return "Agent";
  }
}
