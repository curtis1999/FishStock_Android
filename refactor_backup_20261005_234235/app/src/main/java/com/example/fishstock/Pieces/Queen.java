package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Queen implements Piece {

  // Position tracking
  private Coordinate fromPos;
  private Coordinate curPos;
  private boolean isWhite;
  private Status stat;

  // Move generation
  private ArrayList<Move> possibleMoves = new ArrayList<>();

  // Tactical state
  private ArrayList<Piece> protectors = new ArrayList<>();
  private ArrayList<Piece> attackers = new ArrayList<>();
  private boolean isPinned = false;
  private boolean isRevealChecker = false;
  private boolean isRevealQueenChecker = false;
  private boolean isPinnedToQueen = false;

  // Pin/reveal data
  private ArrayList<Coordinate> pinAve = new ArrayList<>();
  private Coordinate pinnerLoc;
  private Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve;

  // Critical piece tracking
  private ArrayList<Piece> criticallyAttacking = new ArrayList<>();
  private ArrayList<Piece> criticallyDefending = new ArrayList<>();
  private List<Integer> criticallyAttackingValues = new ArrayList<>();
  private List<Integer> criticallyDefendingValues = new ArrayList<>();
  private int forkingValue = 0;
  private int overLoadingValue = 0;

  // Queen moves in all 8 directions (rook + bishop)
  private static final int[][] QUEEN_DIRECTIONS = {
      {1, 0},   // Right (rook)
      {-1, 0},  // Left (rook)
      {0, 1},   // Up (rook)
      {0, -1},  // Down (rook)
      {1, 1},   // Diagonal up-right (bishop)
      {1, -1},  // Diagonal down-right (bishop)
      {-1, 1},  // Diagonal up-left (bishop)
      {-1, -1}  // Diagonal down-left (bishop)
  };

  // Base material value
  private static final double BASE_VALUE = 9.5;

  public Queen(Coordinate curPos, boolean isWhite) {
    this(curPos, curPos, isWhite);
  }

  public Queen(Coordinate fromPos, Coordinate curPos, boolean isWhite) {
    this.fromPos = fromPos;
    this.curPos = curPos;
    this.isWhite = isWhite;
    this.stat = isWhite ? Status.WHITE : Status.BLACK;
  }

  @Override
  public String getName() {
    return "Queen";
  }

  @Override
  public Coordinate getPos() {
    return curPos;
  }

  @Override
  public Coordinate getFromPos() {
    return fromPos;
  }

  @Override
  public boolean getColor() {
    return isWhite;
  }

  @Override
  public void setPos(Coordinate coord) {
    this.fromPos = curPos;
    this.curPos = coord;
  }

  @Override
  public char getSymbol() {
    return 'Q';
  }

  @Override
  public int getValue() {
    return 9;
  }

  /**
   * Generates all possible queen moves (combination of rook and bishop moves).
   * Also handles x-ray vision for pins and discovered checks.
   */
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<>();

    // Generate moves in all 8 directions
    for (int[] direction : QUEEN_DIRECTIONS) {
      generateMovesInDirection(pos, board, direction[0], direction[1], legalMoves);
    }

    if (legalMoves.isEmpty()) {
      legalMoves.add(new Move(false));
    }

    this.possibleMoves = legalMoves;
    return legalMoves;
  }

  /**
   * Generates moves in a single direction (file/rank/diagonal).
   * Handles x-ray vision for pins and discovered checks.
   */
  private void generateMovesInDirection(Coordinate pos, Cell[][] board,
                                        int fileStep, int rankStep,
                                        ArrayList<Move> legalMoves) {
    Coordinate current = new Coordinate(pos.file + fileStep, pos.rank + rankStep);
    boolean xRay = false;
    boolean revealer = false;
    Coordinate pinLoc = new Coordinate(-1, -1);
    Coordinate revealLoc = new Coordinate(-1, -1);
    Move lastMove = new Move(false);

    while (isValidSquare(current)) {
      Cell cell = board[current.rank][current.file];

      if (!xRay) {
        // First pass - normal move generation
        Move move = new Move(pos, current, "Queen", false, isWhite);

        if (cell.PieceStatus == Status.EMPTY) {
          // Empty square
          legalMoves.add(move);
        } else if (cell.PieceStatus != stat) {
          // Enemy piece - capture
          move = new Move(pos, current, "Queen", true, isWhite);
          move.setCapture(cell.piece);
          if (cell.piece.getName().equals("King")) {
            move.setCheck(move.fromCoord, generateAvenue(move.fromCoord, move.toCoord));
          }
          legalMoves.add(move);
          lastMove = move;
          xRay = true;
          pinLoc = new Coordinate(current.file, current.rank);
        } else {
          // Friendly piece - protection
          move.setProtectionMove(current);
          legalMoves.add(move);
          lastMove = move;
          revealer = true;
          xRay = true;
          revealLoc = new Coordinate(current.file, current.rank);
        }
      } else if (revealer) {
        // X-ray through friendly piece - check for discovered checks
        if (cell.PieceStatus == stat) {
          break; // Another friendly piece blocks x-ray
        } else if (cell.PieceStatus == Status.EMPTY) {
          // Continue searching
        } else {
          // Enemy piece - check if it's king or queen
          if (cell.piece.getName().equals("King")) {
            lastMove.setReveal(revealLoc);
          } else if (cell.piece.getName().equals("Queen")) {
            lastMove.setRevealQueen(revealLoc);
          }
          break;
        }
      } else {
        // X-ray through enemy piece - check for pins
        if (cell.PieceStatus == Status.EMPTY) {
          // Continue searching
        } else if (cell.PieceStatus != stat) {
          // Another enemy piece - check if it's king or queen
          if (cell.piece.getName().equals("King")) {
            ArrayList<Coordinate> pinAvenue = generateAvenue(pos, current);
            lastMove.setPin(pinLoc, pinAvenue);
          } else if (cell.piece.getName().equals("Queen")) {
            lastMove.setPinQueen(pinLoc);
          }
          break;
        } else {
          // Friendly piece blocks x-ray
          break;
        }
      }

      current = new Coordinate(current.file + fileStep, current.rank + rankStep);
    }
  }

  /**
   * Checks if a coordinate is within board bounds.
   */
  private boolean isValidSquare(Coordinate coord) {
    return coord.rank >= 0 && coord.rank < 8 && coord.file >= 0 && coord.file < 8;
  }

  /**
   * Generates the avenue (path) between two squares.
   */
  @Override
  public ArrayList<Coordinate> generateAvenue(Coordinate start, Coordinate end) {
    ArrayList<Coordinate> avenue = new ArrayList<>();

    int fileStep = Integer.signum(end.file - start.file);
    int rankStep = Integer.signum(end.rank - start.rank);

    Coordinate current = new Coordinate(start.file + fileStep, start.rank + rankStep);

    while (current.rank != end.rank || current.file != end.file) {
      avenue.add(current);
      current = new Coordinate(current.file + fileStep, current.rank + rankStep);
    }

    return avenue;
  }

  /**
   * Simple evaluation for opponent pieces (used in position evaluator).
   */
  @Override
  public double evaluateSimple(Board board) {
    return BASE_VALUE;
  }

  /**
   * Full evaluation including tactical and positional considerations.
   */
  @Override
  public double evaluate(Board board) {
    Cell curCell = board.board[curPos.rank][curPos.file];
    double eval = BASE_VALUE;

    // Positional penalties
    if (isPinned) {
      eval *= 0.5;
    }

    if (isPinnedToQueen) {
      eval *= 2.0 / 3.0;
    }

    // Positional bonuses
    if (isRevealChecker) {
      eval *= 1.5;
    }

    if (isRevealQueenChecker) {
      eval *= 1.25;
    }

    // Mobility bonus (queens like mobility)
    eval += evaluateMobility();

    // Positional evaluation
    eval += evaluatePosition();

    // Piece safety
    eval *= evaluateSafety(curCell);

    // Tactical bonuses
    eval += forkingValue;
    eval -= overLoadingValue;

    return eval;
  }

  /**
   * Evaluates queen mobility.
   */
  private double evaluateMobility() {
    int numMoves = GameService.filterMoves(possibleMoves).size();

    // Queens can have 0-27 possible moves
    // More moves = better positioned queen
    return (numMoves / 27.0) - 0.3;
  }

  /**
   * Evaluates positional factors for the queen.
   */
  private double evaluatePosition() {
    double positionalEval = 0.0;

    // Penalty for early queen development
    if ((isWhite && curPos.rank < 2) || (!isWhite && curPos.rank > 5)) {
      // Queen hasn't left back rank area - might be undeveloped
      positionalEval -= 0.3;
    }

    // Penalty for queen on the edge (easier to trap)
    if (isOnEdge()) {
      positionalEval -= 0.2;
    }

    // Bonus for centralized queen (but not too early)
    if (isCentralized()) {
      positionalEval += 0.3;
    }

    return positionalEval;
  }

  /**
   * Checks if queen is on the edge of the board.
   */
  private boolean isOnEdge() {
    return curPos.rank == 0 || curPos.rank == 7 || curPos.file == 0 || curPos.file == 7;
  }

  /**
   * Checks if queen is centralized.
   */
  private boolean isCentralized() {
    return (curPos.file >= 2 && curPos.file <= 5) && (curPos.rank >= 2 && curPos.rank <= 5);
  }

  /**
   * Evaluates the safety of the queen based on attackers and defenders.
   */
  private double evaluateSafety(Cell curCell) {
    ArrayList<Piece> attackers;
    ArrayList<Piece> protectors;

    if (isWhite) {
      attackers = curCell.blackAttackers;
      protectors = curCell.whiteAttackers;
    } else {
      attackers = curCell.whiteAttackers;
      protectors = curCell.blackAttackers;
    }

    // Hanging queen - catastrophic
    if (!attackers.isEmpty() && protectors.isEmpty()) {
      return -1.0;
    }

    // Attacked by pawn - very bad
    if (countByType(attackers, "Pawn") > 0) {
      return 0.0;  // Queen attacked by pawn is terrible
    }

    // Attacked by minor piece - bad
    if (countByType(attackers, "Knight") > 0 || countByType(attackers, "Bishop") > 0) {
      if (attackers.size() > protectors.size()) {
        return -1.0;  // Will lose queen
      } else {
        return -0.5;  // Under pressure
      }
    }

    // Attacked by rook - risky
    if (countByType(attackers, "Rook") > 0) {
      if (attackers.size() > protectors.size()) {
        return 0.0;  // Bad exchange
      } else {
        return 0.15;  // Can trade but risky
      }
    }

    // Protected by pawns - good
    int pawnProtectors = countByType(protectors, "Pawn");
    if (pawnProtectors >= 2) {
      return 1.2;
    } else if (pawnProtectors == 1) {
      return 1.1;
    }

    return 1.0;  // Neutral
  }

  /**
   * Counts pieces of a given type in a list.
   */
  private static int countByType(ArrayList<Piece> pieces, String pieceName) {
    int count = 0;
    for (Piece piece : pieces) {
      if (piece.getName().equals(pieceName)) {
        count++;
      }
    }
    return count;
  }

  // ==================== Tactical State Management ====================

  @Override
  public void addAttacker(Piece piece) {
    attackers.add(piece);
  }

  @Override
  public void addProtector(Piece piece) {
    protectors.add(piece);
  }

  @Override
  public ArrayList<Piece> getProtectors() {
    return protectors;
  }

  @Override
  public ArrayList<Piece> getAttackers() {
    return attackers;
  }

  @Override
  public void addCriticalAttack(Piece piece) {
    criticallyAttacking.add(piece);
    if (criticallyAttacking.size() > 1) {
      forkingValue = GameService.getSecondHighestValue(criticallyAttacking);
    }
  }

  @Override
  public void addCriticalDefenence(Piece piece) {
    criticallyDefending.add(piece);
    // Only count as overload if not on same line or diagonal
    if (criticallyDefending.size() > 1 && !areOnSameLineOrDiagonal(criticallyDefending)) {
      overLoadingValue = GameService.getSecondHighestValue(criticallyDefending);
    }
  }

  /**
   * Checks if all pieces are on the same line (rank/file) or diagonal.
   * Queens aren't overloaded if defending pieces along the same line.
   */
  private boolean areOnSameLineOrDiagonal(List<Piece> pieces) {
    if (pieces.isEmpty()) {
      return false;
    }

    // Check if all on same rank
    Coordinate firstPos = pieces.get(0).getPos();
    boolean sameRank = true;
    boolean sameFile = true;
    boolean sameDiagonal1 = true;
    boolean sameDiagonal2 = true;

    for (Piece piece : pieces) {
      Coordinate pos = piece.getPos();

      if (pos.rank != firstPos.rank) sameRank = false;
      if (pos.file != firstPos.file) sameFile = false;
      if (pos.file + pos.rank != firstPos.file + firstPos.rank) sameDiagonal1 = false;
      if (pos.file - pos.rank != firstPos.file - firstPos.rank) sameDiagonal2 = false;
    }

    return sameRank || sameFile || sameDiagonal1 || sameDiagonal2;
  }

  @Override
  public void addOverloadValue(int value) {
    criticallyDefendingValues.add(value);
  }

  @Override
  public void addForkValue(int value) {
    criticallyAttackingValues.add(value);
  }

  @Override
  public void clearCriticalLabels() {
    criticallyAttacking.clear();
    criticallyDefending.clear();
    criticallyDefendingValues.clear();
    criticallyAttackingValues.clear();
    forkingValue = 0;
    overLoadingValue = 0;
  }

  // ==================== Pin/Check State ====================

  @Override
  public boolean isPinned() {
    return isPinned;
  }

  @Override
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc) {
    this.isPinned = true;
    this.pinAve = pinAve;
    this.pinnerLoc = pinnerLoc;
  }

  public void unPin() {
    this.isPinned = false;
    this.pinAve = null;
  }

  public ArrayList<Coordinate> getPinAvenue() {
    return pinAve;
  }

  @Override
  public Coordinate getPinnerLoc() {
    return pinnerLoc;
  }

  @Override
  public void setQueenPin() {
    this.isPinnedToQueen = true;
  }

  @Override
  public boolean isPinnedToQueen() {
    return isPinnedToQueen;
  }

  @Override
  public void setReveal() {
    this.isRevealChecker = true;
  }

  @Override
  public void setRevealChecker(ArrayList<Coordinate> revealAve, Coordinate checkerLoc) {
    this.isRevealChecker = true;
    this.revealCheckerLoc = checkerLoc;
    this.revealAve = revealAve;
  }

  public void setRevealChecker() {
    this.isRevealChecker = true;
  }

  @Override
  public boolean isRevealChecker() {
    return isRevealChecker;
  }

  @Override
  public Coordinate getRevealCheckerLoc() {
    return revealCheckerLoc;
  }

  @Override
  public void setRevealQueen() {
    this.isRevealQueenChecker = true;
  }

  public void setRevealQueenChecker() {
    this.isRevealQueenChecker = true;
  }

  @Override
  public boolean isRevealQueenChecker() {
    return isRevealQueenChecker;
  }

  @Override
  public Coordinate getCheckerLoc() {
    return null;
  }

  // ==================== Move Management ====================

  @Override
  public ArrayList<Move> getPossibleMoves() {
    return possibleMoves;
  }

  @Override
  public void setPossibleMoves(ArrayList<Move> moves) {
    this.possibleMoves = moves;
  }

  // ==================== State Reset ====================

  @Override
  public void reset() {
    attackers = new ArrayList<>();
    protectors = new ArrayList<>();
    isPinned = false;
    pinAve = null;
    pinnerLoc = new Coordinate(-1, -1);
    isRevealChecker = false;
    revealAve = null;
    revealCheckerLoc = new Coordinate(-1, -1);
    clearCriticalLabels();
  }

  public void setProtectors(ArrayList<Piece> protectors) {
    this.protectors = protectors;
  }

  public void setAttackers(ArrayList<Piece> attackers) {
    this.attackers = attackers;
  }

  @Override
  public Piece copyPiece() {
    Queen copy = new Queen(fromPos, curPos, isWhite);
    copy.setPossibleMoves(possibleMoves);
    copy.setProtectors(protectors);
    copy.setAttackers(attackers);
    copy.isRevealChecker = isRevealChecker;
    copy.revealCheckerLoc = revealCheckerLoc;
    copy.isPinned = isPinned;
    copy.pinnerLoc = pinnerLoc;
    copy.pinAve = pinAve;
    copy.revealAve = revealAve;
    copy.isPinnedToQueen = isPinnedToQueen;
    copy.isRevealQueenChecker = isRevealQueenChecker;
    return copy;
  }
}