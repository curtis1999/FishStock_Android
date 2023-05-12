package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
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
  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv) {
    double maxEval = -999;
    int maxIndex = 0;
    int counter = 0;
    for (Move move : possibleMoves) {
      Board board = GameService.copyBoard(ChessBoard);
      GameService.makeMove(board, move, isWhite);
      GameService.updateBoardMeta(board);
      double curEval = evaluate(board);
      if (curEval > maxEval) {
        maxEval = curEval;
        maxIndex = counter;
      }
      counter++;
    }
    return possibleMoves.get(maxIndex);
  }

  /**
   * Evaluates a board position.
   *
   * @param board
   * @return
   */
  public double evaluate(Board board) {
    double eval = 0;
    List<Piece> ourPieces;
    List<Piece> adversaryPieces;
    if (isWhite) {
      ourPieces = board.whitePieces;
      adversaryPieces = board.blackPieces;
    } else {
      ourPieces = board.blackPieces;
      adversaryPieces = board.whitePieces;
    }
    for (Piece piece : ourPieces) {
      eval += piece.evaluate(board);
    }
    for (Piece piece : adversaryPieces) {
      eval -= piece.evaluate(board);
    }
    return eval;
  }

  public static boolean isOutPost(Piece p, Cell[][] board) {
    return false;
  }

  public static boolean isLongDiagonal(Piece p) {
    return false;
  }

  public String getName() {
    return "Simple";
  }
}