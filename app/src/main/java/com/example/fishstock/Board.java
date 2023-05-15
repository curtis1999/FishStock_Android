package com.example.fishstock;
import com.example.fishstock.Pieces.*;

import java.io.Serializable;
import java.util.*;

public class Board implements Serializable {
  public final static String ANCI_RESET = "\u001B[0m";
  public final static String ANCI_RED = "\u001B[31m";
  public final static String ANCI_CYAN = "\u001B[36m";

  public Cell[][] board = new Cell[8][8];
  public ArrayList<Piece> whitePieces = new ArrayList<>();
  public ArrayList<Piece> blackPieces = new ArrayList<>();
  //Generates the start configuration of the board and adds the pieces to the above arrayLists.
  public Board() {
    for (int i = 0; i < 8;
         i++) {    //Iterate down the board starting at the 8th rank going to the first rank
      for (int j = 0; j < 8;
           j++) { //Iterates across the files.  Starting at the a file and going to the h file.
        boolean colour = ((i + j) % 2) == 0;
        Cell cur = new Cell(colour);
        Coordinate coord = new Coordinate(j, i);
        if (i == 0) {
          if (j == 0 || j == 7) {
            Rook rook = new Rook(coord, coord, true);
            cur.putRook(true, rook);
            cur.setStatus(Status.WHITE);
            whitePieces.add(rook);
          } else if (j == 1 || j == 6) {
            Knight knight = new Knight(coord, coord, true);
            cur.putKnight(true, knight);
            cur.setStatus(Status.WHITE);
            whitePieces.add(knight);
          } else if (j == 2 || j == 5) {
            Bishop bishop = new Bishop(coord, coord, true);
            cur.putBishop(true, bishop);
            cur.setStatus(Status.WHITE);
            whitePieces.add(bishop);

          } else if (j == 3) {
            King king = new King(coord, true);//TODO: UPDATE
            cur.putKing(true, king);
            cur.setStatus(Status.WHITE);
            whitePieces.add(0, king); //King first in list
          } else if (j == 4) {
            Queen queen = new Queen(coord, coord, true);
            cur.putQueen(true, queen);
            cur.setStatus(Status.WHITE);
            whitePieces.add(queen);
          }
        } else if (i == 1) {
          Pawn pawn = new Pawn(coord, coord, true, true);
          cur.putPawn(true, pawn);
          cur.setStatus(Status.WHITE);
          whitePieces.add(pawn);
        } else if (i == 6) {

          Pawn pawn = new Pawn(coord, coord, false, true);
          cur.putPawn(false, pawn);
          cur.setStatus(Status.BLACK);
          blackPieces.add(pawn);

        } else if (i == 7) {
          if (j == 0 || j == 7) {
            Rook rook = new Rook(coord, coord, false);
            cur.putRook(false, rook);
            cur.setStatus(Status.BLACK);
            blackPieces.add(rook);
          } else if (j == 1 || j == 6) {
            Knight knight = new Knight(coord, coord, false);
            cur.setStatus(Status.BLACK);
            cur.putKnight(false, knight);
            blackPieces.add(knight);
          } else if (j == 2 || j == 5) {
            Bishop bishop = new Bishop(coord, coord, false);
            cur.setStatus(Status.BLACK);
            cur.putBishop(false, bishop);
            blackPieces.add(bishop);
          } else if (j == 3) {
            King king = new King(coord, false);
            cur.setStatus(Status.BLACK);
            cur.putKing(false, king);
            blackPieces.add(0, king); //Sets the king to the 1st element in the list
          } else if (j == 4) {
            Queen queen = new Queen(coord, coord, false);
            cur.setStatus(Status.BLACK);
            cur.putQueen(false, queen);
            blackPieces.add(queen);
          }
        }
        board[i][j] = cur;
      }
    }
  }

  public Board(Cell[][] board, List<Piece> whitePieces, List<Piece> blackPieces) {
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        this.board[i][j] = board[i][j];
      }
    }
    for (Piece piece : whitePieces) {
      Cell cur = new Cell(true);
      this.whitePieces.add(piece);
      Coordinate curPosition = piece.getPos();
      cur.piece = piece;
      cur.setStatus(Status.WHITE);
      board[curPosition.rank][curPosition.file] = cur;
    }
    for (Piece piece : blackPieces) {
      Cell cur = new Cell(true);
      this.blackPieces.add(piece);
      Coordinate curPosition = piece.getPos();
      cur.piece = piece;
      cur.setStatus(Status.BLACK);
      board[curPosition.rank][curPosition.file] = cur;
    }
  }
  //Overloaded constructor to customise the default piece placement for Testing.
  public Board(List<Piece> whitePieces, List<Piece> blackPieces) {
    for (int i = 0; i< 8; i++) {
      for (int j = 0; j< 8; j++) {
        Cell emptyCell = new Cell (true);
        emptyCell.setStatus(Status.EMPTY);
        board[i][j] = emptyCell;
      }
    }
    for (Piece piece : whitePieces) {
      Cell cur = new Cell(true);
      this.whitePieces.add(piece);
      Coordinate curPosition = piece.getPos();
      cur.piece = piece;
      cur.setStatus(Status.WHITE);
      board[curPosition.rank][curPosition.file] = cur;
      board[curPosition.rank][curPosition.file].Symbol = piece.getSymbol();
    }
    for (Piece piece : blackPieces) {
      Cell cur = new Cell(true);
      this.blackPieces.add(piece);
      Coordinate curPosition = piece.getPos();
      cur.piece = piece;
      cur.setStatus(Status.BLACK);
      board[curPosition.rank][curPosition.file] = cur;
      board[curPosition.rank][curPosition.file].Symbol = piece.getSymbol();
    }
  }
  //Looks up a piece based on the board coordinate.  Returns it's index in the list.
  public static int getIndex(ArrayList<Piece> pieces, Coordinate crd) {
    for (int i=0; i<pieces.size(); i++) {
      if (pieces.get(i).getPos().file == crd.file && pieces.get(i).getPos().rank == crd.rank) {
        return i;
      }
    }
    return -1;
  }

  public static Cell[][] updateBoard(Cell[][] ChessBoard, ArrayList<Piece> Piece, Coordinate move, String name) {
    for (Piece p : Piece) {
      if (p.getClass().toString().contains(name)) {
        for (Move mv: p.generateMoves(p.getPos(),ChessBoard)) {
          if (mv.toCoord.file==move.file && mv.toCoord.rank==move.rank) {
            ChessBoard[p.getPos().rank][p.getPos().file].empty(); //Empties the old square
            ChessBoard[move.rank][move.file].putPiece(p);
            return ChessBoard;
          }
        }
      } else {
        continue;
      }
    }

    return ChessBoard;
  }
  public static void printBoard(Board cBoard, boolean whitesPOV) {
    if (whitesPOV) {
      for (int i=7; i>=0; i--) {
        for (int j=7; j>=0; j--) {
          if(cBoard.board[i][j].PieceStatus==Status.BLACK) {
            System.out.print(" "+ANCI_RED + cBoard.board[i][j].Symbol+ ANCI_RESET+" ");
            continue;
          }else if (cBoard.board[i][j].PieceStatus==Status.WHITE){
            System.out.print(" "+ANCI_CYAN + cBoard.board[i][j].Symbol+ ANCI_RESET+" "); //TODO: CHANGE THE COLOUR
            //System.err.print(" "+cBoard.board[i][j].Symbol+" "); //Caused formatting issues.
            continue;
          }else {
            System.out.print(" "+cBoard.board[i][j].Symbol+" ");
          }
        }
        System.out.println("");
      }
    }else {
      for (int i=0; i<8; i++) {
        for (int j=0; j<8; j++) {
          if(cBoard.board[i][j].PieceStatus==Status.BLACK) {
            System.out.print(" "+ANCI_RED + cBoard.board[i][j].Symbol+ ANCI_RESET+" ");
            continue;
          }else if (cBoard.board[i][j].PieceStatus==Status.WHITE){
            System.out.print(" "+ANCI_CYAN + cBoard.board[i][j].Symbol+ ANCI_RESET+" "); //TODO: CHANGE THE COLOUR
            //System.err.print(" "+cBoard.board[i][j].Symbol+" "); //Caused formatting issues.
            continue;
          }else {
            System.out.print(" "+cBoard.board[i][j].Symbol+" ");
          }
        }
        System.out.println("");
      }
    }
  }

  public static boolean compareBoard(Board board1, Board board2) {
    //Check 1: Same size piece lists.
    if (board1.whitePieces.size() != board2.whitePieces.size()
    || board1.blackPieces.size() != board2.blackPieces.size()) {
      return false;
    }
    for (Piece piece1 : board1.whitePieces) {
      boolean hasMatch = false;
      for (Piece piece2 : board2.whitePieces) {
        if (piece1.getName().equals(piece2.getName())) {
          if (Coordinate.compareCoords(piece1.getPos(), piece2.getPos())){
            hasMatch = true;
            break;
          }
        }
      }
      if (!hasMatch) {
        return false;
      }
    }

    for (Piece piece1 : board1.blackPieces) {
      boolean hasMatch = false;
      for (Piece piece2 : board2.blackPieces) {
        if (piece1.getName().equals(piece2.getName())) {
          if (Coordinate.compareCoords(piece1.getPos(), piece2.getPos())){
            hasMatch = true;
            break;
          }
        }
      }
      if (!hasMatch) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns teh number of a specific piece along a file.
   *
   * @param board
   * @param pieceName
   * @param file
   * @return
   */
  public static int countAlongFile(Cell[][] board, String pieceName, int startingRow, int file, boolean isWhite) {
    int numInFile = 0;
    if (file < 0 || file > 7 || startingRow < 0 || startingRow > 7) {
      return 0;
    }
    if (!isWhite) {
    for (int row = startingRow; row < 8; row++) {
      if (!board[row][file].PieceStatus.equals(Status.EMPTY)) {
        if (board[row][file].piece.getName().equals(pieceName) && board[row][file].piece.getColor() == isWhite) {
          numInFile++;
        }
      }
    }
    }
    else {
      for (int row = startingRow; row > 0; row--) {
        if (!board[row][file].PieceStatus.equals(Status.EMPTY)) {
          if (board[row][file].piece.getName().equals(pieceName) && board[row][file].piece.getColor() == isWhite) {
            numInFile++;
          }
        }
      }
    }
    return numInFile;
  }
}
