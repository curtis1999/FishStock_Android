package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.King;
import com.example.fishstock.Pieces.Piece;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class MinMax extends Agent{
  public MinMax(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  @Override
  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv) throws CloneNotSupportedException {
    int depth = 2; // set the depth to 4, adjust as needed
    double alpha = -999.0;
    double beta = 999.0;
    double maxEval = -999.0;
    int maxIndex = 0;
    int counter = 0;
    for (int i = 0; i < possibleMoves.size(); i++) {
      Board board = GameService.copyBoard(ChessBoard);
      GameService.makeMove(board, possibleMoves.get(i), isWhite);
      GameService.updateBoardMeta(board);
      double curEval = min(board, depth - 1, alpha, beta);
      if (curEval > maxEval) {
        maxEval = curEval;
        maxIndex = i;
      }
      alpha = Math.max(alpha, maxEval);
      GameService.undoMove(board, possibleMoves.get(i), isWhite);
    }
    return possibleMoves.get(maxIndex);
  }

  private double max(Board board, int depth, double alpha, double beta) throws CloneNotSupportedException {
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
      double eval = min(newBoard, depth - 1, alpha, beta);
      alpha = Math.max(alpha, eval);
      if (beta <= alpha) {
        break; // beta cutoff
      }
    }
    return alpha;
  }

  private double min(Board board, int depth, double alpha, double beta) throws CloneNotSupportedException {
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
      double eval = max(newBoard, depth - 1, alpha, beta);
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
  public double evaluate (Board board) throws CloneNotSupportedException {
    double a = 1;
    double b = 1;
    double c = 1;
    boolean isDoubleCheck = false;
    boolean isCheck = false;
    //Part 1: Check for checkmates or draws:
    ArrayList<Move> ourNextMoves = GameService.generateMoves(board, isWhite);
    ArrayList<Move> theirNextMoves = GameService.generateMoves(board, !isWhite);

    if (isWhite && ((King)board.blackPieces.get(0)).isDoubleChecked) {
      theirNextMoves = GameService.generateMovesDoubleCheck(board, theirNextMoves, false);
      if (theirNextMoves.size() == 0) {
        return 10001;  //Check-Mate
      } else {
        isDoubleCheck = true;
      }
    }
    if (isWhite && ((King)board.blackPieces.get(0)).isChecked) {
      theirNextMoves = GameService.generateMovesCheck(board, theirNextMoves, false);
      if (theirNextMoves.size() == 0) {
        return 10000;  //Check-Mate
      } else {
        isCheck = true;
      }
    }
    if (!isWhite && ((King)board.whitePieces.get(0)).isDoubleChecked) {
      theirNextMoves = GameService.generateMovesDoubleCheck(board, theirNextMoves, true);
      if (theirNextMoves.size() == 0) {
        return 10001;  //Check-Mate
      } else {
        isDoubleCheck = true;
      }
    }
    if (!isWhite && ((King)board.whitePieces.get(0)).isChecked) {
      theirNextMoves = GameService.generateMovesCheck(board, theirNextMoves, true);
      if (theirNextMoves.size() == 0) {
        return 10000;  //Check-Mate
      } else {
        isCheck = true;
      }
    }

    double ourPieceQuality = 0.0;
    List<Piece> ourPieces =  getPiecesFromBoard(board.board, isWhite);
    List<Piece> adversaryPieces = getPiecesFromBoard(board.board, !isWhite);

    for (Piece piece : ourPieces) {
      ourPieceQuality += piece.evaluate(board);
    }
    double theirPieceQuality = 0.0;

    for (Piece piece : adversaryPieces) {
      theirPieceQuality += piece.evaluateSimple(board);
    }
    double ourPawnStructure = evaluatePawnStructure(board, isWhite);
    double theirPawnStructure = evaluatePawnStructure(board, !isWhite);
    double ourKingSafety = evaluateKingSafety(board, isWhite);
    double theirKingSafety = evaluateKingSafety(board, !isWhite);
    return a * (ourPieceQuality - theirPieceQuality) + b * (ourPawnStructure - theirPawnStructure) + c * (ourKingSafety - theirKingSafety);
  }
  public double evaluateKingSafety(Board board, boolean isWhite) {
    return 1.0;
  }

  public double evaluatePawnStructure(Board board, boolean isWhite) {
    return 1.0;
  }
  public List<Piece> getPiecesFromBoard(Cell[][] board, boolean isWhite) {
    List<Piece> Pieces = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      for (int j=0; j<8; j++){
        if ((isWhite && board[i][j].PieceStatus.equals(Status.WHITE))
            || (!isWhite && board[i][j].PieceStatus.equals(Status.BLACK))) {
          if (board[i][j].piece.getName().equals("King")) {
            Pieces.add(0, board[i][j].piece);
          } else {
            Pieces.add(board[i][j].piece);
          }
        }
      }
    }
    return Pieces;
  }
  public static boolean isOutPost(Piece p, Cell[][] board) {
    return false;
  }
  public static boolean isLongDiagonal(Piece p) {
    return false;
  }
  public String getName() {
    return "MIN MAX";
  }
}
