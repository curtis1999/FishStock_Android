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
      updatePieces(board);
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
  //Iterates and identifies all pieces threatening to win material or needed for defense (removing the piece would result in a loss of material.)
  public static void updatePieces(Board board) {
    for (Piece piece : board.whitePieces) {
      List<Piece> protectors = piece.getProtectors();
      List<Piece> attackers = piece.getAttackers();
      labelCriticalPieces(board, piece, protectors, attackers);
    }
    for (Piece piece : board.blackPieces) {
      List<Piece> protectors = piece.getProtectors();
      List<Piece> attackers = piece.getAttackers();
      labelCriticalPieces(board, piece, protectors, attackers);
    }
  }

  public static void labelCriticalPieces(Board board, Piece piece, List<Piece> protectors, List<Piece> attackers) {
    if (protectors.size() == 0 && attackers.size() > 0) {
      // CASE 1: No defenders, each attack is critical.
      for (Piece attacker : attackers) {
        attacker.addCriticalAttack(piece);
        board.board[attacker.getFromPos().rank][attacker.getFromPos().file].piece.addCriticalAttack(piece);
      }

    }else if (protectors.size() > 0 && attackers.size() == 0) {
      return;
    }
    // CASE 2: One defender and one attacker. Critical defense and potential critical attack.
    else if (protectors.size() == 1 && attackers.size() == 1) {
      protectors.get(0).addCriticalDefenence(piece);
      board.board[protectors.get(0).getFromPos().rank][protectors.get(0).getFromPos().file].piece.addCriticalDefenence(piece);
      if (attackers.get(0).getValue() < piece.getValue()) {
        attackers.get(0).addCriticalAttack(piece);
        board.board[attackers.get(0).getFromPos().rank][attackers.get(0).getFromPos().file].piece.addCriticalAttack(piece);
      }

      // CASE 3: One defender and multiple attackers
    } else if (protectors.size() == 1 && attackers.size() > 1) {
      protectors.get(0).addCriticalDefenence(getLowestPiece(attackers)); //Adds an overloadingvalue
      board.board[protectors.get(0).getFromPos().rank][protectors.get(0).getFromPos().file].piece.addCriticalDefenence(getLowestPiece(attackers));
      for (Piece attacker : attackers) {
        if (attacker.getValue() < piece.getValue()) {
          attacker.addCriticalAttack(piece);
          board.board[attacker.getFromPos().rank][attacker.getFromPos().file].piece.addCriticalAttack(piece);
        }
      }
      //CASE 4: Multiple protectors, 1 attacker:
    } else if (protectors.size() > 1 && attackers.size() == 1) {
      if (attackers.get(0).getValue() < piece.getValue()) {
        attackers.get(0).addCriticalAttack(piece);
        board.board[attackers.get(0).getFromPos().rank][attackers.get(0).getFromPos().file].piece.addCriticalAttack(piece);
      }
      //Many attackers and defenders.
    } else {
      // CASE 5: Multiple defenders and attackers

      //5.1 Get the unique list of defenders.
      List<Piece> copyProtectors = new ArrayList<>();
      List<Piece> copyAttackers = new ArrayList<>();
      for (Piece p : protectors) {
        copyProtectors.add(p);
      }
      for (Piece p : attackers) {
        copyAttackers.add(p);
      }
      for (Piece attacker : attackers) {
        if (attacker.getName().equals("Pawn")) {
          if (countByType((ArrayList<Piece>) copyProtectors, "Pawn") > 0) {
            removeByName(copyAttackers, "Pawn");
            removeByName(copyProtectors, "Pawn");
          }
        } else if (attacker.getName().equals("Knight") || attacker.getName().equals("Bishop")) {
          if (countByType((ArrayList<Piece>) copyProtectors, "Knight") > 0
              || countByType((ArrayList<Piece>) copyProtectors, "Bishop") > 0) {
            removeByName(copyProtectors, "Knight");
            removeByName(copyAttackers, "Knight");
          }
        } else if (attacker.getName().equals("Rook")) {
          if (countByType((ArrayList<Piece>) copyProtectors, "Rook") > 0) {
            removeByName(copyProtectors, "Rook");
            removeByName(copyAttackers, "Rook");
          }
        } else if (attacker.getName().equals("Queen")) {
          if (countByType((ArrayList<Piece>) copyProtectors, "Queen") > 0) {
            removeByName(copyProtectors, "Queen");
            removeByName(copyAttackers, "Queen");
          }
        } else {
          if (countByType((ArrayList<Piece>) copyProtectors, "King") > 0) {
            removeByName(copyProtectors, "King");
            removeByName(copyAttackers, "King");
          }
        }
        if (copyProtectors.size() == 0 || copyAttackers.size() == 0) {
          break;
        }
      }
      //5.1: Balanced tension: Set all protectors to critical.
      if (copyProtectors.size() == 0 && copyAttackers.size() == 0) {
        for (Piece protector : protectors) {
          protector.addCriticalDefenence(piece);
          board.board[protector.getFromPos().rank][protector.getFromPos().file].piece.addCriticalDefenence(piece);
        }
      } else if (copyProtectors.size() == 0) {
        for (Piece attacker : attackers) {
          attacker.addCriticalAttack(piece);
          board.board[attacker.getFromPos().rank][attacker.getFromPos().file].piece.addCriticalAttack(piece);
        }
        //More defenders than attackers, no critical attacks or defenses.
      } else if (copyAttackers.size() == 0) {
        return;
      } else {
        //5.2 if The attacker threatens to win material right away.
        for (Piece attacker : attackers) {
          if (attacker.getValue() < piece.getValue()) {
            attacker.addCriticalAttack(piece);
            board.board[attacker.getFromPos().rank][attacker.getFromPos().file].piece.addCriticalAttack(piece);
          }
        }
        //5.3: A piece threatens to win material after an initial trade.
        //TODO: CONFIRM THIS LOGIC.
        if (getLowestPiece(copyAttackers).getValue() < getLowestPiece(copyProtectors).getValue()
            && attackers.size() > protectors.size()) {
          getLowestPiece(copyAttackers).addCriticalAttack(getLowestPiece(protectors));
          board.board[getLowestPiece(copyAttackers).getFromPos().rank][getLowestPiece(copyAttackers).getFromPos().file].piece.addCriticalAttack(getLowestPiece(protectors));
        }
      }
    }
  }

  public static int countByType(ArrayList<Piece> pieces, String pieceName) {
    int num = 0;
    for (Piece piece : pieces) {
      if (piece.getName().equals(pieceName)) {
        num++;
      }
    }
    return num;
  }
  public static Piece getLowestPiece(List<Piece> pieces) {
    int lowestValue = 999;
    Piece lowestPiece = null;
    for (Piece piece : pieces) {
      int pieceValue = piece.getValue();
      if (pieceValue == 1) {
        return piece;
      }
      if (pieceValue < lowestValue) {
        lowestValue = pieceValue;
        lowestPiece = piece;
      }
    }
    return lowestPiece;
  }

  //NOTE: pieceName of Knight for both Bishops and knights.
  public static boolean removeByName(List<Piece> pieces, String pieceName) {
    for (Piece piece: pieces) {
      if (piece.getName().equals(pieceName) || piece.getName().equals("Bishop") && pieceName.equals("Knight")) {
        pieces.remove(piece);
        return true;
      }
    }
    return false;
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
