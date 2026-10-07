package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.GameManager;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.Piece;

import java.util.ArrayList;
import java.util.List;

public class Simple extends Agent {

  public Simple(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  @Override
  public Move getMove(Board chessBoard, ArrayList<Move> possibleMoves,
                      ArrayList<Move> possibleMovesAdv) throws CloneNotSupportedException {
    if (possibleMoves.isEmpty()) {
      return null;
    }

    double maxEval = Double.NEGATIVE_INFINITY;
    Move bestMove = possibleMoves.get(0);

    for (Move move : possibleMoves) {
      Board boardCopy = GameService.copyBoard(chessBoard);
      GameService.makeMove(boardCopy, move, isWhite);
      GameService.updateBoardMeta(boardCopy);
      PositionEvaluator.updatePieces(boardCopy);

      double evaluation = GameManager.isEndGame(boardCopy)
          ? evaluateEndGame(boardCopy)
          : PositionEvaluator.evaluate(boardCopy, isWhite);

      if (evaluation > maxEval) {
        maxEval = evaluation;
        bestMove = move;
      }
    }

    return bestMove;
  }

  public double evaluateEndGame(Board board) throws CloneNotSupportedException {
    // TODO: Implement endgame-specific evaluation
    // Could include: king activity, pawn advancement, piece coordination
    return PositionEvaluator.evaluate(board, isWhite);
  }
  /**
   * Counts pieces of a specific type in the list.
   */
  public static int countByType(List<Piece> pieces, String pieceName) {
    int count = 0;
    for (Piece piece : pieces) {
      if (piece.getName().equals(pieceName)) {
        count++;
      }
    }
    return count;
  }
  @Override
  public String getName() {
    return "Simple";
  }
}