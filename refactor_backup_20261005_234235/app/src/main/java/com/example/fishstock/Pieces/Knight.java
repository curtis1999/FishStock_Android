package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Knight implements Piece {

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
  private boolean isPinnedToQueen = false;
  private boolean isRevealQueenChecker = false;

  // Pin/reveal check data
  private ArrayList<Coordinate> pinAve;
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

  // Knight move offsets (L-shapes)
  private static final int[][] KNIGHT_MOVES = {
      {1, 2}, {2, 1}, {-1, 2}, {2, -1},
      {-2, 1}, {1, -2}, {-2, -1}, {-1, -2}
  };

  // Base material value
  private static final double BASE_VALUE = 3.05;

  public Knight(Coordinate curPos, boolean isWhite) {
    this(curPos, curPos, isWhite);
  }

  public Knight(Coordinate fromPos, Coordinate curPos, boolean isWhite) {
    this.fromPos = fromPos;
    this.curPos = curPos;
    this.isWhite = isWhite;
    this.stat = isWhite ? Status.WHITE : Status.BLACK;
  }

  @Override
  public String getName() {
    return "Knight";
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
    return 'N';
  }

  @Override
  public int getValue() {
    return 3;
  }

  /**
   * Generates all possible moves for the knight.
   * Knights move in an L-shape: 2 squares in one direction, 1 square perpendicular.
   */
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<>();

    for (int[] offset : KNIGHT_MOVES) {
      Coordinate targetPos = new Coordinate(pos.file + offset[0], pos.rank + offset[1]);

      // Check if target is on the board
      if (!isValidSquare(targetPos)) {
        continue;
      }

      Cell targetCell = board[targetPos.rank][targetPos.file];
      Move move = new Move(pos, targetPos, "Knight", false, isWhite);

      if (targetCell.PieceStatus == Status.EMPTY) {
        // Empty square - normal move
        legalMoves.add(move);
      } else if (targetCell.PieceStatus != stat) {
        // Enemy piece - capture
        move.setCapture(targetCell.piece);
        if (targetCell.piece.getName().equals("King")) {
          move.setCheck(move.fromCoord, generateAvenue(move.fromCoord, move.toCoord));
        }
        legalMoves.add(move);
      } else {
        // Friendly piece - protection
        move.setProtectionMove(targetPos);
        legalMoves.add(move);
      }
    }

    if (legalMoves.isEmpty()) {
      legalMoves.add(new Move(false));
    }

    this.possibleMoves = legalMoves;
    return legalMoves;
  }

  /**
   * Checks if a coordinate is within board bounds.
   */
  private boolean isValidSquare(Coordinate coord) {
    return coord.rank >= 0 && coord.rank < 8 && coord.file >= 0 && coord.file < 8;
  }

  /**
   * Simple evaluation for opponent pieces (used in position evaluator).
   */
  @Override
  public double evaluateSimple(Board board) {
    double eval = BASE_VALUE;

    // Penalty for pinned knights
    if (isPinned) {
      eval *= 0.5;
    }

    // Bonus for reveal checkers
    if (isRevealChecker) {
      eval *= 1.5;
    }

    if (isPinnedToQueen) {
      eval *= 2.0 / 3.0;
    }

    if (isRevealQueenChecker) {
      eval *= 1.25;
    }

    // Mobility bonus (knights are stronger in the center)
    int numMoves = GameService.filterMoves(possibleMoves).size();
    eval += (numMoves / 8.0) - (5.0 / 8.0);

    // Outpost bonus
    if (isOutPost(board.board)) {
      eval += 1.0;
    }

    return eval;
  }

  /**
   * Full evaluation including tactical considerations.
   */
  @Override
  public double evaluate(Board board) {
    double eval = BASE_VALUE;
    Cell curCell = board.board[curPos.rank][curPos.file];

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

    // Mobility evaluation
    eval += evaluateMobility();

    // Positional bonuses
    eval += evaluatePosition(board);

    // Piece safety
    eval *= evaluateSafety(curCell);

    // Tactical bonuses
    eval += forkingValue;
    eval -= overLoadingValue;

    return eval;
  }

  /**
   * Evaluates knight mobility (knights like centralization).
   */
  private double evaluateMobility() {
    int numMoves = GameService.filterMoves(possibleMoves).size();

    // Knights can have 2-8 possible moves
    // Center knights typically have 8 moves, rim knights have 2-4
    // Formula: bonus ranges from -0.625 (2 moves) to +0.375 (8 moves)
    return (numMoves / 8.0) - (5.0 / 8.0);
  }

  /**
   * Evaluates positional factors for the knight.
   */
  private double evaluatePosition(Board board) {
    double positionalBonus = 0.0;

    // Outpost bonus (knight on 4th/5th/6th rank protected and can't be driven away by pawns)
    if (isOutPost(board.board)) {
      positionalBonus += 1.0;

      // Extra bonus for advanced outposts (5th/6th rank for white, 3rd/2nd rank for black)
      if ((isWhite && curPos.rank >= 5) || (!isWhite && curPos.rank <= 2)) {
        positionalBonus += 0.5;
      }
    }

    // Penalty for knights on the rim ("A knight on the rim is dim")
    if (isOnRim()) {
      positionalBonus -= 0.3;

      // Extra penalty for corner knights
      if (isInCorner()) {
        positionalBonus -= 0.2;
      }
    }

    // Bonus for centralized knights (d4, d5, e4, e5)
    if (isCentralized()) {
      positionalBonus += 0.3;
    }

    return positionalBonus;
  }

  /**
   * Checks if knight is on an outpost square.
   * An outpost is a square on the 4th-6th rank that:
   * - Cannot be attacked by enemy pawns
   * - Is relatively safe from exchange
   */
  private boolean isOutPost(Cell[][] board) {
    // Must be on central ranks
    if (curPos.rank < 2 || curPos.rank > 5) {
      return false;
    }

    if (isWhite) {
      // Check if black pawns can attack this square
      return Board.countAlongFile(board, "Pawn", false, curPos.rank + 1, curPos.file + 1, true) == 0
          && Board.countAlongFile(board, "Pawn", false, curPos.rank + 1, curPos.file - 1, true) == 0;
    } else {
      return Board.countAlongFile(board, "Pawn", true, curPos.rank - 1, curPos.file + 1, false) == 0
          && Board.countAlongFile(board, "Pawn", true, curPos.rank - 1, curPos.file - 1, false) == 0;
    }
  }

  /**
   * Checks if the knight is on the rim of the board.
   */
  private boolean isOnRim() {
    return curPos.rank == 0 || curPos.rank == 7 || curPos.file == 0 || curPos.file == 7;
  }

  /**
   * Checks if the knight is in a corner.
   */
  private boolean isInCorner() {
    return (curPos.rank == 0 || curPos.rank == 7) && (curPos.file == 0 || curPos.file == 7);
  }

  /**
   * Checks if the knight is centralized (on d4, d5, e4, e5).
   */
  private boolean isCentralized() {
    return (curPos.file == 3 || curPos.file == 4) && (curPos.rank == 3 || curPos.rank == 4);
  }

  /**
   * Evaluates the safety of the knight based on attackers and defenders.
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

    // Hanging piece - very bad
    if (!attackers.isEmpty() && protectors.isEmpty()) {
      return -1.0;
    }

    // Attacked by pawn - dangerous
    int pawnAttackers = countByType(attackers, "Pawn");
    if (pawnAttackers > 0) {
      if (attackers.size() > protectors.size()) {
        return -1.0; // Will be captured
      } else {
        return -0.5; // Under pressure
      }
    }

    // Protected by pawns - very good
    int pawnProtectors = countByType(protectors, "Pawn");
    if (pawnProtectors >= 2) {
      return 1.25;
    } else if (pawnProtectors == 1) {
      return 1.15;
    }

    // Protected by minor pieces
    int minorProtectors = countByType(protectors, "Knight") + countByType(protectors, "Bishop");
    int minorAttackers = countByType(attackers, "Knight") + countByType(attackers, "Bishop");

    if (minorProtectors > minorAttackers) {
      if (attackers.size() > protectors.size()) {
        // Might lose knight for rook (good exchange)
        if (countByType(attackers, "Rook") > 0) {
          return 0.8;
        } else {
          return 1.1;
        }
      } else {
        return 1.15;
      }
    }

    return 1.0; // Neutral
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
    if (criticallyDefending.size() > 1) {
      overLoadingValue = GameService.getSecondHighestValue(criticallyDefending);
    }
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

  @Override
  public boolean isRevealChecker() {
    return isRevealChecker;
  }

  @Override
  public Coordinate getRevealCheckerLoc() {
    return revealCheckerLoc;
  }

  public void unReveal() {
    this.isRevealChecker = false;
    this.revealAve = null;
    this.revealCheckerLoc = new Coordinate(-1, -1);
  }

  @Override
  public void setRevealQueen() {
    this.isRevealQueenChecker = true;
  }

  @Override
  public void setRevealQueenChecker() {
    this.isRevealQueenChecker = true;
  }

  @Override
  public boolean isRevealQueenChecker() {
    return isRevealQueenChecker;
  }

  @Override
  public Coordinate getCheckerLoc() {
    return null; // Knights give direct check only
  }

  @Override
  public ArrayList<Coordinate> generateAvenue(Coordinate c1, Coordinate c2) {
    // Knights jump, so avenue is just the two squares
    ArrayList<Coordinate> ave = new ArrayList<>();
    ave.add(c1);
    ave.add(c2);
    return ave;
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

  @Override
  public void setRevealChecker() {

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
    Knight copy = new Knight(fromPos, curPos, isWhite);
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