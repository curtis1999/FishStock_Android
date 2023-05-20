package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Game;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.*;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Simple extends Agent {

  public Simple(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  @Override
  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv) throws CloneNotSupportedException {
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
  public double evaluate(Board board) throws CloneNotSupportedException {
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
    /* TODO: ADD MATE IN 1 CHECK.
    for (Move move : theirNextMoves) {
      if (move.isCheck) {
        Board copyBoard = GameService.copyBoard(board);
        GameService.makeMove(copyBoard, move, isWhite);
        GameService.updateBoardMeta(copyBoard);
        ArrayList<Move> possibleMoves = GameService.generateMoves(board, isWhite);
        if (GameService.generateMovesCheck(board, possibleMoves, isWhite).size() == 0);
      }
    }

     */

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
  public double evaluateKingSafety(Board board, boolean isWhite) {
    King king;
    if (isWhite) {
      king = (King)board.whitePieces.get(0);
    } else {
      king = (King)board.blackPieces.get(0);
    }
    return king.evaluateSafety(board);
  }
  public double evaluatePawnStructure(Board board, boolean isWhite) {
    return 1.0;
  }


  public String getName() {
    return "Simple";
  }
}