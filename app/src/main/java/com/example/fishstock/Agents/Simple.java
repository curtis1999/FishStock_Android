package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.GameManager;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.*;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Simple extends Agent {

  // Evaluation weights - can be tuned
  private static final double MATERIAL_WEIGHT = 1.0;
  private static final double PAWN_STRUCTURE_WEIGHT = 0.2;
  private static final double KING_SAFETY_WEIGHT = 0.5;
  private static final double CHECK_BONUS = 0.5;
  private static final double DOUBLE_CHECK_BONUS = 1.0;

  // Evaluation scores
  private static final double CHECKMATE_SCORE = 10000.0;
  private static final double DOUBLE_CHECK_MATE_SCORE = 10001.0;
  private static final double MATE_IN_ONE_PENALTY = -9999.0;
  private static final double STALEMATE_SCORE = 0.0;

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
      updatePieces(boardCopy);

      double evaluation = GameManager.isEndGame(boardCopy)
          ? evaluateEndGame(boardCopy)
          : evaluate(boardCopy);

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
    return evaluate(board);
  }

  /**
   * Main evaluation function for board positions.
   * Returns positive scores for positions favorable to the agent.
   */
  public double evaluate(Board board) throws CloneNotSupportedException {
    // Generate legal moves for both sides
    ArrayList<Move> ourMoves = GameService.generateMoves(board, isWhite);
    ArrayList<Move> theirMoves = GameService.generateMoves(board, !isWhite);

    // Check for immediate checkmate/stalemate
    double mateScore = evaluateMateScenarios(board, ourMoves, theirMoves);
    if (Math.abs(mateScore) > 9000) {
      return mateScore;
    }

    // Check if opponent can deliver mate in one
    double mateInOneThreat = checkForMateInOne(board, theirMoves);
    if (mateInOneThreat < -9000) {
      return mateInOneThreat;
    }

    // Standard position evaluation
    double materialBalance = evaluateMaterial(board);
    double pawnStructure = evaluatePawnStructure(board, isWhite)
        - evaluatePawnStructure(board, !isWhite);
    double kingSafety = evaluateKingSafety(board, isWhite)
        - evaluateKingSafety(board, !isWhite);

    double baseEval = MATERIAL_WEIGHT * materialBalance
        + PAWN_STRUCTURE_WEIGHT * pawnStructure
        + KING_SAFETY_WEIGHT * kingSafety;

    // Bonus for checking opponent
    King opponentKing = isWhite
        ? (King) board.blackPieces.get(0)
        : (King) board.whitePieces.get(0);

    if (opponentKing.isDoubleChecked) {
      baseEval += DOUBLE_CHECK_BONUS;
    } else if (opponentKing.isChecked) {
      baseEval += CHECK_BONUS;
    }

    return baseEval;
  }

  /**
   * Evaluates immediate mate scenarios (checkmate or stalemate).
   */
  private double evaluateMateScenarios(Board board, ArrayList<Move> ourMoves,
                                       ArrayList<Move> theirMoves) {
    King opponentKing = isWhite
        ? (King) board.blackPieces.get(0)
        : (King) board.whitePieces.get(0);

    // Check if opponent is in checkmate
    if (opponentKing.isDoubleChecked) {
      theirMoves = GameService.generateMovesDoubleCheck(board, theirMoves, !isWhite);
      if (theirMoves.isEmpty()) {
        return DOUBLE_CHECK_MATE_SCORE;
      }
    } else if (opponentKing.isChecked) {
      theirMoves = GameService.generateMovesCheck(board, theirMoves, !isWhite);
      if (theirMoves.isEmpty()) {
        return CHECKMATE_SCORE;
      }
    } else if (theirMoves.isEmpty()) {
      // Stalemate - no moves and not in check
      return STALEMATE_SCORE;
    }

    return 0.0; // No immediate mate
  }

  /**
   * Checks if the opponent can deliver checkmate in one move.
   * This is the fixed version of the commented-out TODO section.
   */
  private double checkForMateInOne(Board board, ArrayList<Move> theirMoves)
      throws CloneNotSupportedException {

    for (Move opponentMove : theirMoves) {
      Board futureBoard = GameService.copyBoard(board);
      GameService.makeMove(futureBoard, opponentMove, !isWhite);
      GameService.updateBoardMeta(futureBoard);

      King ourKing = isWhite
          ? (King) futureBoard.whitePieces.get(0)
          : (King) futureBoard.blackPieces.get(0);

      // Generate our possible responses
      ArrayList<Move> ourResponses = GameService.generateMoves(futureBoard, isWhite);

      // If opponent's move puts us in check, filter legal moves
      if (ourKing.isDoubleChecked) {
        ourResponses = GameService.generateMovesDoubleCheck(futureBoard, ourResponses, isWhite);
        if (ourResponses.isEmpty()) {
          return MATE_IN_ONE_PENALTY; // We get checkmated
        }
      } else if (ourKing.isChecked) {
        ourResponses = GameService.generateMovesCheck(futureBoard, ourResponses, isWhite);
        if (ourResponses.isEmpty()) {
          return MATE_IN_ONE_PENALTY; // We get checkmated
        }
      } else if (ourResponses.isEmpty()) {
        // Stalemate after opponent's move - not great but not mate
        return STALEMATE_SCORE;
      }
    }

    return 0.0; // No mate-in-one threat
  }

  /**
   * Evaluates material balance on the board.
   */
  private double evaluateMaterial(Board board) {
    double ourMaterialValue = 0.0;
    double theirMaterialValue = 0.0;

    List<Piece> ourPieces = getPiecesFromBoard(board.board, isWhite);
    List<Piece> theirPieces = getPiecesFromBoard(board.board, !isWhite);

    for (Piece piece : ourPieces) {
      ourMaterialValue += piece.evaluate(board);
    }

    for (Piece piece : theirPieces) {
      theirMaterialValue += piece.evaluateSimple(board);
    }

    return ourMaterialValue - theirMaterialValue;
  }

  /**
   * Retrieves all pieces of a given color from the board.
   * King is placed first in the list for consistent access.
   */
  public List<Piece> getPiecesFromBoard(Cell[][] board, boolean isWhite) {
    List<Piece> pieces = new ArrayList<>();

    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        Cell cell = board[rank][file];

        if ((isWhite && cell.PieceStatus == Status.WHITE)
            || (!isWhite && cell.PieceStatus == Status.BLACK)) {

          if (cell.piece.getName().equals("King")) {
            pieces.add(0, cell.piece); // King always first
          } else {
            pieces.add(cell.piece);
          }
        }
      }
    }

    return pieces;
  }

  /**
   * Evaluates king safety for a given color.
   */
  public double evaluateKingSafety(Board board, boolean isWhite) {
    King king = isWhite
        ? (King) board.whitePieces.get(0)
        : (King) board.blackPieces.get(0);

    return king.evaluateSafety(board);
  }

  /**
   * Evaluates pawn structure quality.
   * TODO: Implement detailed pawn structure evaluation:
   * - Doubled pawns (penalty)
   * - Isolated pawns (penalty)
   * - Passed pawns (bonus)
   * - Pawn chains (bonus)
   */
  public double evaluatePawnStructure(Board board, boolean isWhite) {
    // Placeholder - implement pawn structure heuristics
    return 0.0;
  }

  /**
   * Updates all pieces on the board to identify critical attacks and defenses.
   * This marks pieces involved in tactical operations.
   */
  public static void updatePieces(Board board) {
    // Process white pieces
    for (Piece piece : board.whitePieces) {
      List<Piece> protectors = piece.getProtectors();
      List<Piece> attackers = piece.getAttackers();
      labelCriticalPieces(board, piece, protectors, attackers);
    }

    // Process black pieces
    for (Piece piece : board.blackPieces) {
      List<Piece> protectors = piece.getProtectors();
      List<Piece> attackers = piece.getAttackers();
      labelCriticalPieces(board, piece, protectors, attackers);
    }
  }

  /**
   * Labels pieces as critical based on attack/defense dynamics.
   * This helps identify hanging pieces, overloaded defenders, and tactical motifs.
   */
  public static void labelCriticalPieces(Board board, Piece targetPiece,
                                         List<Piece> protectors, List<Piece> attackers) {
    int numProtectors = protectors.size();
    int numAttackers = attackers.size();

    // Case 1: Undefended piece under attack (hanging piece)
    if (numProtectors == 0 && numAttackers > 0) {
      for (Piece attacker : attackers) {
        markCriticalAttack(board, attacker, targetPiece);
      }
      return;
    }

    // Case 2: No attackers - piece is safe
    if (numAttackers == 0) {
      return;
    }

    // Case 3: Single defender, single attacker
    if (numProtectors == 1 && numAttackers == 1) {
      Piece defender = protectors.get(0);
      Piece attacker = attackers.get(0);

      markCriticalDefense(board, defender, targetPiece);

      if (attacker.getValue() < targetPiece.getValue()) {
        markCriticalAttack(board, attacker, targetPiece);
      }
      return;
    }

    // Case 4: Single defender, multiple attackers (overloaded defender)
    if (numProtectors == 1 && numAttackers > 1) {
      Piece defender = protectors.get(0);
      Piece lowestAttacker = getLowestValuePiece(attackers);

      markCriticalDefense(board, defender, lowestAttacker); // Overloaded

      for (Piece attacker : attackers) {
        if (attacker.getValue() < targetPiece.getValue()) {
          markCriticalAttack(board, attacker, targetPiece);
        }
      }
      return;
    }

    // Case 5: Multiple defenders, single attacker
    if (numProtectors > 1 && numAttackers == 1) {
      Piece attacker = attackers.get(0);
      if (attacker.getValue() < targetPiece.getValue()) {
        markCriticalAttack(board, attacker, targetPiece);
      }
      return;
    }

    // Case 6: Multiple defenders and attackers (complex exchange)
    handleComplexExchange(board, targetPiece, protectors, attackers);
  }

  /**
   * Handles complex tactical situations with multiple attackers and defenders.
   */
  private static void handleComplexExchange(Board board, Piece targetPiece,
                                            List<Piece> protectors, List<Piece> attackers) {
    // Simulate the exchange sequence to determine net material
    List<Piece> copyProtectors = new ArrayList<>(protectors);
    List<Piece> copyAttackers = new ArrayList<>(attackers);

    // Remove matching piece values (trades)
    removeMatchingTrades(copyProtectors, copyAttackers);

    // Balanced tension - all defenders are critical
    if (copyProtectors.isEmpty() && copyAttackers.isEmpty()) {
      for (Piece protector : protectors) {
        markCriticalDefense(board, protector, targetPiece);
      }
    }
    // Attackers can win material
    else if (copyProtectors.isEmpty() && !copyAttackers.isEmpty()) {
      for (Piece attacker : attackers) {
        markCriticalAttack(board, attacker, targetPiece);
      }
    }
    // Defenders have advantage - no critical pieces
    else if (!copyProtectors.isEmpty() && copyAttackers.isEmpty()) {
      return;
    }
    // Net advantage for attackers
    else {
      Piece lowestAttacker = getLowestValuePiece(copyAttackers);
      Piece lowestDefender = getLowestValuePiece(copyProtectors);

      // Immediate material win
      for (Piece attacker : attackers) {
        if (attacker.getValue() < targetPiece.getValue()) {
          markCriticalAttack(board, attacker, targetPiece);
        }
      }

      // Material win after trade sequence
      if (lowestAttacker.getValue() < lowestDefender.getValue()
          && attackers.size() > protectors.size()) {
        markCriticalAttack(board, lowestAttacker, lowestDefender);
      }
    }
  }

  /**
   * Removes equal-value pieces from both lists to simulate trades.
   */
  private static void removeMatchingTrades(List<Piece> protectors, List<Piece> attackers) {
    String[] pieceTypes = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King"};

    for (String pieceType : pieceTypes) {
      while (countByType(protectors, pieceType) > 0
          && countByType(attackers, pieceType) > 0) {
        removeByName(protectors, pieceType);
        removeByName(attackers, pieceType);
      }
    }
  }

  /**
   * Helper methods to mark pieces as critical.
   */
  private static void markCriticalAttack(Board board, Piece attacker, Piece target) {
    attacker.addCriticalAttack(target);
    Cell attackerCell = board.board[attacker.getFromPos().rank][attacker.getFromPos().file];
    attackerCell.piece.addCriticalAttack(target);
  }

  private static void markCriticalDefense(Board board, Piece defender, Piece target) {
    defender.addCriticalDefenence(target);
    Cell defenderCell = board.board[defender.getFromPos().rank][defender.getFromPos().file];
    defenderCell.piece.addCriticalDefenence(target);
  }

  /**
   * Finds the piece with the lowest material value.
   */
  public static Piece getLowestValuePiece(List<Piece> pieces) {
    if (pieces.isEmpty()) {
      return null;
    }

    Piece lowestPiece = pieces.get(0);
    int lowestValue = lowestPiece.getValue();

    for (Piece piece : pieces) {
      int value = piece.getValue();
      if (value == 1) { // Pawn is lowest
        return piece;
      }
      if (value < lowestValue) {
        lowestValue = value;
        lowestPiece = piece;
      }
    }

    return lowestPiece;
  }

  /**
   * Removes first piece of given type from list.
   * Note: Treats Knights and Bishops as equivalent (minor pieces).
   */
  public static boolean removeByName(List<Piece> pieces, String pieceName) {
    for (int i = 0; i < pieces.size(); i++) {
      Piece piece = pieces.get(i);
      String name = piece.getName();

      if (name.equals(pieceName)
          || (name.equals("Bishop") && pieceName.equals("Knight"))
          || (name.equals("Knight") && pieceName.equals("Bishop"))) {
        pieces.remove(i);
        return true;
      }
    }
    return false;
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