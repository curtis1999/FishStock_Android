package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Bishop implements Piece {

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

  // Pin/reveal check data
  private Coordinate pinnerLoc;
  private ArrayList<Coordinate> pinAve = new ArrayList<>();
  private Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve;

  // Critical piece tracking
  private List<Piece> criticallyAttacking = new ArrayList<>();
  private List<Piece> criticallyDefending = new ArrayList<>();
  private List<Integer> criticallyAttackingValues = new ArrayList<>();
  private List<Integer> criticallyDefendingValues = new ArrayList<>();
  private int forkingValue = 0;
  private int overLoadingValue = 0;

  // Diagonal directions: {fileOffset, rankOffset}
  private static final int[][] DIAGONAL_DIRECTIONS = {
      {1, 1},   // towards a8
      {1, -1},  // towards a1
      {-1, 1},  // towards h8
      {-1, -1}  // towards h1
  };

  // Base material value
  private static final double BASE_VALUE = 3.33;

  public Bishop(Coordinate curPos, boolean isWhite) {
    this(curPos, curPos, isWhite);
  }

  public Bishop(Coordinate fromPos, Coordinate curPos, boolean isWhite) {
    this.fromPos = fromPos;
    this.curPos = curPos;
    this.isWhite = isWhite;
    this.stat = isWhite ? Status.WHITE : Status.BLACK;
  }

  @Override
  public String getName() {
    return "Bishop";
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
    return 'B';
  }

  @Override
  public int getValue() {
    return 3;
  }

  /**
   * Generates all possible diagonal moves for the bishop.
   * Also handles x-ray vision for pins and discovered checks.
   */
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<>();

    for (int[] direction : DIAGONAL_DIRECTIONS) {
      generateMovesInDirection(pos, board, direction[0], direction[1], legalMoves);
    }

    if (legalMoves.isEmpty()) {
      legalMoves.add(new Move(false));
    }

    this.possibleMoves = legalMoves;
    return legalMoves;
  }

  /**
   * Generates moves in a single diagonal direction.
   * Handles x-ray vision for pins and discovered checks.
   */
  private void generateMovesInDirection(Coordinate pos, Cell[][] board,
                                        int fileStep, int rankStep,
                                        ArrayList<Move> legalMoves) {
    Coordinate current = new Coordinate(pos.file + fileStep, pos.rank + rankStep);
    boolean xRay = false;
    boolean revealer = false;
    Coordinate pinLoc = new Coordinate(-1, -1);
    Coordinate revealerLoc = new Coordinate(-1, -1);
    Move lastMove = new Move(false);

    while (isValidSquare(current)) {
      Cell cell = board[current.rank][current.file];

      if (!xRay) {
        // First pass - normal move generation
        Move move = new Move(pos, current, "Bishop", false, isWhite);

        if (cell.PieceStatus == Status.EMPTY) {
          legalMoves.add(move);
        } else if (cell.PieceStatus != stat) {
          // Enemy piece - capture
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
          revealerLoc = new Coordinate(current.file, current.rank);
        }
      } else if (revealer) {
        // X-ray through friendly piece - check for discovered checks
        if (cell.PieceStatus == stat) {
          break; // Our own piece blocks further x-ray
        } else if (cell.PieceStatus == Status.EMPTY) {
          // Continue searching
        } else {
          // Enemy piece - check if it's king or queen
          if (cell.piece.getName().equals("King")) {
            lastMove.setReveal(revealerLoc);
          } else if (cell.piece.getName().equals("Queen")) {
            lastMove.setRevealQueen(revealerLoc);
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
   * Generates the avenue (path) between two squares on a diagonal.
   */
  @Override
  public ArrayList<Coordinate> generateAvenue(Coordinate start, Coordinate end) {
    ArrayList<Coordinate> avenue = new ArrayList<>();

    int fileStep = Integer.signum(end.file - start.file);
    int rankStep = Integer.signum(end.rank - start.rank);

    Coordinate current = new Coordinate(start.file + fileStep, start.rank + rankStep);

    while (current.rank != end.rank && current.file != end.file) {
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
    double eval = BASE_VALUE;

    // Penalty for pinned bishops
    if (isPinned) {
      eval *= 0.5;
    }

    if (isPinnedToQueen) {
      eval *= 2.0 / 3.0;
    }

    // Bonus for reveal checkers
    if (isRevealChecker) {
      eval *= 1.5;
    }

    if (isRevealQueenChecker) {
      eval *= 1.25;
    }

    // Mobility bonus
    int numMoves = GameService.filterMoves(possibleMoves).size();
    eval += (numMoves / 12.0) - (4.0 / 14.0);

    return eval;
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

    // Mobility evaluation
    eval += evaluateMobility();

    // Positional evaluation
    eval += evaluatePosition(board);

    // Piece safety
    eval *= evaluateSafety(curCell);

    // Tactical bonuses
    eval += forkingValue;
    eval -= overLoadingValue;

    return eval;
  }

  /**
   * Evaluates bishop mobility.
   */
  private double evaluateMobility() {
    int numMoves = GameService.filterMoves(possibleMoves).size();

    // Bishops can have 0-13 possible moves
    // Central bishops typically have 13 moves, blocked bishops have few
    // Formula gives bonus for mobility
    return (numMoves / 12.0) - (4.0 / 14.0);
  }

  /**
   * Evaluates positional factors for the bishop.
   */
  private double evaluatePosition(Board board) {
    double positionalBonus = 0.0;

    // Bonus for long diagonals (a1-h8, h1-a8 and nearby)
    if (isOnLongDiagonal()) {
      positionalBonus += 0.4;
    }

    // Bonus for fianchetto bishops (protected by pawns on g2/g7/b2/b7)
    if (isFianchettoed(board)) {
      positionalBonus += 0.3;
    }

    // Penalty for bishops blocked by own pawns (bad bishop)
    int blockedDiagonals = countBlockedDiagonals(board);
    positionalBonus -= blockedDiagonals * 0.2;

    // Bonus for bishop pair (if this is evaluated in context with another bishop)
    // This would need to be evaluated at the board level, not piece level

    return positionalBonus;
  }

  /**
   * Checks if bishop is on or near the long diagonals (a1-h8, h1-a8).
   */
  private boolean isOnLongDiagonal() {
    // a1-h8 diagonal: rank + file = 7
    // h1-a8 diagonal: rank - file = 0
    int sum = curPos.rank + curPos.file;
    int diff = Math.abs(curPos.rank - curPos.file);

    return sum == 7 || diff == 0;
  }

  /**
   * Checks if bishop is fianchettoed (on g2, b2, g7, or b7 protected by pawn).
   */
  private boolean isFianchettoed(Board board) {
    if (isWhite) {
      // Check for b2 or g2 positions
      if ((curPos.file == 1 || curPos.file == 6) && curPos.rank == 1) {
        // Check if protected by pawn on b3/g3
        Cell protectorCell = board.board[2][curPos.file];
        return protectorCell.PieceStatus == Status.WHITE &&
            protectorCell.piece.getName().equals("Pawn");
      }
    } else {
      // Check for b7 or g7 positions
      if ((curPos.file == 1 || curPos.file == 6) && curPos.rank == 6) {
        // Check if protected by pawn on b6/g6
        Cell protectorCell = board.board[5][curPos.file];
        return protectorCell.PieceStatus == Status.BLACK &&
            protectorCell.piece.getName().equals("Pawn");
      }
    }
    return false;
  }

  /**
   * Counts how many diagonals are blocked by own pawns.
   */
  private int countBlockedDiagonals(Board board) {
    int blocked = 0;

    for (int[] direction : DIAGONAL_DIRECTIONS) {
      Coordinate adjacent = new Coordinate(
          curPos.file + direction[0],
          curPos.rank + direction[1]
      );

      if (isValidSquare(adjacent)) {
        Cell cell = board.board[adjacent.rank][adjacent.file];
        if (cell.PieceStatus == stat && cell.piece.getName().equals("Pawn")) {
          blocked++;
        }
      }
    }

    return blocked;
  }

  /**
   * Evaluates the safety of the bishop based on attackers and defenders.
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
        return 0.0; // Will likely be captured
      } else {
        return 0.1; // Under pressure but defended
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
        // Might lose bishop for rook (good exchange)
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
    // Only count as overload if not on same diagonal
    if (criticallyDefending.size() > 1 && !areOnSameDiagonal(criticallyDefending)) {
      overLoadingValue = GameService.getSecondHighestValue(criticallyDefending);
    }
  }

  /**
   * Checks if all pieces in the list are on the same diagonal.
   * If they are, the bishop can defend them all without being overloaded.
   */
  private boolean areOnSameDiagonal(List<Piece> pieces) {
    if (pieces.isEmpty()) {
      return false;
    }

    // Check if all pieces are on the same diagonal from the bishop
    for (Piece piece : pieces) {
      Coordinate piecePos = piece.getPos();
      int fileDiff = Math.abs(piecePos.file - curPos.file);
      int rankDiff = Math.abs(piecePos.rank - curPos.rank);

      // Not on diagonal if file and rank differences don't match
      if (fileDiff != rankDiff) {
        return false;
      }
    }

    // Check if all pieces are on the same diagonal line (not opposite diagonals)
    Coordinate firstPos = pieces.get(0).getPos();
    int firstFileDir = Integer.signum(firstPos.file - curPos.file);
    int firstRankDir = Integer.signum(firstPos.rank - curPos.rank);

    for (int i = 1; i < pieces.size(); i++) {
      Coordinate pos = pieces.get(i).getPos();
      int fileDir = Integer.signum(pos.file - curPos.file);
      int rankDir = Integer.signum(pos.rank - curPos.rank);

      if (fileDir != firstFileDir || rankDir != firstRankDir) {
        return false;
      }
    }

    return true;
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

  @Override
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
    Bishop copy = new Bishop(fromPos, curPos, isWhite);
    copy.setPossibleMoves(possibleMoves);
    copy.setProtectors(protectors);
    copy.setAttackers(attackers);
    copy.isRevealChecker = isRevealChecker;
    copy.isPinned = isPinned;
    copy.pinnerLoc = pinnerLoc;
    copy.pinAve = pinAve;
    copy.revealCheckerLoc = revealCheckerLoc;
    copy.revealAve = revealAve;
    copy.isPinnedToQueen = isPinnedToQueen;
    copy.isRevealQueenChecker = isRevealQueenChecker;
    return copy;
  }
}