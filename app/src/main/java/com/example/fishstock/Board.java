package com.example.fishstock;

import com.example.fishstock.Pieces.*;

import java.io.Serializable;
import java.util.*;

public class Board implements Serializable {
  // ANSI color codes for console output
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_CYAN = "\u001B[36m";

  public Cell[][] board = new Cell[8][8];
  public ArrayList<Piece> whitePieces = new ArrayList<>();
  public ArrayList<Piece> blackPieces = new ArrayList<>();

  /**
   * Default constructor - generates standard starting chess position.
   */
  public Board() {
    initializeStandardBoard();
  }

  /**
   * Constructor for custom board configuration with existing pieces.
   */
  public Board(List<Piece> whitePieces, List<Piece> blackPieces) {
    initializeEmptyBoard();

    if (whitePieces != null) {
      placePieces(whitePieces, true);
    }

    if (blackPieces != null) {
      placePieces(blackPieces, false);
    }
  }

  /**
   * Copy constructor (alternative).
   */
  public Board(Cell[][] board, List<Piece> whitePieces, List<Piece> blackPieces) {
    // Copy board cells
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        this.board[rank][file] = board[rank][file];
      }
    }

    // Copy white pieces
    if (whitePieces != null) {
      this.whitePieces.addAll(whitePieces);
    }

    // Copy black pieces
    if (blackPieces != null) {
      this.blackPieces.addAll(blackPieces);
    }
  }

  /**
   * Initializes an empty 8x8 board with no pieces.
   */
  private void initializeEmptyBoard() {
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        boolean isLightSquare = ((rank + file) % 2) == 0;
        Cell cell = new Cell(isLightSquare);
        cell.setStatus(Status.EMPTY);
        board[rank][file] = cell;
      }
    }
  }

  /**
   * Places pieces on the board and adds them to piece lists.
   */
  private void placePieces(List<Piece> pieces, boolean isWhite) {
    for (Piece piece : pieces) {
      if (piece == null) {
        continue;
      }

      Coordinate position = piece.getPos();
      Cell cell = board[position.rank][position.file];
      cell.piece = piece;
      cell.Symbol = piece.getSymbol();
      cell.setStatus(isWhite ? Status.WHITE : Status.BLACK);

      if (isWhite) {
        whitePieces.add(piece);
      } else {
        blackPieces.add(piece);
      }
    }
  }

  /**
   * Initializes the standard starting chess position.
   */
  private void initializeStandardBoard() {
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 0; file < 8; file++) {
        boolean isLightSquare = ((rank + file) % 2) == 0;
        Cell cell = new Cell(isLightSquare);
        Coordinate coord = new Coordinate(file, rank);

        // White pieces (ranks 0-1)
        if (rank == 0) {
          setupBackRank(cell, coord, file, true);
        } else if (rank == 1) {
          setupPawnRank(cell, coord, true);
        }
        // Black pieces (ranks 6-7)
        else if (rank == 6) {
          setupPawnRank(cell, coord, false);
        } else if (rank == 7) {
          setupBackRank(cell, coord, file, false);
        }

        board[rank][file] = cell;
      }
    }
  }

  /**
   * Sets up back rank pieces (rank 0 for white, rank 7 for black).
   */
  private void setupBackRank(Cell cell, Coordinate coord, int file, boolean isWhite) {
    Piece piece = null;

    switch (file) {
      case 0:
      case 7:
        piece = new Rook(coord, coord, isWhite);
        cell.putRook(isWhite, (Rook) piece);
        break;
      case 1:
      case 6:
        piece = new Knight(coord, coord, isWhite);
        cell.putKnight(isWhite, (Knight) piece);
        break;
      case 2:
      case 5:
        piece = new Bishop(coord, coord, isWhite);
        cell.putBishop(isWhite, (Bishop) piece);
        break;
      case 3:
        piece = new King(coord, isWhite);
        cell.putKing(isWhite, (King) piece);
        break;
      case 4:
        piece = new Queen(coord, coord, isWhite);
        cell.putQueen(isWhite, (Queen) piece);
        break;
    }

    if (piece != null) {
      cell.setStatus(isWhite ? Status.WHITE : Status.BLACK);
      if (isWhite) {
        if (piece instanceof King) {
          whitePieces.add(0, piece); // King always first
        } else {
          whitePieces.add(piece);
        }
      } else {
        if (piece instanceof King) {
          blackPieces.add(0, piece); // King always first
        } else {
          blackPieces.add(piece);
        }
      }
    }
  }

  /**
   * Sets up pawn rank (rank 1 for white, rank 6 for black).
   */
  private void setupPawnRank(Cell cell, Coordinate coord, boolean isWhite) {
    Pawn pawn = new Pawn(coord, coord, isWhite, true);
    cell.putPawn(isWhite, pawn);
    cell.setStatus(isWhite ? Status.WHITE : Status.BLACK);

    if (isWhite) {
      whitePieces.add(pawn);
    } else {
      blackPieces.add(pawn);
    }
  }

  /**
   * Finds the index of a piece at a given coordinate in the piece list.
   * Returns -1 if not found.
   */
  public static int getIndex(ArrayList<Piece> pieces, Coordinate coord) {
    for (int i = 0; i < pieces.size(); i++) {
      Coordinate piecePos = pieces.get(i).getPos();
      if (piecePos.file == coord.file && piecePos.rank == coord.rank) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Updates board by moving a piece to a new position.
   * @deprecated This method seems unused - consider using GameService.makeMove instead
   */
  @Deprecated
  public static Cell[][] updateBoard(Cell[][] chessBoard, ArrayList<Piece> pieces,
                                     Coordinate move, String pieceName) {
    for (Piece piece : pieces) {
      if (piece.getClass().getSimpleName().equals(pieceName)) {
        for (Move mv : piece.generateMoves(piece.getPos(), chessBoard)) {
          if (mv.toCoord.file == move.file && mv.toCoord.rank == move.rank) {
            Coordinate oldPos = piece.getPos();
            chessBoard[oldPos.rank][oldPos.file].empty();
            chessBoard[move.rank][move.file].putPiece(piece);
            return chessBoard;
          }
        }
      }
    }
    return chessBoard;
  }

  /**
   * Prints the board to console with colored pieces.
   * @param board The board to print
   * @param whitesPOV If true, prints from white's perspective (rank 1 at bottom)
   */
  public static void printBoard(Board board, boolean whitesPOV) {
    if (whitesPOV) {
      printBoardWhitePerspective(board);
    } else {
      printBoardBlackPerspective(board);
    }
  }

  private static void printBoardWhitePerspective(Board board) {
    for (int rank = 7; rank >= 0; rank--) {
      for (int file = 0; file < 8; file++) {
        printCell(board.board[rank][file]);
      }
      System.out.println();
    }
  }

  private static void printBoardBlackPerspective(Board board) {
    for (int rank = 0; rank < 8; rank++) {
      for (int file = 7; file >= 0; file--) {
        printCell(board.board[rank][file]);
      }
      System.out.println();
    }
  }

  private static void printCell(Cell cell) {
    String symbol = String.valueOf(cell.Symbol);

    if (cell.PieceStatus == Status.BLACK) {
      System.out.print(" " + ANSI_RED + symbol + ANSI_RESET + " ");
    } else if (cell.PieceStatus == Status.WHITE) {
      System.out.print(" " + ANSI_CYAN + symbol + ANSI_RESET + " ");
    } else {
      System.out.print(" " + symbol + " ");
    }
  }

  /**
   * Generates a random board configuration for testing.
   * WARNING: This method is incomplete and has a bug in the while loop logic.
   */
  public static Board generateRandomBoard() {
    Random random = new Random();
    Board board = new Board(null, null);

    // Place white king
    Coordinate whiteKingCoord = Coordinate.generateRandomCoordinate();
    board.board[whiteKingCoord.rank][whiteKingCoord.file].putPiece(
        new King(whiteKingCoord, true));
    board.board[whiteKingCoord.rank][whiteKingCoord.file].setStatus(Status.WHITE);

    // Place black king (ensure not adjacent to white king)
    Coordinate blackKingCoord;
    do {
      blackKingCoord = Coordinate.generateRandomCoordinate();
    } while (areKingsTooClose(whiteKingCoord, blackKingCoord));

    board.board[blackKingCoord.rank][blackKingCoord.file].putPiece(
        new King(blackKingCoord, false));
    board.board[blackKingCoord.rank][blackKingCoord.file].setStatus(Status.BLACK);

    // Place random pawns
    placePawnsRandomly(board, random.nextInt(9), true); // 0-8 white pawns
    placePawnsRandomly(board, random.nextInt(9), false); // 0-8 black pawns

    // Place other pieces randomly
    placeRandomPieces(board, random);

    return board;
  }

  /**
   * Checks if two king positions are too close (adjacent squares).
   */
  private static boolean areKingsTooClose(Coordinate king1, Coordinate king2) {
    int fileDiff = Math.abs(king1.file - king2.file);
    int rankDiff = Math.abs(king1.rank - king2.rank);
    return fileDiff <= 1 && rankDiff <= 1;
  }

  /**
   * Places pawns randomly on the board.
   */
  private static void placePawnsRandomly(Board board, int numPawns, boolean isWhite) {
    int placed = 0;
    int attempts = 0;
    int maxAttempts = 100;

    while (placed < numPawns && attempts < maxAttempts) {
      Coordinate coord = Coordinate.generateRandomCoordinate();

      // Pawns can't be on first or last rank
      if (coord.rank == 0 || coord.rank == 7) {
        attempts++;
        continue;
      }

      if (board.board[coord.rank][coord.file].PieceStatus == Status.EMPTY) {
        Pawn pawn = new Pawn(coord, coord, isWhite, true);
        board.board[coord.rank][coord.file].putPiece(pawn);
        board.board[coord.rank][coord.file].setStatus(isWhite ? Status.WHITE : Status.BLACK);
        placed++;
      }
      attempts++;
    }
  }

  /**
   * Places random pieces (rooks, knights, bishops, queens) on the board.
   */
  private static void placeRandomPieces(Board board, Random random) {
    // Up to 2 of each piece type per side
    placeRandomPieceType(board, random, "Rook", random.nextInt(3), true);
    placeRandomPieceType(board, random, "Rook", random.nextInt(3), false);
    placeRandomPieceType(board, random, "Knight", random.nextInt(3), true);
    placeRandomPieceType(board, random, "Knight", random.nextInt(3), false);
    placeRandomPieceType(board, random, "Bishop", random.nextInt(3), true);
    placeRandomPieceType(board, random, "Bishop", random.nextInt(3), false);
    placeRandomPieceType(board, random, "Queen", random.nextInt(2), true);
    placeRandomPieceType(board, random, "Queen", random.nextInt(2), false);
  }

  /**
   * Helper to place a specific piece type randomly.
   */
  private static void placeRandomPieceType(Board board, Random random,
                                           String pieceType, int count, boolean isWhite) {
    int placed = 0;
    int attempts = 0;
    int maxAttempts = 100;

    while (placed < count && attempts < maxAttempts) {
      Coordinate coord = Coordinate.generateRandomCoordinate();

      if (board.board[coord.rank][coord.file].PieceStatus == Status.EMPTY) {
        Piece piece = createPiece(pieceType, coord, isWhite);
        if (piece != null) {
          board.board[coord.rank][coord.file].putPiece(piece);
          board.board[coord.rank][coord.file].setStatus(isWhite ? Status.WHITE : Status.BLACK);
          placed++;
        }
      }
      attempts++;
    }
  }

  /**
   * Factory method to create pieces by type name.
   */
  private static Piece createPiece(String type, Coordinate coord, boolean isWhite) {
    switch (type) {
      case "Rook":
        return new Rook(coord, coord, isWhite);
      case "Knight":
        return new Knight(coord, coord, isWhite);
      case "Bishop":
        return new Bishop(coord, coord, isWhite);
      case "Queen":
        return new Queen(coord, coord, isWhite);
      default:
        return null;
    }
  }

  /**
   * Compares two boards for equality (same pieces in same positions).
   */
  public static boolean compareBoard(Board board1, Board board2) {
    // Check if piece counts match
    if (board1.whitePieces.size() != board2.whitePieces.size()
        || board1.blackPieces.size() != board2.blackPieces.size()) {
      return false;
    }

    // Check all white pieces match
    if (!allPiecesMatch(board1.whitePieces, board2.whitePieces)) {
      return false;
    }

    // Check all black pieces match
    if (!allPiecesMatch(board1.blackPieces, board2.blackPieces)) {
      return false;
    }

    return true;
  }

  /**
   * Checks if all pieces in list1 have matching pieces in list2.
   */
  private static boolean allPiecesMatch(List<Piece> list1, List<Piece> list2) {
    for (Piece piece1 : list1) {
      boolean foundMatch = false;

      for (Piece piece2 : list2) {
        if (piece1.getName().equals(piece2.getName())
            && Coordinate.compareCoords(piece1.getPos(), piece2.getPos())) {
          foundMatch = true;
          break;
        }
      }

      if (!foundMatch) {
        return false;
      }
    }
    return true;
  }

  /**
   * Counts pieces of a specific type along a file (column).
   *
   * @param board The chess board
   * @param pieceName Name of piece to count
   * @param lookingForWhite Whether to count white pieces (false = black)
   * @param startingRow Row to start counting from
   * @param file The file (column) to search
   * @param upRow If true, search upward; if false, search downward
   * @return Number of matching pieces found
   */
  public static int countAlongFile(Cell[][] board, String pieceName, boolean lookingForWhite,
                                   int startingRow, int file, boolean upRow) {
    if (!isValidSquare(startingRow, file)) {
      return 0;
    }

    int count = 0;
    int startRow = startingRow;
    int endRow = upRow ? 8 : -1;
    int step = upRow ? 1 : -1;

    for (int row = startRow; row != endRow; row += step) {
      Cell cell = board[row][file];

      if (cell.PieceStatus != Status.EMPTY
          && cell.piece.getName().equals(pieceName)
          && cell.piece.getColor() == lookingForWhite) {
        count++;
      }
    }

    return count;
  }

  /**
   * Counts pieces of a specific type along a diagonal.
   *
   * @param board The chess board
   * @param pieceName Name of piece to count
   * @param coord Starting coordinate
   * @param upFile If true, moves toward higher files; if false, toward lower files
   * @param isWhite Whether to count white pieces (false = black)
   * @return Number of matching pieces found
   */
  public static int countAlongDiagonal(Cell[][] board, String pieceName, Coordinate coord,
                                       boolean upFile, boolean isWhite) {
    if (!isValidSquare(coord.rank, coord.file)) {
      return 0;
    }

    int count = 0;
    int rank = coord.rank;
    int file = coord.file;

    // Determine direction based on color and upFile parameter
    int rankStep = isWhite ? 1 : -1;
    int fileStep = upFile ? 1 : -1;

    while (isValidSquare(rank, file)) {
      Cell cell = board[rank][file];

      if (cell.PieceStatus != Status.EMPTY
          && cell.piece.getName().equals(pieceName)
          && cell.piece.getColor() == isWhite) {
        count++;
      }

      rank += rankStep;
      file += fileStep;
    }

    return count;
  }

  /**
   * Checks if a square coordinate is valid (within board bounds).
   */
  private static boolean isValidSquare(int rank, int file) {
    return rank >= 0 && rank < 8 && file >= 0 && file < 8;
  }
}