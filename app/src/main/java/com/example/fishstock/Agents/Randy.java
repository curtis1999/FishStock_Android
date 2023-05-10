package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Move;

import java.util.ArrayList;
import java.util.Random;

public class Randy extends Agent{
  public Randy(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }
  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv) {
    int seed = possibleMoves.size();
    Random random = new Random();
    int randy = random.nextInt(seed);
    Move randomMove = possibleMoves.get(randy);
    while (true) {
      if (randomMove.coverMove || randomMove.protectionMove) {
        randy = random.nextInt(seed);
        randomMove = possibleMoves.get(randy);
        continue;
      }else {
        break;
      }
    }
    return randomMove;
  }
  public String getName() {
    return "Randy";
  }
}
