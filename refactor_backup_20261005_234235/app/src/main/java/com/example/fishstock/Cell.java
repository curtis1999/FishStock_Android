package com.example.fishstock;
import java.io.Serializable;
import java.util.*;
import com.example.fishstock.Pieces.*;

public class Cell implements Serializable {
  char Symbol;
  boolean isLight;
  public boolean isEmpty;
  boolean isPawn;
  boolean isKnight;
  boolean isBishop;
  boolean isRook;
  boolean isQueen;
  boolean isKing;
  boolean isWhite;
  public Status PieceStatus;
  boolean xRay;
  public Piece piece;
  public ArrayList<Piece> whiteAttackers=new ArrayList<Piece>();
  public ArrayList<Piece> blackAttackers=new ArrayList<Piece>();
  public Cell(boolean colour) {
    this.Symbol='-';
    this.isLight =colour;
    this.isEmpty=true;
    this.isPawn=false;
    this.isKnight=false;
    this.isBishop=false;
    this.isRook=false;
    this.isQueen=false;
    this.isKing=false;
    this.isWhite=false;  //Default to false which indicates that a white piece is on the square.
    this.PieceStatus=Status.EMPTY;
    this.xRay=false;
  }
  public void setStatus (Status stat) {
    this.PieceStatus = stat;
  }
  public void setLight(boolean c) {
    this.isLight = c;
  }
  public void empty() {
    this.Symbol='-';
    this.isEmpty=true;
    this.isPawn=false;
    this.isKnight=false;
    this.isBishop=false;
    this.isRook=false;
    this.isQueen=false;
    this.isKing=false;
    this.isWhite=false;
    this.PieceStatus = Status.EMPTY;
    this.piece=null;
  }
  public void putPiece(Piece p) {
    if (p.getColor()) {
      this.PieceStatus = Status.WHITE;
    }else {
      this.PieceStatus = Status.BLACK;
    }
    if (p.getName().contains("Pawn")) {
      putPawn(p.getColor(),p);
    }else if (p.getName().contains("Knight")) {
      putKnight(p.getColor(),p);
      this.isPawn = false;
    }else if (p.getName().contains("Bishop")) {
      putBishop(p.getColor(),p);
      this.isPawn = false;
    }else if (p.getName().contains("Rook")) {
      putRook(p.getColor(),p);
      this.isPawn = false;
    }else if (p.getName().contains("Queen")) {
      putQueen(p.getColor(),p);
      this.isPawn = false;
    }else if (p.getName().contains("King")) {
      putKing(p.getColor(),p);
      this.isPawn = false;
    }
  }
  public void putPawn(boolean pC, Piece pawn) {
    this.isEmpty=false;
    this.isPawn=true;
    this.Symbol='P';
    this.isWhite=pC;
    this.piece=pawn;
  }

  public void putKnight(boolean pC, Piece knight) {
    this.isEmpty=false;
    this.isKnight=true;
    this.Symbol='N';
    this.isWhite=pC;
    this.piece=knight;
  }
  public void putBishop(boolean pC, Piece bishop) {
    this.isEmpty=false;
    this.isBishop=true;
    this.Symbol='B';
    this.isWhite=pC;
    this.piece=bishop;
  }
  public void putRook(boolean pC, Piece rook) {
    this.isEmpty=false;
    this.isRook=true;
    this.Symbol='R';
    this.isWhite = pC;
    this.piece =rook;
    if (rook.getColor()) {
      this.PieceStatus = Status.WHITE;
    }else {
      this.PieceStatus = Status.BLACK;
    }
  }
  public void putQueen(boolean pC, Piece queen) {
    this.isEmpty=false;
    this.isQueen=true;
    this.Symbol='Q';
    this.isWhite=pC;
    this.piece=queen;
  }
  public void putKing(boolean pC, Piece king) {
    this.isEmpty=false;
    this.isKing=true;
    this.Symbol='K';
    this.isWhite=pC;
    this.piece=king;
  }
  public void addAttacker(Piece p) {
    if (p.getColor()) {
      this.whiteAttackers.add(p);
    }else {
      this.blackAttackers.add(p);
    }
  }
  /*
  char Symbol;
  boolean colour;
  boolean isEmpty;
  boolean isPawn;
  boolean isKnight;
  boolean isBishop;
  boolean isRook;
  boolean isQueen;
  boolean isKing;
  boolean isWhite;
  Status PieceStatus;
  Piece piece;
  ArrayList<Piece> whiteAttackers=new ArrayList<Piece>();
  ArrayList<Piece> blackAttackers=new ArrayList<Piece>();
  */
  @SuppressWarnings("unchecked")
  public static Cell copyCell(Cell c) {
    Cell copyCell =new Cell(c.isLight);
    copyCell.isEmpty=c.isEmpty;
    copyCell.isPawn=c.isPawn;
    copyCell.isKnight=c.isKnight;
    copyCell.isBishop=c.isBishop;
    copyCell.isRook=c.isRook;
    copyCell.isQueen=c.isQueen;
    copyCell.isKing=c.isKing;
    copyCell.isWhite=c.isWhite;
    copyCell.PieceStatus=c.PieceStatus;
    if (c.piece != null) {
      copyCell.piece = c.piece.copyPiece();
    } else {
      copyCell.piece = null;//TODO: ENSURE THIS DOESNT NEED A DEEP COPY
    }
    copyCell.whiteAttackers=(ArrayList<Piece>) c.whiteAttackers.clone();
    copyCell.blackAttackers=(ArrayList<Piece>) c.blackAttackers.clone();
    copyCell.isEmpty=c.isEmpty;
    return copyCell;
  }
  public static boolean isCentralSquare(Coordinate crd) {
    boolean isCentral=false;
    if (crd.file==3&&crd.rank==3) {
      isCentral=true;
    }else if (crd.file==3&&crd.rank==4) {
      isCentral=true;
    }
    else if (crd.file==4&&crd.rank==3) {
      isCentral=true;
    }
    else if (crd.file==4&&crd.rank==4) {
      isCentral=true;
    }
    return isCentral;
  }
  public void setXRay() {
    this.xRay=true;
  }
}
