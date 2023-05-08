package com.example.fishstock;

import com.example.fishstock.Pieces.*;

import java.util.ArrayList;

public class Move implements Comparable<Move>{
  double eval=0;
  public Coordinate fromCoord;
  public Coordinate toCoord;

  public Piece piece;
  public boolean isCapture;
  public boolean isPromotion;
  public boolean coverMove; //Not a legal Move but useful for evaluation
  public boolean protectionMove; //Pawn protects a square without being able to go there
  public boolean isCheck;
  public Coordinate checkerLoc;
  public ArrayList<Coordinate> checkingAve = new ArrayList<>();

  public boolean isDoubleCheck;
  public boolean isPin;
  public boolean isReveal;
  public boolean isCastle;
  public boolean isEnPassant;
  public Piece capturablePiece;
  public Coordinate pinLoc; //Location of the pinned piece.
  public ArrayList<Coordinate> pinAvenue= new ArrayList<>();
  public Coordinate revealLoc;
  public ArrayList<Coordinate> revealAvenue = new ArrayList<>();
  public Piece promotionPiece;
  public Coordinate protectionLocation;
  //No input then the Move is set to castle.

  public Move(boolean nullMove) {
    this.fromCoord = new Coordinate(-1,-1);
    this.toCoord = new Coordinate(-1,-1);
    this.piece = null;
    this.isCapture =false;
    this.isPromotion=false;
    this.coverMove=false;
    this.protectionMove=false;
    this.isCheck =false;
    this.isDoubleCheck = false;
    this.isPin=false;
    this.isCastle=false;
    this.isEnPassant =false;
    this.pinLoc = new Coordinate(-1,-1);

  }

  //Copy constructor if there is a capture
  public Move(Coordinate fromCrd, Coordinate crd, String name, boolean isCapture, boolean isWhite) {
    this.fromCoord=fromCrd;
    this.toCoord = crd;
    this.isCapture=isCapture; //Not a capture by default
    if (name.equals("Pawn")) {
      this.piece= new Pawn(fromCrd,crd, isWhite,true);
      ((Pawn)this.piece).growUp();
    } else if (name.equals("Rook")) {
      this.piece = new Rook(fromCrd,crd, isWhite);
    }else if (name.equals("Knight")) {
      this.piece = new Knight(fromCrd,crd, isWhite);
    }else if (name.equals("Bishop")) {
      this.piece = new Bishop(fromCrd,crd, isWhite);
    }else if (name.equals("Queen")) {
      this.piece = new Queen(fromCrd,crd, isWhite);
    }else if (name.equals("King")) {
      this.piece = new King(crd,isWhite);
    }else {
      this.piece = null;
    }
    this.isPromotion=false;
    this.coverMove=false;
    this.protectionMove=false;
    this.isCastle=false;
    this.isCheck = false;
    this.isDoubleCheck = false;
    this.isPin=false;
    this.isEnPassant =false;
    this.capturablePiece=null;
  }

  public String printCoord() {
    char fromFile = (char)('h'-fromCoord.file);
    int fromRank = fromCoord.rank+1;
    char file = (char)('h' - toCoord.file);
    int rank = toCoord.rank+1;
    if (!(this.piece ==null)) {
      return piece.getName() + " " + fromFile + " " + fromRank + " " + file + " "+ rank;
    } else {
      return "error";
    }
  }
  //TODO: Add extra details to the string.
  public String printMove() {
    return "";
  }

  public boolean equals(Move mv) {
    if(mv.fromCoord.rank==this.fromCoord.rank && mv.fromCoord.file==this.fromCoord.file && mv.toCoord.rank==this.toCoord.rank && mv.toCoord.file==this.toCoord.file && this.piece.getName().equals(mv.piece.getName())) {
      return true;
    }else {
      return false;
    }
  }
  public void setCapture(Piece capPiece) {
    this.isCapture=true;
    this.capturablePiece=capPiece;
  }
  public void setPromotion() {
    this.isPromotion=true;
  }
  public void setPromotion(Piece p) {
    this.isPromotion=true;
    this.promotionPiece=p;
  }
  public Piece getPromotionPiece() {
    return this.promotionPiece;
  }
  public void setCoverMove() {
    this.coverMove=true;
  }
  public void setProtectionMove(Coordinate protectionLocation) {
    this.protectionMove=true;
    this.protectionLocation = protectionLocation;

  }
  public boolean getCover() {
    return this.coverMove;
  }
  public boolean isCheck() {
    return this.isCheck;
  }
  public void setCheck(Coordinate checkerLoc, ArrayList<Coordinate> checkingAve) {
    this.isCheck = true;
    this.checkerLoc = checkerLoc;
    this.checkingAve = checkingAve;
  }
  public void setCheck() {
    this.isCheck = true;
  }
  public void setDoubleCheck() { this.isDoubleCheck = true; }
  public void setPin(Coordinate pinLoc, ArrayList<Coordinate> pinAve) {
    this.isPin = true;
    this.pinLoc = pinLoc; //Location of the pinned piece
    this.pinAvenue=pinAve;
  }
  public void setCastle() {
    this.isCastle = true;
  }
  public void setReveal(Coordinate revealLoc, ArrayList<Coordinate> revealAve) {
    this.isReveal = true;
    this.revealLoc = revealLoc;
    this.revealAvenue = revealAve;
  }
  public void unPin( ) {
    this.isPin= false;
    this.pinLoc = new Coordinate(-1,-1);
  }
  public void unReveal() {
    this.isReveal=false;
    this.revealLoc = new Coordinate(-1,-1);
  }
  public Coordinate getPinLoc() {
    return this.pinLoc;
  }
  public Coordinate getRevealLoc() {
    return this.revealLoc;
  }
  public ArrayList<Coordinate> getPinAvenue(){
    return this.pinAvenue;
  }
  public ArrayList<Coordinate> getRevealAvenue(){
    return this.revealAvenue;
  }
  public void Castle() {
    this.isCastle=true;
  }
  public void setEnPassant() {
    this.isEnPassant = true;
  }
  public void setCapturable(Piece cap) {
    this.capturablePiece=cap;
  }
  public Piece getCapturable() {
    return this.capturablePiece;
  }
  public void setEval(double d) {
    this.eval=d;
  }

  @Override
  public int compareTo(Move o) {
    return 0;
  }

}
