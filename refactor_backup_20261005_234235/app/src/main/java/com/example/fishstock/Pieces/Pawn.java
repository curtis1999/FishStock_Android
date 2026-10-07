package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Pawn implements Piece {

  // Position and state
  private Coordinate fromPos;
  private Coordinate curPos;
  private boolean isWhite;
  private Status stat;
  private boolean firstMove;
  public boolean enPassantable;

  // Move generation
  private ArrayList<Move> possibleMoves = new ArrayList<>();

  // Tactical state
  private ArrayList<Piece> protectors = new ArrayList<>();
  private ArrayList<Piece> attackers = new ArrayList<>();
  private boolean isPinned = false;
  private boolean isRevealChecker = false;
  private boolean isPinnedToQueen = false;
  private boolean isRevealQueenChecker = false;

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

  // Base value
  private static final double BASE_VALUE = 1.0;

  public Pawn(Coordinate pos, boolean isWhite) {
    this(pos, pos, isWhite, true);
  }

  public Pawn(Coordinate fromPos, Coordinate curPos, boolean isWhite, boolean firstMove) {
    this.fromPos = fromPos;
    this.curPos = curPos;
    this.isWhite = isWhite;
    this.firstMove = firstMove;
    this.stat = isWhite ? Status.WHITE : Status.BLACK;
  }

  @Override
  public String getName() {
    return "Pawn";
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
    return 'P';
  }

  @Override
  public int getValue() {
    return 1;
  }

  public void growUp() {
    this.firstMove = false;
  }

  public void unGrow() {
    this.firstMove = true;
  }

  public void setEnPassantable() {
    this.enPassantable = true;
  }

  public void unEnPassantable() {
    this.enPassantable = false;
  }

  /**
   * Generates all legal pawn moves including captures, pushes, en passant, and promotions.
   */
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<>();

    int direction = isWhite ? 1 : -1;
    int promotionRank = isWhite ? 7 : 0;
    int enPassantRank = isWhite ? 4 : 3;
    Status enemyStat = isWhite ? Status.BLACK : Status.WHITE;

    // Forward moves
    generateForwardMoves(pos, board, legalMoves, direction, promotionRank);

    // Capture moves (left and right diagonals)
    generateCaptureMoves(pos, board, legalMoves, direction, promotionRank, enemyStat, enPassantRank);

    this.possibleMoves = legalMoves;
    return legalMoves;
  }

  /**
   * Generates forward pawn pushes (1 or 2 squares).
   */
  private void generateForwardMoves(Coordinate pos, Cell[][] board, ArrayList<Move> legalMoves,
                                    int direction, int promotionRank) {
    Coordinate oneSquareAhead = new Coordinate(pos.file, pos.rank + direction);

    // Check bounds
    if (oneSquareAhead.rank < 0 || oneSquareAhead.rank > 7) {
      return;
    }

    // One square forward
    if (board[oneSquareAhead.rank][oneSquareAhead.file].PieceStatus == Status.EMPTY) {
      Move move = new Move(pos, oneSquareAhead, "Pawn", false, isWhite);

      // Check for promotion
      if (oneSquareAhead.rank == promotionRank) {
        move.setPromotion();
      }

      legalMoves.add(move);

      // Two squares forward (only if first move)
      if (firstMove) {
        Coordinate twoSquaresAhead = new Coordinate(pos.file, pos.rank + (2 * direction));
        if (board[twoSquaresAhead.rank][twoSquaresAhead.file].PieceStatus == Status.EMPTY) {
          legalMoves.add(new Move(pos, twoSquaresAhead, "Pawn", false, isWhite));
        }
      }
    }
  }

  /**
   * Generates capture moves (diagonal attacks).
   */
  private void generateCaptureMoves(Coordinate pos, Cell[][] board, ArrayList<Move> legalMoves,
                                    int direction, int promotionRank, Status enemyStat, int enPassantRank) {
    int[] fileOffsets = {-1, 1};  // Left and right

    for (int fileOffset : fileOffsets) {
      Coordinate captureSquare = new Coordinate(pos.file + fileOffset, pos.rank + direction);

      // Check bounds
      if (captureSquare.file < 0 || captureSquare.file > 7 ||
          captureSquare.rank < 0 || captureSquare.rank > 7) {
        continue;
      }

      Cell targetCell = board[captureSquare.rank][captureSquare.file];

      if (targetCell.PieceStatus == enemyStat) {
        // Regular capture
        Move move = new Move(pos, captureSquare, "Pawn", true, isWhite);
        move.setCapture(targetCell.piece);

        if (targetCell.piece.getName().equals("King")) {
          move.setCheck(move.fromCoord, generateAvenue(move.fromCoord, move.toCoord));
        }

        if (captureSquare.rank == promotionRank) {
          move.setPromotion();
        }

        legalMoves.add(move);
      } else if (targetCell.PieceStatus == stat) {
        // Protection move
        Move move = new Move(pos, captureSquare, "Pawn", false, isWhite);
        move.setProtectionMove(captureSquare);
        legalMoves.add(move);
      } else {
        // Empty square - could be en passant or just a cover move
        if (pos.rank == enPassantRank) {
          // Check for en passant
          Coordinate adjacentSquare = new Coordinate(pos.file + fileOffset, pos.rank);
          if (board[adjacentSquare.rank][adjacentSquare.file].PieceStatus == enemyStat &&
              board[adjacentSquare.rank][adjacentSquare.file].piece.getName().equals("Pawn") &&
              ((Pawn) board[adjacentSquare.rank][adjacentSquare.file].piece).enPassantable) {

            Move move = new Move(pos, captureSquare, "Pawn", true, isWhite);
            move.setCapture(board[adjacentSquare.rank][adjacentSquare.file].piece);
            move.setEnPassant();
            legalMoves.add(move);
          } else {
            // Just a cover move
            Move move = new Move(pos, captureSquare, "Pawn", false, isWhite);
            move.setCoverMove();
            legalMoves.add(move);
          }
        } else {
          // Cover move (controls the square)
          Move move = new Move(pos, captureSquare, "Pawn", false, isWhite);
          move.setCoverMove();
          legalMoves.add(move);
        }
      }
    }
  }

  @Override
  public double evaluateSimple(Board board) {
    return BASE_VALUE;
  }

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

    // Pawn structure evaluation
    eval += evaluatePawnStructure(board);

    // Central pawns are stronger
    if (Cell.isCentralSquare(curPos)) {
      eval += 0.25;
    }

    // Piece safety
    eval *= evaluateSafety(curCell);

    // Tactical bonuses/penalties
    eval += forkingValue;
    eval -= overLoadingValue;

    return eval;
  }

  /**
   * Evaluates pawn structure factors.
   */
  private double evaluatePawnStructure(Board board) {
    double structureEval = 0.0;

    // Penalty for doubled pawns
    int pawnsOnFile = Board.countAlongFile(board.board, "Pawn", isWhite, 0, curPos.file, true);
    if (pawnsOnFile > 1) {
      structureEval -= 0.2 * (pawnsOnFile - 1);
    }

    // Bonus for passed pawns
    if (isPassed(board.board)) {
      structureEval += 0.75;  // Passed pawns are very valuable

      // Extra bonus for advanced passed pawns
      int distanceToPromotion = isWhite ? (7 - curPos.rank) : curPos.rank;
      structureEval += (7 - distanceToPromotion) * 0.15;  // Closer = better
    }

    // Bonus for connected pawns (pawns on adjacent files)
    if (hasConnectedPawn(board)) {
      structureEval += 0.2;
    }

    // Penalty for isolated pawns (no friendly pawns on adjacent files)
    if (isIsolated(board)) {
      structureEval -= 0.25;
    }

    // Penalty for backward pawns
    if (isBackward(board)) {
      structureEval -= 0.15;
    }

    return structureEval;
  }

  /**
   * Checks if the pawn is passed (no enemy pawns can stop it).
   */
  private boolean isPassed(Cell[][] board) {
    // Check the same file and adjacent files ahead of the pawn
    int startRank = isWhite ? curPos.rank + 1 : 0;
    int endRank = isWhite ? 8 : curPos.rank;

    // Check own file
    if (Board.countAlongFile(board, "Pawn", !isWhite, startRank, curPos.file, isWhite) > 0) {
      return false;
    }

    // Check adjacent files
    if (curPos.file > 0) {
      if (Board.countAlongFile(board, "Pawn", !isWhite, startRank, curPos.file - 1, isWhite) > 0) {
        return false;
      }
    }

    if (curPos.file < 7) {
      if (Board.countAlongFile(board, "Pawn", !isWhite, startRank, curPos.file + 1, isWhite) > 0) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if the pawn has a connected pawn on an adjacent file.
   */
  private boolean hasConnectedPawn(Board board) {
    if (curPos.file > 0) {
      Cell leftCell = board.board[curPos.rank][curPos.file - 1];
      if (leftCell.PieceStatus == stat && leftCell.piece.getName().equals("Pawn")) {
        return true;
      }
    }

    if (curPos.file < 7) {
      Cell rightCell = board.board[curPos.rank][curPos.file + 1];
      if (rightCell.PieceStatus == stat && rightCell.piece.getName().equals("Pawn")) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if the pawn is isolated (no friendly pawns on adjacent files).
   */
  private boolean isIsolated(Board board) {
    boolean hasLeftPawn = false;
    boolean hasRightPawn = false;

    if (curPos.file > 0) {
      hasLeftPawn = Board.countAlongFile(board.board, "Pawn", isWhite, 0, curPos.file - 1, true) > 0;
    }

    if (curPos.file < 7) {
      hasRightPawn = Board.countAlongFile(board.board, "Pawn", isWhite, 0, curPos.file + 1, true) > 0;
    }

    return !hasLeftPawn && !hasRightPawn;
  }

  /**
   * Checks if the pawn is backward (behind friendly pawns and can't advance safely).
   */
  private boolean isBackward(Board board) {
    // Check if there are friendly pawns ahead on adjacent files
    int direction = isWhite ? 1 : -1;

    if (curPos.file > 0) {
      Coordinate leftAhead = new Coordinate(curPos.file - 1, curPos.rank + direction);
      if (leftAhead.rank >= 0 && leftAhead.rank < 8) {
        Cell cell = board.board[leftAhead.rank][leftAhead.file];
        if (cell.PieceStatus == stat && cell.piece.getName().equals("Pawn")) {
          return false;  // Has pawn support, not backward
        }
      }
    }

    if (curPos.file < 7) {
      Coordinate rightAhead = new Coordinate(curPos.file + 1, curPos.rank + direction);
      if (rightAhead.rank >= 0 && rightAhead.rank < 8) {
        Cell cell = board.board[rightAhead.rank][rightAhead.file];
        if (cell.PieceStatus == stat && cell.piece.getName().equals("Pawn")) {
          return false;  // Has pawn support, not backward
        }
      }
    }

    // Check if adjacent pawns are behind us
    boolean hasAdjacentPawns = false;
    if (curPos.file > 0) {
      int leftPawnCount = Board.countAlongFile(board.board, "Pawn", isWhite, 0, curPos.file - 1, true);
      if (leftPawnCount > 0) hasAdjacentPawns = true;
    }

    if (curPos.file < 7) {
      int rightPawnCount = Board.countAlongFile(board.board, "Pawn", isWhite, 0, curPos.file + 1, true);
      if (rightPawnCount > 0) hasAdjacentPawns = true;
    }

    return hasAdjacentPawns;  // Has adjacent pawns but they're all behind
  }

  /**
   * Evaluates pawn safety based on attackers and defenders.
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

    // Remove matching pieces to simplify evaluation
    ArrayList<Piece> copyProtectors = (ArrayList<Piece>) protectors.clone();
    ArrayList<Piece> copyAttackers = (ArrayList<Piece>) attackers.clone();

    removeMatchingPieces(copyProtectors, copyAttackers);

    // Protected by two pawns - very strong
    if (countByType(copyProtectors, "Pawn") >= 2) {
      return 1.25 + 0.1 * (protectors.size() - attackers.size());
    }

    // Attacked by two pawns - very weak
    if (countByType(copyAttackers, "Pawn") >= 2) {
      return 0.6 - 0.1 * (attackers.size() - protectors.size());
    }

    // Protected by one pawn - strong
    if (countByType(copyProtectors, "Pawn") == 1) {
      return 1.15 + 0.1 * (protectors.size() - attackers.size());
    }

    // Attacked by one pawn - weak
    if (countByType(copyAttackers, "Pawn") == 1) {
      return 0.8 - 0.1 * (attackers.size() - protectors.size());
    }

    // Protected by minor piece
    if (countByType(copyProtectors, "Knight") + countByType(copyProtectors, "Bishop") > 0) {
      return 1.1 + 0.1 * (protectors.size() - attackers.size());
    }

    // Attacked by minor piece
    if (countByType(copyAttackers, "Knight") + countByType(copyAttackers, "Bishop") > 0) {
      return 0.9 - 0.1 * (attackers.size() - protectors.size());
    }

    return 1.0;
  }

  /**
   * Removes matching piece types from both lists.
   */
  private void removeMatchingPieces(ArrayList<Piece> protectors, ArrayList<Piece> attackers) {
    String[] pieceTypes = {"Pawn", "Knight", "Bishop", "Rook", "Queen", "King"};

    for (String pieceType : pieceTypes) {
      while (countByType(protectors, pieceType) > 0 && countByType(attackers, pieceType) > 0) {
        removeByName(protectors, pieceType);
        if (attackers.size() == 0) break;
      }
    }
  }

  private static int countByType(ArrayList<Piece> pieces, String pieceName) {
    int count = 0;
    for (Piece piece : pieces) {
      if (piece.getName().equals(pieceName)) {
        count++;
      }
    }
    return count;
  }

  private boolean removeByName(List<Piece> pieces, String pieceName) {
    for (int i = 0; i < pieces.size(); i++) {
      Piece piece = pieces.get(i);
      if (piece.getName().equals(pieceName) ||
          (piece.getName().equals("Bishop") && pieceName.equals("Knight")) ||
          (piece.getName().equals("Knight") && pieceName.equals("Bishop"))) {
        pieces.remove(i);
        return true;
      }
    }
    return false;
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

  public void unReveal() {
    this.isRevealChecker = false;
    this.revealAve = null;
    this.revealCheckerLoc = new Coordinate(-1, -1);
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
  public ArrayList<Coordinate> generateAvenue(Coordinate c1, Coordinate c2) {
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
    Pawn copy = new Pawn(fromPos, curPos, isWhite, firstMove);
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
    copy.enPassantable = enPassantable;
    return copy;
  }
}