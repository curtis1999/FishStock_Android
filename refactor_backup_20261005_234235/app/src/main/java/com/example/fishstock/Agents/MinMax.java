package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.King;
import com.example.fishstock.Pieces.Piece;

import java.util.ArrayList;
import java.util.List;

public class MinMax extends Agent {

  private static final int DEFAULT_DEPTH = 2; // Increased from 2 for better play

  public MinMax(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  @Override
  public Move getMove(Board chessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv)
      throws CloneNotSupportedException {

    if (possibleMoves.isEmpty()) {
      return null;
    }

    double alpha = Double.NEGATIVE_INFINITY;
    double beta = Double.POSITIVE_INFINITY;
    double maxEval = Double.NEGATIVE_INFINITY;
    Move bestMove = possibleMoves.get(0);

    for (Move move : possibleMoves) {
      Board boardCopy = GameService.copyBoard(chessBoard);
      GameService.makeMove(boardCopy, move, isWhite);
      GameService.updateBoardMeta(boardCopy);
      PositionEvaluator.updatePieces(boardCopy);

      double evaluation = min(boardCopy, DEFAULT_DEPTH - 1, alpha, beta);

      if (evaluation > maxEval) {
        maxEval = evaluation;
        bestMove = move;
      }

      alpha = Math.max(alpha, maxEval);

      // Alpha-beta pruning at root
      if (beta <= alpha) {
        break;
      }
    }

    return bestMove;
  }

  /**
   * Maximizing player (our side).
   */
  private double max(Board board, int depth, double alpha, double beta)
      throws CloneNotSupportedException {

    // Terminal condition: depth reached or game over
    if (depth == 0) {
      return PositionEvaluator.evaluate(board, isWhite);
    }

    ArrayList<Move> possibleMoves = GameService.generateMoves(board, isWhite);

    // Handle check situations
    King ourKing = isWhite ?
        (King) board.whitePieces.get(0) :
        (King) board.blackPieces.get(0);

    if (ourKing.isDoubleChecked) {
      possibleMoves = GameService.generateMovesDoubleCheck(board, possibleMoves, isWhite);
    } else if (ourKing.isChecked) {
      possibleMoves = GameService.generateMovesCheck(board, possibleMoves, isWhite);
    }

    // Terminal: no legal moves (checkmate or stalemate)
    if (possibleMoves.isEmpty()) {
      return PositionEvaluator.evaluate(board, isWhite);
    }

    double maxEval = Double.NEGATIVE_INFINITY;

    for (Move move : possibleMoves) {
      // Skip illegal king captures
      if (move.isCapture && move.capturablePiece != null &&
          move.capturablePiece.getName().equals("King")) {
        continue;
      }

      Board newBoard = GameService.copyBoard(board);
      GameService.makeMove(newBoard, move, isWhite);
      GameService.updateBoardMeta(newBoard);
      PositionEvaluator.updatePieces(newBoard);

      double eval = min(newBoard, depth - 1, alpha, beta);
      maxEval = Math.max(maxEval, eval);
      alpha = Math.max(alpha, eval);

      // Beta cutoff
      if (beta <= alpha) {
        break;
      }
    }

    return maxEval;
  }

  /**
   * Minimizing player (opponent).
   */
  private double min(Board board, int depth, double alpha, double beta)
      throws CloneNotSupportedException {

    // Terminal condition: depth reached or game over
    if (depth == 0) {
      return PositionEvaluator.evaluate(board, isWhite);
    }

    ArrayList<Move> possibleMoves = GameService.generateMoves(board, !isWhite);

    // Handle check situations
    King opponentKing = isWhite ?
        (King) board.blackPieces.get(0) :
        (King) board.whitePieces.get(0);

    if (opponentKing.isDoubleChecked) {
      possibleMoves = GameService.generateMovesDoubleCheck(board, possibleMoves, !isWhite);
    } else if (opponentKing.isChecked) {
      possibleMoves = GameService.generateMovesCheck(board, possibleMoves, !isWhite);
    }

    // Terminal: no legal moves (checkmate or stalemate)
    if (possibleMoves.isEmpty()) {
      return PositionEvaluator.evaluate(board, isWhite);
    }

    double minEval = Double.POSITIVE_INFINITY;

    for (Move move : possibleMoves) {
      // Skip illegal king captures
      if (move.isCapture && move.capturablePiece != null &&
          move.capturablePiece.getName().equals("King")) {
        continue;
      }

      Board newBoard = GameService.copyBoard(board);
      GameService.makeMove(newBoard, move, !isWhite);
      GameService.updateBoardMeta(newBoard);
      PositionEvaluator.updatePieces(newBoard);

      double eval = max(newBoard, depth - 1, alpha, beta);
      minEval = Math.min(minEval, eval);
      beta = Math.min(beta, eval);

      // Alpha cutoff
      if (beta <= alpha) {
        break;
      }
    }


    return minEval;
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
    return "MinMax";
  }
}