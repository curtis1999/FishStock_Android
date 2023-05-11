package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.Piece;

import java.util.ArrayList;
import java.util.List;

public class Simple extends Agent{
  public Simple(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  @Override
  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv) throws CloneNotSupportedException {
    int depth = 3; // set the depth to 4, adjust as needed
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;
    int maxEval = Integer.MIN_VALUE;
    int maxIndex = 0;
    int counter = 0;
    for (int i = 0; i < possibleMoves.size(); i++) {
      Board board = GameService.copyBoard(ChessBoard);
      GameService.makeMove(board, possibleMoves.get(i), isWhite);
      GameService.updateBoardMeta(board);
      int curEval = min(board, depth - 1, alpha, beta);
      if (curEval > maxEval) {
        maxEval = curEval;
        maxIndex = i;
      }
      alpha = Math.max(alpha, maxEval);
    }
    return possibleMoves.get(maxIndex);
  }

  private int max(Board board, int depth, int alpha, int beta) throws CloneNotSupportedException {
    if (depth == 0) {
      return evaluate(board);
    }
    ArrayList<Move> possibleMoves = GameService.generateMoves(board, isWhite);
    for (Move move : possibleMoves) {
      if (move.isCapture && move.capturablePiece.getName().equals("King")) {
        break;
      }
      Board newBoard = GameService.copyBoard(board);
      GameService.makeMove(newBoard, move, isWhite);
      GameService.updateBoardMeta(newBoard);
      int eval = min(newBoard, depth - 1, alpha, beta);
      alpha = Math.max(alpha, eval);
      if (beta <= alpha) {
        break; // beta cutoff
      }
    }
    return alpha;
  }

  private int min(Board board, int depth, int alpha, int beta) throws CloneNotSupportedException {
    if (depth == 0) {
      return evaluate(board);
    }
    ArrayList<Move> possibleMoves = GameService.generateMoves(board, !isWhite);
    for (Move move : possibleMoves) {
      if (move.isCapture && move.capturablePiece.getName().equals("King")) {
        break;
      }
      Board newBoard = GameService.copyBoard(board);
      GameService.makeMove(newBoard, move, !isWhite);
      GameService.updateBoardMeta(newBoard);
      int eval = max(newBoard, depth - 1, alpha, beta);
      beta = Math.min(beta, eval);
      if (beta <= alpha) {
        break; // alpha cutoff
      }
    }
    return beta;
  }


  /**
   * Evaluates a board position.
   * @param board
   * @return
   */
  public int evaluate (Board board) {
    int eval = 0;
    List<Piece> ourPieces;
    List<Piece> adversaryPieces;
    if (isWhite) {
      ourPieces = board.whitePieces;
      adversaryPieces = board.blackPieces;
    } else {
      ourPieces = board.blackPieces;
      adversaryPieces = board.whitePieces;
    }
    //PART 1: Eval the pieces individually.
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
