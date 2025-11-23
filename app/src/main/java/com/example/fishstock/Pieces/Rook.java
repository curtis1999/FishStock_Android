package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Rook implements Piece {

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

  // Rook-specific state
  public boolean hasMoved = false;  // For castling
  public boolean isConnected = false;  // Connected rooks bonus

  // Pin/reveal data
  private ArrayList<Coordinate> pinAve = new ArrayList<>();
  private Coordinate pinnerLoc;
  private Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve = new ArrayList<>();

  // Critical piece tracking
  private ArrayList<Piece> criticallyAttacking = new ArrayList<>();
  private ArrayList<Piece> criticallyDefending = new ArrayList<>();
  private List<Integer> criticallyAttackingValues = new ArrayList<>();
  private List<Integer> criticallyDefendingValues = new ArrayList<>();
  private int forkingValue = 0;
  private int overLoadingValue = 0;

  // Rook moves in 4 directions (ranks and files)
  private static final int[][] ROOK_DIRECTIONS = {
      {1, 0},   // Right
      {-1, 0},  // Left
      {0, 1},   // Up
      {0, -1}   // Down
  };

  // Base material value
  private static final double BASE_VALUE = 5.63;

  public Rook(Coordinate curPos, boolean isWhite) {
    this(curPos, curPos, isWhite);
  }

  public Rook(Coordinate fromPos, Coordinate curPos, boolean isWhite) {
    this.fromPos = fromPos;
    this.curPos = curPos;
    this.isWhite = isWhite;
    this.stat = isWhite ? Status.WHITE : Status.BLACK;
  }

  @Override
  public String getName() {
    return "Rook";
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
    return 'R';
  }

  @Override
  public int getValue() {
    return 5;
  }

  public void moved() {
    this.hasMoved = true;
  }

  public void connectRooks() {
    this.isConnected = true;
  }

  public boolean isConnected() {
    return isConnected;
  }

  /**
   * Generates all possible rook moves (along ranks and files).
   * Also handles x-ray vision for pins and discovered checks.
   */
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<>();

    // Generate moves in all 4 directions
    for (int[] direction : ROOK_DIRECTIONS) {
      generateMovesInDirection(pos, board, direction[0], direction[1], legalMoves);
    }

    if (legalMoves.isEmpty()) {
      legalMoves.add(new Move(false));
    }

    this.possibleMoves = legalMoves;
    return legalMoves;
  }

  /**
   * Generates moves in a single direction (file or rank).
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
        Move move = new Move(pos, current, "Rook", false, isWhite);

        if (cell.PieceStatus == Status.EMPTY) {
          // Empty square
          legalMoves.add(move);
        } else if (cell.PieceStatus != stat) {
          // Enemy piece - capture
          move = new Move(pos, current, "Rook", true, isWhite);
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
            ArrayList<Coordinate> revealAvenue = generateAvenue(pos, current);
            lastMove.setReveal(revealLoc, revealAvenue);
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
   * Generates the avenue (path) between two squares along a rank or file.
   */
  @Override
  public ArrayList<Coordinate> generateAvenue(Coordinate start, Coordinate end) {
    ArrayList<Coordinate> avenue = new ArrayList<>();

    // Same rank (horizontal)
    if (start.rank == end.rank) {
      int fileStep = Integer.signum(end.file - start.file);
      Coordinate current = new Coordinate(start.file + fileStep, start.rank);

      while (current.file != end.file) {
        avenue.add(current);
        current = new Coordinate(current.file + fileStep, current.rank);
      }
    }
    // Same file (vertical)
    else if (start.file == end.file) {
      int rankStep = Integer.signum(end.rank - start.rank);
      Coordinate current = new Coordinate(start.file, start.rank + rankStep);

      while (current.rank != end.rank) {
        avenue.add(current);
        current = new Coordinate(current.file, current.rank + rankStep);
      }
    }

    return avenue;
  }

  /**
   * Simple evaluation for opponent pieces (used in position evaluator).
   */
  @Override
  public double evaluateSimple(Board board) {
    double eval = BASE_VALUE;

    // Penalty for pinned rooks
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

    // Mobility bonus
    int numMoves = GameService.filterMoves(possibleMoves).size();
    eval += (numMoves / 14.0) - (5.0 / 14.0);

    // Doubled rooks bonus
    if (Board.countAlongFile(board.board, "Rook", isWhite, 0, curPos.file, true) == 2) {
      eval += 1.0;
    }

    // Seventh rank bonus
    if ((isWhite && curPos.rank == 6) || (!isWhite && curPos.rank == 1)) {
      eval += 0.5;
    }

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

    // Mobility bonus
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
   * Evaluates rook mobility.
   */
  private double evaluateMobility() {
    int numMoves = GameService.filterMoves(possibleMoves).size();

    // Rooks can have 0-14 possible moves
    // More moves = better positioned rook
    return (numMoves / 14.0) - (5.0 / 14.0);
  }

  /**
   * Evaluates positional factors for the rook.
   */
  private double evaluatePosition(Board board) {
    double positionalEval = 0.0;

    // Doubled rooks (two rooks on same file) - very strong
    int rooksOnFile = Board.countAlongFile(board.board, "Rook", isWhite, 0, curPos.file, true);
    if (rooksOnFile == 2) {
      positionalEval += 1.0;
    }

    // Rook on seventh rank (very powerful)
    if ((isWhite && curPos.rank == 6) || (!isWhite && curPos.rank == 1)) {
      positionalEval += 0.5;

      // Extra bonus if enemy king is on back rank
      King enemyKing = isWhite ?
          (King) board.blackPieces.get(0) :
          (King) board.whitePieces.get(0);

      int enemyKingRank = enemyKing.getPos().rank;
      if ((isWhite && enemyKingRank == 7) || (!isWhite && enemyKingRank == 0)) {
        positionalEval += 0.3;  // Rook on 7th with king on 8th
      }
    }

    // Connected rooks bonus
    if (isConnected) {
      positionalEval += 0.5;
    }

    // Open file bonus (no pawns on the file)
    if (isOnOpenFile(board)) {
      positionalEval += 0.4;
    }

    // Semi-open file bonus (no friendly pawns, but enemy pawns exist)
    else if (isOnSemiOpenFile(board)) {
      positionalEval += 0.25;
    }

    return positionalEval;
  }

  /**
   * Checks if rook is on an open file (no pawns).
   */
  private boolean isOnOpenFile(Board board) {
    int whitePawns = Board.countAlongFile(board.board, "Pawn", true, 0, curPos.file, true);
    int blackPawns = Board.countAlongFile(board.board, "Pawn", false, 0, curPos.file, true);

    return whitePawns == 0 && blackPawns == 0;
  }

  /**
   * Checks if rook is on a semi-open file (no friendly pawns).
   */
  private boolean isOnSemiOpenFile(Board board) {
    int friendlyPawns = Board.countAlongFile(board.board, "Pawn", isWhite, 0, curPos.file, true);
    return friendlyPawns == 0;
  }

  /**
   * Evaluates the safety of the rook based on attackers and defenders.
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

    // Hanging rook - very bad
    if (!attackers.isEmpty() && protectors.isEmpty()) {
      return -1.0;
    }

    // Attacked by pawn - very bad
    if (countByType(attackers, "Pawn") > 0) {
      if (attackers.size() > protectors.size()) {
        return -1.0;  // Will lose rook
      } else {
        return -0.5;  // Under pressure
      }
    }

    // Attacked by minor piece - bad exchange
    int minorAttackers = countByType(attackers, "Knight") + countByType(attackers, "Bishop");
    if (minorAttackers > 0) {
      if (attackers.size() > protectors.size()) {
        return 0.6;  // Will lose rook for minor piece
      } else {
        return 0.8;  // Can hold but under pressure
      }
    }

    // Protected by pawns - excellent
    int pawnProtectors = countByType(protectors, "Pawn");
    if (pawnProtectors >= 2) {
      return 1.25;
    } else if (pawnProtectors == 1) {
      return 1.15;
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
    // Only count as overload if not on same line
    if (criticallyDefending.size() > 1 && !isOnSameLine(criticallyDefending)) {
      overLoadingValue = GameService.getSecondHighestValue(criticallyDefending);
    }
  }

  /**
   * Checks if all pieces are on the same rank or file.
   * Rooks aren't overloaded if defending pieces along the same line.
   */
  private boolean isOnSameLine(List<Piece> pieces) {
    if (pieces.isEmpty()) {
      return false;
    }

    Coordinate firstPos = pieces.get(0).getPos();
    boolean sameRank = true;
    boolean sameFile = true;

    for (Piece piece : pieces) {
      Coordinate pos = piece.getPos();
      if (pos.rank != firstPos.rank) sameRank = false;
      if (pos.file != firstPos.file) sameFile = false;
    }

    return sameRank || sameFile;
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

  public void setRevealCheck(Coordinate checkerLoc) {
    this.isRevealChecker = true;
    this.revealCheckerLoc = checkerLoc;
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

  @Override
  public ArrayList<Move> getPossibleMoves() {
    return possibleMoves;
  }

  @Override
  public void setPossibleMoves(ArrayList<Move> moves) {
    this.possibleMoves = moves;
  }

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
    Rook copy = new Rook(fromPos, curPos, isWhite);
    copy.setPossibleMoves(possibleMoves);
    copy.setProtectors(protectors);
    copy.setAttackers(attackers);
    copy.isRevealChecker = isRevealChecker;
    copy.isPinned = isPinned;
    copy.pinnerLoc = pinnerLoc;
    copy.pinAve = pinAve;
    copy.revealCheckerLoc = revealCheckerLoc;
    copy.revealAve = revealAve;
    copy.hasMoved = hasMoved;
    copy.isConnected = isConnected;
    copy.isPinnedToQueen = isPinnedToQueen;
    copy.isRevealQueenChecker = isRevealQueenChecker;
    return copy;
  }
}