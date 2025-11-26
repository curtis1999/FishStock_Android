package com.example.fishstock;

import com.example.fishstock.Agents.*;
import com.example.fishstock.Pieces.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameService implements Serializable {

  public static boolean isRepetition(List<Board> boardStates, Board board) {
    return boardStates.stream()
        .filter(curBoard -> Board.compareBoard(board, curBoard))
        .count() >= 3;
  }

  public static void makeMove(Board chessBoard, Move move, boolean isWhite) {
    ArrayList<Piece> activePieces = isWhite ? chessBoard.whitePieces : chessBoard.blackPieces;
    ArrayList<Piece> opponentPieces = isWhite ? chessBoard.blackPieces : chessBoard.whitePieces;

    int pieceIndex = Board.getIndex(activePieces, move.fromCoord);

    // 1. Move piece on board
    movePieceOnBoard(chessBoard, move, activePieces.get(pieceIndex));

    // 2. Handle capture
    if (move.isCapture) {
      handleCapture(chessBoard, move, opponentPieces);
    }

    // 3. Handle castling
    if (move.isCastle) {
      handleCastling(chessBoard, move, isWhite, activePieces);
    }

    // 4. Handle promotion
    if (move.isPromotion) {
      handlePromotion(chessBoard, move, activePieces);
    }

    // 5. Final updates
    finalizeMove(chessBoard, move, activePieces, pieceIndex);
  }

  private static void movePieceOnBoard(Board board, Move move, Piece piece) {
    board.board[move.toCoord.rank][move.toCoord.file].empty();
    board.board[move.toCoord.rank][move.toCoord.file].putPiece(move.piece);
    board.board[move.fromCoord.rank][move.fromCoord.file].empty();
    piece.setPos(move.toCoord);
  }

  private static void handleCapture(Board board, Move move, ArrayList<Piece> opponentPieces) {
    int capturedIndex = Board.getIndex(opponentPieces, move.capturablePiece.getPos());
    opponentPieces.remove(capturedIndex);

    if (move.isEnPassant) {
      board.board[move.capturablePiece.getPos().rank][move.capturablePiece.getPos().file].empty();
    }
  }

  private static void handleCastling(Board board, Move move, boolean isWhite, ArrayList<Piece> pieces) {
    King king = (King) pieces.get(0);
    king.moved();

    int rank = isWhite ? 0 : 7;
    boolean isShortCastle = move.toCoord.file == 1;

    Coordinate rookFrom = new Coordinate(isShortCastle ? 0 : 7, rank);
    Coordinate rookTo = new Coordinate(isShortCastle ? 2 : 4, rank);

    int rookIndex = Board.getIndex(pieces, rookFrom);
    pieces.get(rookIndex).setPos(rookTo);
    board.board[rank][rookTo.file].putRook(isWhite, pieces.get(rookIndex));
    board.board[rank][rookFrom.file].empty();
  }

  private static void handlePromotion(Board board, Move move, ArrayList<Piece> pieces) {
    int promotionIndex = Board.getIndex(pieces, move.toCoord);
    pieces.remove(promotionIndex);
    move.promotionPiece.setPos(move.toCoord);
    pieces.add(move.promotionPiece);
    board.board[move.toCoord.rank][move.toCoord.file].empty();
    board.board[move.toCoord.rank][move.toCoord.file].putPiece(move.promotionPiece);
  }

  private static void finalizeMove(Board board, Move move, ArrayList<Piece> pieces, int pieceIndex) {
    ((King) board.blackPieces.get(0)).unCheck();
    ((King) board.whitePieces.get(0)).unCheck();

    String pieceName = move.piece.getName();

    if (pieceName.equals("Pawn") && !move.isPromotion) {
      Pawn pawn = (Pawn) board.board[move.toCoord.rank][move.toCoord.file].piece;
      pawn.growUp();
      ((Pawn) pieces.get(pieceIndex)).growUp();

      int rankDiff = Math.abs(move.toCoord.rank - move.fromCoord.rank);
      if (rankDiff == 2) {
        pawn.setEnPassantable();
        ((Pawn) pieces.get(pieceIndex)).setEnPassantable();
      }
    } else if (pieceName.equals("Rook")) {
      ((Rook) board.board[move.toCoord.rank][move.toCoord.file].piece).moved();
    } else if (pieceName.equals("King")) {
      ((King) board.board[move.toCoord.rank][move.toCoord.file].piece).moved();
      ((King) pieces.get(pieceIndex)).moved();
    }

    move.piece.setPossibleMoves(move.piece.generateMoves(move.piece.getPos(), board.board));
  }

  public static void undoMove(Board chessBoard, Move move, boolean isWhite) {
    ArrayList<Piece> activePieces = isWhite ? chessBoard.whitePieces : chessBoard.blackPieces;
    ArrayList<Piece> opponentPieces = isWhite ? chessBoard.blackPieces : chessBoard.whitePieces;

    int pieceIndex = Board.getIndex(activePieces, move.toCoord);

    // 1. Restore piece position
    chessBoard.board[move.fromCoord.rank][move.fromCoord.file].putPiece(move.piece);
    chessBoard.board[move.toCoord.rank][move.toCoord.file].empty();
    activePieces.get(pieceIndex).setPos(move.fromCoord);

    // 2. Restore captured piece
    if (move.isCapture) {
      if (move.isEnPassant) {
        chessBoard.board[move.capturablePiece.getPos().rank][move.capturablePiece.getPos().file]
            .putPiece(move.capturablePiece);
      } else {
        opponentPieces.add(move.capturablePiece);
      }
    }

    // 3. Undo castling
    if (move.isCastle) {
      undoCastling(chessBoard, move, isWhite, activePieces);
    }

    // 4. Undo promotion
    if (move.isPromotion) {
      undoPromotion(chessBoard, move, activePieces, isWhite);
    }

    // 5. Reset piece states
    resetPieceStates(chessBoard, move, isWhite);
  }

  private static void undoCastling(Board board, Move move, boolean isWhite,ArrayList<Piece> pieces) {
    int rank = isWhite ? 0 : 7;
    boolean isShortCastle = move.toCoord.file == 1;

    Coordinate rookFrom = new Coordinate(isShortCastle ? 2 : 4, rank);
    Coordinate rookTo = new Coordinate(isShortCastle ? 0 : 7, rank);

    int rookIndex = Board.getIndex(pieces, rookFrom);
    pieces.get(rookIndex).setPos(rookTo);
    board.board[rank][rookTo.file].putRook(isWhite, pieces.get(rookIndex));
    board.board[rank][rookFrom.file].empty();
  }

  private static void undoPromotion(Board board, Move move,ArrayList<Piece> pieces, boolean isWhite) {
    int pieceIndex = Board.getIndex(pieces, move.toCoord);
    pieces.remove(pieceIndex);
    pieces.add(new Pawn(move.toCoord, isWhite));
  }

  private static void resetPieceStates(Board board, Move move, boolean isWhite) {
    String pieceName = move.piece.getName();

    if (pieceName.equals("Pawn")) {
      Pawn pawn = (Pawn) board.board[move.fromCoord.rank][move.fromCoord.file].piece;
      pawn.enPassantable = false;
    } else if (pieceName.equals("King")) {
      King king = (King) board.board[move.fromCoord.rank][move.fromCoord.file].piece;
      king.hasMoved = false;
    }

    if (move.isCheck) {
      King opponentKing = isWhite ?
          (King) board.blackPieces.get(0) :
          (King) board.whitePieces.get(0);
      opponentKing.unCheck();
    }
  }

  public static ArrayList<Move> generateMoves(Board board, boolean isWhite) {
    ArrayList<Move> moves = new ArrayList<>();
    ArrayList<Piece> pieces = isWhite ? board.whitePieces : board.blackPieces;

    for (Piece piece : pieces) {
      if (piece.isPinned()) continue;

      ArrayList<Move> filteredMoves = filterMoves(piece.generateMoves(piece.getPos(), board.board));

      for (Move move : filteredMoves) {
        if (move.isPromotion) {
          moves.addAll(generatePromotionMoves(move, isWhite));
        } else {
          moves.add(move);
        }
      }
    }
    return moves;
  }

  private static ArrayList<Move> generatePromotionMoves(Move move, boolean isWhite) {
    ArrayList<Move> promotionMoves = new ArrayList<>();
    Piece[] promotionPieces = {
        new Knight(move.toCoord, isWhite),
        new Bishop(move.toCoord, isWhite),
        new Rook(move.toCoord, isWhite),
        new Queen(move.toCoord, isWhite)
    };

    for (Piece promotionPiece : promotionPieces) {
      Move promotionMove = new Move(move.fromCoord, move.toCoord, "Pawn", move.isCapture, true);
      promotionMove.setPromotion(promotionPiece);
      if (move.isCapture) {
        promotionMove.setCapture(move.capturablePiece);
      }
      promotionMoves.add(promotionMove);
    }

    return promotionMoves;
  }

  public static ArrayList<Move> generateMovesDoubleCheck(Board board, ArrayList<Move> possibleMoves, boolean isWhite) {
    ArrayList<Move> validMoves = new ArrayList<>();
    King king = (King) (isWhite ? board.whitePieces.get(0) : board.blackPieces.get(0));
    ArrayList<Coordinate> checkingAve1 = king.getCheckingAve();
    ArrayList<Coordinate> checkingAve2 = king.getCheckingAve2();

    for (Move move : possibleMoves) {
      if (!move.piece.getName().equals("King")) continue;

      boolean isAttacked = isWhite ?
          board.board[move.toCoord.rank][move.toCoord.file].blackAttackers.size() > 0 :
          board.board[move.toCoord.rank][move.toCoord.file].whiteAttackers.size() > 0;

      if (isAttacked) continue;

      boolean inCheckingAve = isInCheckingAvenue(move.toCoord, checkingAve1) ||
          isInCheckingAvenue(move.toCoord, checkingAve2);

      if (!inCheckingAve) {
        validMoves.add(move);
      }
    }

    return validMoves;
  }

  public static ArrayList<Move> generateMovesCheck(Board board, ArrayList<Move> possibleMoves, boolean isWhite) {
    King king = (King) (isWhite ? board.whitePieces.get(0) : board.blackPieces.get(0));
    Coordinate checkerLoc = king.checkerLoc;
    Piece checkingPiece = board.board[checkerLoc.rank][checkerLoc.file].piece;
    Coordinate kingLoc = king.getPos();

    ArrayList<Coordinate> checkingAvenue = getCheckingAvenue(checkingPiece, checkerLoc, kingLoc);
    ArrayList<Coordinate> trimmedCheckingAvenue = getCheckingAvenueTrimmed(checkingPiece, checkerLoc, kingLoc);
    ArrayList<Move> validMoves = new ArrayList<>();

    for (Move move : possibleMoves) {
      if (move.piece.getName().equals("King")) {
        if (isKingMoveValidInCheck(board, move, checkingAvenue, checkingPiece, isWhite)) {
          validMoves.add(move);
        }
      } else {
        if (isInCheckingAvenue(move.toCoord, trimmedCheckingAvenue)) {
          validMoves.add(move);
        }
      }
    }

    return validMoves;
  }

  private static boolean isKingMoveValidInCheck(Board board, Move move, ArrayList<Coordinate> checkingAvenue, Piece checkingPiece, boolean isWhite) {
    Cell targetCell = board.board[move.toCoord.rank][move.toCoord.file];

    // Check if king is moving to an attacked square
    boolean isAttacked = isWhite ?
        targetCell.blackAttackers.size() > 0 :
        targetCell.whiteAttackers.size() > 0;

    // If capturing the checking piece, we need to verify it's not protected
    if (move.isCapture && move.capturablePiece.equals(checkingPiece)) {
      // Count attackers excluding the checking piece itself
      int numAttackers = isWhite ?
          (int) targetCell.blackAttackers.stream().filter(p -> !p.equals(checkingPiece)).count() :
          (int) targetCell.whiteAttackers.stream().filter(p -> !p.equals(checkingPiece)).count();

      return numAttackers == 0; // Can capture if not protected by another piece
    }

    // For non-capture moves, square must not be attacked and not in checking avenue
    return !isAttacked && !isInCheckingAvenue(move.toCoord, checkingAvenue);
  }

  private static boolean isKingMoveValid(Board board, Move move, ArrayList<Coordinate> checkingAvenue, boolean isWhite) {
    boolean isAttacked = isWhite ?
        board.board[move.toCoord.rank][move.toCoord.file].blackAttackers.size() > 0 :
        board.board[move.toCoord.rank][move.toCoord.file].whiteAttackers.size() > 0;

    return !isAttacked && !isInCheckingAvenue(move.toCoord, checkingAvenue);
  }

  private static boolean isInCheckingAvenue(Coordinate coord, ArrayList<Coordinate> avenue) {
    return avenue.stream().anyMatch(c -> c.rank == coord.rank && c.file == coord.file);
  }

  private static ArrayList<Coordinate> getCheckingAvenueTrimmed(Piece checkingPiece, Coordinate checkerLoc, Coordinate kingLoc) {
    ArrayList<Coordinate> avenue = new ArrayList<>();
    avenue.add(checkerLoc);

    if (checkingPiece.getName().equals("Knight") || checkingPiece.getName().equals("Pawn")) {
      avenue.add(kingLoc);
      return avenue;
    }

    addCoordinatesBetween(avenue, checkerLoc, kingLoc, false);
    return avenue;
  }

  public static ArrayList<Coordinate> getCheckingAvenue(Piece checkingPiece, Coordinate checkerLoc, Coordinate kingLoc) {
    ArrayList<Coordinate> avenue = new ArrayList<>();
    avenue.add(checkerLoc);

    if (checkingPiece.getName().equals("Knight") || checkingPiece.getName().equals("Pawn")) {
      avenue.add(kingLoc);
      return avenue;
    }

    addCoordinatesBetween(avenue, checkerLoc, kingLoc, true);
    return avenue;
  }

  private static void addCoordinatesBetween(ArrayList<Coordinate> avenue, Coordinate from, Coordinate to, boolean extended) {
    int fileDiff = Integer.compare(to.file, from.file);
    int rankDiff = Integer.compare(to.rank, from.rank);

    Coordinate current = new Coordinate(from.file + fileDiff, from.rank + rankDiff);
    int limit = extended ? (to.rank + rankDiff) : to.rank;

    if (fileDiff == 0) { // Same file
      while (current.rank != limit && current.rank >= 0 && current.rank < 8) {
        avenue.add(current);
        current = new Coordinate(current.file, current.rank + rankDiff);
      }
    } else if (rankDiff == 0) { // Same rank
      limit = extended ? (to.file + fileDiff) : to.file;
      while (current.file != limit && current.file >= 0 && current.file < 8) {
        avenue.add(current);
        current = new Coordinate(current.file + fileDiff, current.rank);
      }
    } else { // Diagonal
      while (current.rank != limit && current.rank >= 0 && current.rank < 8) {
        avenue.add(current);
        current = new Coordinate(current.file + fileDiff, current.rank + rankDiff);
      }
    }
  }

  public static Board updateBoardMeta(Board board) {
    clearBoard(board);
    updatePiecesMeta(board, board.whitePieces, true);
    updatePiecesMeta(board, board.blackPieces, false);
    return board;
  }

  private static void updatePiecesMeta(Board board,ArrayList<Piece> pieces, boolean isWhite) {
    ArrayList<Piece> opponentPieces = isWhite ? board.blackPieces : board.whitePieces;

    for (Piece piece : pieces) {
      ArrayList<Move> rawMoves = piece.generateMoves(piece.getPos(), board.board);

      for (Move move : rawMoves) {
        if (shouldSkipMove(move)) continue;

        if (move.protectionMove) {
          handleProtectionMove(board, move);
        } else if (move.isCapture) {
          handleCaptureMetadata(board, move, opponentPieces, isWhite);
        } else {
          board.board[move.toCoord.rank][move.toCoord.file].addAttacker(move.piece);
        }
      }
    }
  }

  private static boolean shouldSkipMove(Move move) {
    return move.piece == null ||
        (move.piece.getName().equals("Pawn") &&
            !(move.coverMove || move.protectionMove || move.isCapture));
  }

  private static void handleProtectionMove(Board board, Move move) {
    board.board[move.toCoord.rank][move.toCoord.file].addAttacker(move.piece);
    board.board[move.toCoord.rank][move.toCoord.file].piece.addProtector(move.piece);

    // Determine which piece list to use based on the moving piece's color
    ArrayList<Piece> revealPieces = move.piece.getColor() ? board.whitePieces : board.blackPieces;

    if (move.isReveal) {
      int revealIndex = Board.getIndex(revealPieces, move.getRevealLoc());
      if (revealIndex != -1) {
        revealPieces.get(revealIndex).setReveal();
      }
    }
    if (move.isRevealQueen) {
      int revealIndex = Board.getIndex(revealPieces, move.getRevealQueenLocation());
      if (revealIndex != -1) {
        revealPieces.get(revealIndex).setRevealQueen();
      }
    }
  }

  private static void handleCaptureMetadata(Board board, Move move,ArrayList<Piece> opponentPieces, boolean isWhite) {
    Coordinate capturePos = move.capturablePiece.getPos();

    if (move.capturablePiece.getName().equals("King")) {
      King opponentKing = (King) opponentPieces.get(0);
      opponentKing.setCheck(move.fromCoord, getCheckingAvenue(move.piece, move.fromCoord, opponentKing.getPos()));
    }

    board.board[capturePos.rank][capturePos.file].addAttacker(move.piece);
    board.board[capturePos.rank][capturePos.file].piece.addAttacker(move.piece);

    if (move.isPin) {
      int pinnedIndex = Board.getIndex(opponentPieces, move.getPinLoc());
      opponentPieces.get(pinnedIndex).setPin(move.pinAvenue, move.piece.getPos());
      King opponentKing = (King) opponentPieces.get(0);
      opponentKing.setXRay(move.piece);
    }

    if (move.isPinQueen) {
      int pinnedIndex = Board.getIndex(opponentPieces, move.getQueenPinLoc());
      opponentPieces.get(pinnedIndex).setQueenPin();
    }
  }

  public static Board clearBoard(Board board) {
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        board.board[i][j].blackAttackers = new ArrayList<>();
        board.board[i][j].whiteAttackers = new ArrayList<>();
        if (!board.board[i][j].PieceStatus.equals(Status.EMPTY)) {
          board.board[i][j].piece.reset();
        }
      }
    }

    board.whitePieces.forEach(Piece::reset);
    board.blackPieces.forEach(Piece::reset);

    return board;
  }

  public static Agent getAgent(String name, boolean isWhite) {
    switch (name) {
      case "Random":
        return new Randy(AgentType.RANDY, isWhite);
      case "FishStock":
        return new FishStock(AgentType.FISHSTOCK, isWhite);
      default:
        return new Human(AgentType.HUMAN, isWhite);
    }
  }

  public static ArrayList<Move> filterMoves(ArrayList<Move> rawMoves) {
    return rawMoves.stream()
        .filter(move -> !(move.protectionMove || move.coverMove))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
  }

  public static Board copyBoard(Board chessBoard) {
    ArrayList<Piece> copyWhitePieces = new ArrayList<>();
    ArrayList<Piece> copyBlackPieces = new ArrayList<>();
    Cell[][] copyBoard = new Cell[8][8];

    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        copyBoard[i][j] = Cell.copyCell(chessBoard.board[i][j]);

        if (copyBoard[i][j].PieceStatus.equals(Status.WHITE)) {
          addPieceToList(copyWhitePieces, copyBoard[i][j].piece);
        } else if (copyBoard[i][j].PieceStatus.equals(Status.BLACK)) {
          addPieceToList(copyBlackPieces, copyBoard[i][j].piece);
        }
      }
    }

    return new Board(copyBoard, copyWhitePieces, copyBlackPieces);
  }

  private static void addPieceToList(ArrayList<Piece> pieces, Piece piece) {
    if (piece.getName().equals("King")) {
      pieces.add(0, piece);
    } else {
      pieces.add(piece);
    }
  }

  public static boolean isDeadPosition(ArrayList<Piece> whitePieces,ArrayList<Piece> blackPieces) {
    return isInsufficientMaterial(whitePieces, blackPieces) &&
        isInsufficientMaterial(blackPieces, whitePieces);
  }

  private static boolean isInsufficientMaterial(List<Piece> pieces,ArrayList<Piece> opponentPieces) {
    if (pieces.size() == 1) return true;

    if (pieces.size() == 2) {
      String pieceName = pieces.get(1).getName();
      return (pieceName.equals("Bishop") || pieceName.equals("Knight")) &&
          opponentPieces.size() == 1;
    }

    return false;
  }

  public static int countPieces(String pieceName,ArrayList<Piece> pieces) {
    return (int) pieces.stream()
        .filter(p -> p.getName().equals(pieceName))
        .count();
  }

  public static int getSecondHighestValue(List<Piece> pieces) {
    ArrayList<Piece> sortedPieces = new ArrayList<>(pieces);
    sortedPieces.sort(Comparator.comparingInt(Piece::getValue).reversed());
    return sortedPieces.size() >= 2 ? sortedPieces.get(1).getValue() : 0;
  }
}