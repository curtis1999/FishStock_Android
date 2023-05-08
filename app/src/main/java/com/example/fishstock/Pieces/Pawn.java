package com.example.fishstock.Pieces;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.*;

public class Pawn implements Piece {
  boolean isWhite;
  public ArrayList<Move> legalMoves;
  public boolean firstMove;
  Coordinate fromPos;
  Coordinate curPos;
  Status stat;
  ArrayList<Piece> protectors = new ArrayList<>();
  ArrayList<Piece> attackers = new ArrayList<>();
  boolean isRevealChecker = false;
  boolean isPinned=false;

  public ArrayList<Coordinate> pinAve = new ArrayList<>();
  public boolean enPassantable;
  private Coordinate pinnerLoc;
  private Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve;
  ArrayList<Move>possibleMoves;

  public Pawn (Coordinate pos, boolean isWhite) {
    this.isWhite = isWhite;
    this.fromPos = pos;
    this.curPos = pos;
    this.firstMove = true;
    if (isWhite) {
      this.stat=Status.WHITE;
    }else {
      this.stat=Status.BLACK;
    }
  }

  public Pawn(Coordinate fromPos,Coordinate toPos, boolean isWhite, boolean isFirst) {
    this.isWhite=isWhite;
    this.fromPos=fromPos;
    this.curPos =toPos;
    this.firstMove=isFirst;
    if (isWhite) {
      this.stat=Status.WHITE;
    }else {
      this.stat=Status.BLACK;
    }
  }
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board){
    ArrayList<Move> legalMoves = new ArrayList<>();
    if (firstMove && isWhite) {
      Coordinate pos1 = new Coordinate(curPos.file,curPos.rank+1);
      if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mv1 = new Move(pos,pos1,"Pawn",false, this.isWhite);
        legalMoves.add(mv1);
        Coordinate pos2 = new Coordinate(curPos.file,curPos.rank+2);
        if (board[pos2.rank][pos2.file].PieceStatus==Status.EMPTY) {
          Move mv2 = new Move(pos,pos2,"Pawn",false,this.isWhite);
          legalMoves.add(mv2);
        }
      }
      if (curPos.file<7) {
        Coordinate pos3 = new Coordinate(curPos.file+1,curPos.rank+1);
        if (board[pos3.rank][pos3.file].PieceStatus==Status.BLACK) {
          Move m3 = new Move(pos,pos3,"Pawn",true,this.isWhite);
          m3.setCapture(board[pos3.rank][pos3.file].piece);
          if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
            m3.setCheck(m3.fromCoord, generateAvenue(m3.fromCoord, m3.toCoord));
          }
          legalMoves.add(m3);
        }else if (board[pos3.rank][pos3.file].PieceStatus==Status.WHITE){
          Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
          m3.setProtectionMove(pos3);
          legalMoves.add(m3);
        }else {
          Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
          m3.setCoverMove();
          legalMoves.add(m3);
        }
      }if (curPos.file>0) {
        Coordinate pos4 = new Coordinate(curPos.file-1,curPos.rank+1);
        if (board[pos4.rank][pos4.file].PieceStatus==Status.BLACK) {
          Move m4 = new Move(pos,pos4,"Pawn",true,this.isWhite);
          m4.setCapture(board[pos4.rank][pos4.file].piece);
          if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
            m4.setCheck(m4.fromCoord, generateAvenue(m4.fromCoord, m4.toCoord));
          }
          legalMoves.add(m4);
        }
        else if (board[pos4.rank][pos4.file].PieceStatus==Status.WHITE){
          Move m4 = new Move(pos,pos4,"Pawn",false,this.isWhite);
          m4.setProtectionMove(pos4);
          legalMoves.add(m4);
        }else {
          Move m4 = new Move(pos,pos4,"Pawn",false,this.isWhite);
          m4.setCoverMove();
          legalMoves.add(m4);
        }

      }
    } else if (!firstMove && isWhite) {
      //Can be promoted next Move
      if (pos.rank==6) {
        if(board[pos.rank+1][pos.file].PieceStatus==Status.EMPTY) {
          Coordinate pos1 = new Coordinate(pos.file,pos.rank+1);
          Move mv1 = new Move(pos,pos1,"Pawn",false, true);
          mv1.setPromotion();
          legalMoves.add(mv1);
        }
        if (pos.file<7) {
          Coordinate pos1 = new Coordinate(pos.file+1,pos.rank+1);
          if(board[pos1.rank][pos1.file].PieceStatus==Status.BLACK) {
            Move mv1 = new Move(pos,pos1,"Pawn",true, isWhite);
            mv1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mv1.setCheck(mv1.fromCoord, generateAvenue(mv1.fromCoord, mv1.toCoord));
            }
            mv1.setPromotion();
            legalMoves.add(mv1);
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.WHITE){
            Move m3 = new Move(pos,pos1,"Pawn",false,this.isWhite);
            m3.setProtectionMove(pos1);
            legalMoves.add(m3);
          }else {
            Move m3 = new Move(pos,pos1,"Pawn",false,this.isWhite);
            m3.setCoverMove();
            legalMoves.add(m3);
          }
        }
        if (pos.file>0) {
          Coordinate pos1 = new Coordinate(pos.file-1,pos.rank+1);
          if(board[pos1.rank][pos1.file].PieceStatus==Status.BLACK) {
            Move mv1 = new Move(pos,pos1,"Pawn",true,isWhite);
            mv1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mv1.setCheck(mv1.fromCoord, generateAvenue(mv1.fromCoord, mv1.toCoord));
            }
            mv1.setPromotion();
            legalMoves.add(mv1);
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.WHITE){
            Move m3 = new Move(pos,pos1,"Pawn",false, this.isWhite);
            m3.setProtectionMove(pos1);
            legalMoves.add(m3);
          }else {
            Move m3 = new Move(pos,pos1,"Pawn",false, this.isWhite);
            m3.setCoverMove();
            legalMoves.add(m3);
          }
        }
        //Have to check for enPassant.
      }else if (pos.rank==4) {
        Coordinate pos1 = new Coordinate(curPos.file,curPos.rank+1);
        if (board[pos1.rank][pos1.file].PieceStatus ==Status.EMPTY) {
          Move m1 = new Move(pos,pos1,"Pawn",false,this.isWhite);
          legalMoves.add(m1);
        }
        if (curPos.file<7) {
          Coordinate pos2 = new Coordinate(curPos.file+1,curPos.rank+1);
          if (board[pos2.rank][pos2.file].PieceStatus == Status.BLACK) {
            Move m2 = new Move(pos, pos2,"Pawn",true,this.isWhite);
            m2.setCapture(board[pos2.rank][pos2.file].piece);
            if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
              m2.setCheck(m2.fromCoord, generateAvenue(m2.fromCoord, m2.toCoord));
            }
            legalMoves.add(m2);
          }else if (board[pos2.rank][pos2.file].PieceStatus == Status.WHITE){
            Move m3 = new Move(pos,pos2,"Pawn",false,this.isWhite);
            m3.setProtectionMove(pos2);
            legalMoves.add(m3);
          }else {
            if (!(board[curPos.rank][curPos.file+1].isEmpty)
                && board[curPos.rank][curPos.file+1].piece.getName().equals("Pawn")
                && ((Pawn)board[curPos.rank][curPos.file+1].piece).enPassantable) {
              Move m3 = new Move(pos, pos2, "Pawn", true, this.isWhite);
              m3.setCapture(board[curPos.rank][curPos.file+1].piece);
              m3.setEnPassant();
              legalMoves.add(m3);
            } else {
              Move m3 = new Move(pos,pos2,"Pawn",false,this.isWhite);
              m3.setCoverMove();
              legalMoves.add(m3);
            }
          }
        }
        if (curPos.file > 0) {
          Coordinate pos3 = new Coordinate(curPos.file-1, curPos.rank+1);
          if (board[pos3.rank][pos3.file].PieceStatus ==Status.BLACK) {
            Move m3 = new Move(pos, pos3,"Pawn",true,this.isWhite);
            m3.setCapture(board[pos3.rank][pos3.file].piece);
            if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
              m3.setCheck(m3.fromCoord, generateAvenue(m3.fromCoord, m3.toCoord));
            }
            legalMoves.add(m3);
          }else if (board[pos3.rank][pos3.file].PieceStatus==Status.WHITE){
            Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
            m3.setProtectionMove(pos3);
            legalMoves.add(m3);
          }else {
            if (!(board[curPos.rank][curPos.file-1].isEmpty)
                && board[curPos.rank][curPos.file-1].piece.getName().equals("Pawn")
            && ((Pawn)board[curPos.rank][curPos.file-1].piece).enPassantable) {
              Move m3 = new Move(pos, pos3, "Pawn", true, this.isWhite);
              m3.setCapture(board[curPos.rank][curPos.file-1].piece);
              m3.setEnPassant();
              legalMoves.add(m3);
            } else {
              Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
              m3.setCoverMove();
              legalMoves.add(m3);
            }
          }
        }
      }
      //Not one away from promotion and no dont need to check for enPassant
      else {
        if (pos.rank<7) {
          Coordinate pos1 = new Coordinate(curPos.file,curPos.rank+1);
          if (board[pos1.rank][pos1.file].PieceStatus ==Status.EMPTY) {
            Move m1 = new Move(pos,pos1,"Pawn",false,this.isWhite);
            legalMoves.add(m1);
          }
          if (curPos.file<7) {
            Coordinate pos2 = new Coordinate(curPos.file+1,curPos.rank+1);
            if (board[pos2.rank][pos2.file].PieceStatus ==Status.BLACK) {
              Move m2 = new Move(pos, pos2,"Pawn",true,this.isWhite);
              m2.setCapture(board[pos2.rank][pos2.file].piece);
              if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
                m2.setCheck(m2.fromCoord, generateAvenue(m2.fromCoord, m2.toCoord));
              }

              legalMoves.add(m2);
            }else if (board[pos2.rank][pos2.file].PieceStatus==Status.WHITE){
              Move m3 = new Move(pos,pos2,"Pawn",false,this.isWhite);
              m3.setProtectionMove(pos2);
              legalMoves.add(m3);
            }else {
              Move m3 = new Move(pos,pos2,"Pawn",false,this.isWhite);
              m3.setCoverMove();
              legalMoves.add(m3);
            }
          }
          if (curPos.file > 0) {
            Coordinate pos3 = new Coordinate(curPos.file-1, curPos.rank+1);
            if (board[pos3.rank][pos3.file].PieceStatus ==Status.BLACK) {
              Move m3 = new Move(pos, pos3,"Pawn",true,this.isWhite);
              m3.setCapture(board[pos3.rank][pos3.file].piece);
              if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
                m3.setCheck(m3.fromCoord, generateAvenue(m3.fromCoord, m3.toCoord));
              }
              legalMoves.add(m3);
            }else if (board[pos3.rank][pos3.file].PieceStatus==Status.WHITE){
              Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
              m3.setProtectionMove(pos3);
              legalMoves.add(m3);
            }else {
              Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
              m3.setCoverMove();
              legalMoves.add(m3);
            }
          }
        }
      }
    } else if (firstMove && !isWhite) {
      Coordinate pos1 = new Coordinate(curPos.file,curPos.rank-1);
      if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move m1 = new Move(pos,pos1,"Pawn",false,this.isWhite);
        legalMoves.add(m1);
        Coordinate pos2 = new Coordinate(curPos.file,curPos.rank-2);
        if (board[pos2.rank][pos2.file].PieceStatus==Status.EMPTY) {
          Move m2 = new Move(pos,pos2,"Pawn",false,this.isWhite);
          legalMoves.add(m2);
        }
      }
      if (curPos.file<7) {
        Coordinate pos3 = new Coordinate(curPos.file+1,curPos.rank-1);
        if (board[pos3.rank][pos3.file].PieceStatus==Status.WHITE) {
          Move m3 = new Move(pos,pos3,"Pawn",true,this.isWhite);
          m3.setCapture(board[pos3.rank][pos3.file].piece);
          if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
            m3.setCheck(m3.fromCoord, generateAvenue(m3.fromCoord, m3.toCoord));
          }
          legalMoves.add(m3);
        }else if (board[pos3.rank][pos3.file].PieceStatus==Status.BLACK){
          Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
          m3.setProtectionMove(pos3);
          legalMoves.add(m3);
        }else {
          Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
          m3.setCoverMove();
          legalMoves.add(m3);
        }
      }
      if (curPos.file>0) {
        Coordinate pos4 = new Coordinate(curPos.file-1,curPos.rank-1);
        if (board[pos4.rank][pos4.file].PieceStatus==Status.WHITE) {
          Move m4 = new Move(pos,pos4,"Pawn",true,this.isWhite);
          m4.setCapture(board[pos4.rank][pos4.file].piece);
          if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
            m4.setCheck(m4.fromCoord, generateAvenue(m4.fromCoord, m4.toCoord));
          }
          legalMoves.add(m4);
        }else if (board[pos4.rank][pos4.file].PieceStatus==Status.BLACK){
          Move m3 = new Move(pos,pos4,"Pawn",false,this.isWhite);
          m3.setProtectionMove(pos4);
          legalMoves.add(m3);
        }else {
          Move m3 = new Move(pos,pos4,"Pawn",false,this.isWhite);
          m3.setCoverMove();
          legalMoves.add(m3);
        }
      }
    } else if (!firstMove && !isWhite) {
      //Black could promote next turn
      if (pos.rank==1) {
        if(board[pos.rank-1][pos.file].PieceStatus==Status.EMPTY) {
          Coordinate pos1 = new Coordinate(pos.file,pos.rank-1);
          Move mv1 = new Move(pos,pos1,"Pawn",false, false);
          mv1.setPromotion();
          legalMoves.add(mv1);
        }
        if (pos.file<7) {
          Coordinate pos1 = new Coordinate(pos.file+1,pos.rank-1);
          if(board[pos1.rank][pos1.file].PieceStatus==Status.WHITE) {
            Move mv1 = new Move(pos,pos1,"Pawn",true, false);
            mv1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mv1.setCheck(mv1.fromCoord, generateAvenue(mv1.fromCoord, mv1.toCoord));
            }
            mv1.setPromotion();
            legalMoves.add(mv1);
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.BLACK){
            Move m3 = new Move(pos,pos1,"Pawn",false,this.isWhite);
            m3.setProtectionMove(pos1);
            legalMoves.add(m3);
          }else {
            Move m3 = new Move(pos,pos1,"Pawn",false,this.isWhite);
            m3.setCoverMove();
            legalMoves.add(m3);
          }
        }
        if (pos.file>0) {
          Coordinate pos1 = new Coordinate(pos.file-1,pos.rank-1);
          if(board[pos1.rank][pos1.file].PieceStatus==Status.WHITE) {
            Move mv1 = new Move(pos,pos1,"Pawn",true,isWhite);
            mv1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mv1.setCheck(mv1.fromCoord, generateAvenue(mv1.fromCoord, mv1.toCoord));
            }
            mv1.setPromotion();
            legalMoves.add(mv1);
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.BLACK){
            Move m3 = new Move(pos,pos1,"Pawn",false, this.isWhite);
            m3.setProtectionMove(pos1);
            legalMoves.add(m3);
          }else {
            Move m3 = new Move(pos,pos1,"Pawn",false, this.isWhite);
            m3.setCoverMove();
            legalMoves.add(m3);
          }
        }
        //Need to check for enPassants
      }else if (pos.rank==3) {
        Coordinate pos1 = new Coordinate(curPos.file,curPos.rank-1);
        if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
          Move m1 = new Move(pos,pos1,"Pawn",false,this.isWhite);
          legalMoves.add(m1);
        }
        if (curPos.file<7) {
          Coordinate pos2 = new Coordinate(curPos.file + 1, curPos.rank - 1);
          if (board[pos2.rank][pos2.file].PieceStatus == Status.WHITE) {
            Move m2 = new Move(pos, pos2, "Pawn", true, this.isWhite);
            m2.setCapture(board[pos2.rank][pos2.file].piece);
            if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
              m2.setCheck(m2.fromCoord, generateAvenue(m2.fromCoord, m2.toCoord));
            }
            legalMoves.add(m2);
          } else if (board[pos2.rank][pos2.file].PieceStatus == Status.BLACK) {
            Move m2 = new Move(pos, pos2, "Pawn", false, this.isWhite);
            m2.setProtectionMove(pos2);
            legalMoves.add(m2);
            //CoverMove or EnPassant.
          } else {
            if (board[curPos.rank][curPos.file + 1].PieceStatus == Status.WHITE
                && board[curPos.rank][curPos.file + 1].piece.getName().equals("Pawn")
                && ((Pawn) board[curPos.rank][curPos.file + 1].piece).enPassantable) {
              Move m2 = new Move(pos, pos2, "Pawn", true, this.isWhite);
              m2.setCapture(board[curPos.rank][curPos.file + 1].piece);
              m2.setEnPassant();
              legalMoves.add(m2);
            } else {
              Move m2 = new Move(pos, pos2, "Pawn", false, this.isWhite);
              m2.setCoverMove();
              legalMoves.add(m2);
            }
          }
        }
          if (curPos.file>0) {
            Coordinate pos3 = new Coordinate(curPos.file-1, curPos.rank-1);
            if (board[pos3.rank][pos3.file].PieceStatus ==Status.WHITE) {
              Move m3 = new Move(pos, pos3,"Pawn",true,this.isWhite);
              m3.setCapture(board[pos3.rank][pos3.file].piece);
              if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
                m3.setCheck(m3.fromCoord, generateAvenue(m3.fromCoord, m3.toCoord));
              }
              legalMoves.add(m3);
            }else if (board[pos3.rank][pos3.file].PieceStatus==Status.BLACK){
              Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
              m3.setProtectionMove(pos3);
              legalMoves.add(m3);
              //Cover Move or EnPassant.
            }else {
              if (board[curPos.rank][curPos.file - 1].PieceStatus == Status.WHITE
                  && board[curPos.rank][curPos.file-1].piece.getName().equals("Pawn")
                  &&((Pawn)board[curPos.rank][curPos.file-1].piece).enPassantable){
                Move m3 = new Move(pos,pos3,"Pawn",true,this.isWhite);
                m3.setCapture(board[curPos.rank][curPos.file-1].piece);
                m3.setEnPassant();
                legalMoves.add(m3);
              } else {
                Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
                m3.setCoverMove();
                legalMoves.add(m3);
              }
            }
          }
        }
      else {
        Coordinate pos1 = new Coordinate(curPos.file,curPos.rank-1);
        if (pos1.rank >= 0) {
          if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            Move m1 = new Move(pos,pos1,"Pawn",false,this.isWhite);
            legalMoves.add(m1);
          }
          if (curPos.file<7) {
            Coordinate pos2 = new Coordinate(curPos.file+1,curPos.rank-1);
            if (board[pos2.rank][pos2.file].PieceStatus ==Status.WHITE) {
              Move m2 = new Move(pos, pos2,"Pawn",true,this.isWhite);
              m2.setCapture(board[pos2.rank][pos2.file].piece);
              if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
                m2.setCheck(m2.fromCoord, generateAvenue(m2.fromCoord, m2.toCoord));
              }
              legalMoves.add(m2);
            }else if (board[pos2.rank][pos2.file].PieceStatus==Status.BLACK){
              Move m3 = new Move(pos,pos2,"Pawn",false,this.isWhite);
              m3.setProtectionMove(pos2);
              legalMoves.add(m3);
            }else {
              Move m3 = new Move(pos,pos2,"Pawn",false,this.isWhite);
              m3.setCoverMove();
              legalMoves.add(m3);
            }
          }
          if (curPos.file>0) {
            Coordinate pos3 = new Coordinate(curPos.file-1, curPos.rank-1);
            if (board[pos3.rank][pos3.file].PieceStatus ==Status.WHITE) {
              Move m3 = new Move(pos, pos3,"Pawn",true,this.isWhite);
              m3.setCapture(board[pos3.rank][pos3.file].piece);
              if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
                m3.setCheck(m3.fromCoord, generateAvenue(m3.fromCoord, m3.toCoord));
              }
              legalMoves.add(m3);
            }else if (board[pos3.rank][pos3.file].PieceStatus==Status.BLACK){
              Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
              m3.setProtectionMove(pos3);
              legalMoves.add(m3);
            }else {
              Move m3 = new Move(pos,pos3,"Pawn",false,this.isWhite);
              m3.setCoverMove();
              legalMoves.add(m3);
            }
          }
        }
      }
    }
    //TODO: CONFIRM IF THIS IS NECESSARY

    this.possibleMoves=legalMoves;
    return legalMoves;
  }

  public String getName() {
    return "Pawn";
  }
  public Coordinate getPos() {
    return this.curPos;
  }
  public boolean getColor() {
    return this.isWhite;
  }
  public void setPos(Coordinate coord) {
    this.curPos=coord;
  }
  public void growUp() {
    this.firstMove=false;
  }
  public void addAttacker(Piece p) {
    this.attackers.add(p);
  }
  public void addProtector(Piece p) {
    this.protectors.add(p);
  }
  @Override
  public ArrayList<Piece> getProtectors() {
    return this.protectors;
  }
  @Override
  public ArrayList<Piece> getAttackers() {
    return this.attackers;
  }
  public void setRevealChecker() {
    this.isRevealChecker=true;
  }
  public boolean getPin() {
    return this.isPinned;
  }
  public void setEnPassantable() {
    this.enPassantable=true;
  }
  public void unEnPassantable() {
    this.enPassantable=false;
  }

  public boolean isRevealChecker() { return this.isRevealChecker;}
  public boolean isPinned() {return this.isPinned;}


  public ArrayList<Coordinate> getPinAvenue() {
    return this.pinAve;
  }
  public void unPin() {
    this.isPinned=false;
    this.pinAve = null;
  }
  @Override
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc) {
    this.isPinned=true;
    this.pinAve=pinAve;
    this.pinnerLoc = pinnerLoc;
  }
  public Coordinate getPinnerLoc() {
    return this.pinnerLoc;
  }
  @Override
  public Coordinate getCheckerLoc() {
    // TODO Auto-generated method stub
    return null;
  }
  @Override
  public ArrayList<Coordinate> generateAvenue(Coordinate c1, Coordinate c2) {
    ArrayList<Coordinate> ave = new ArrayList<>();
    ave.add(c1); ave.add(c2);
    return ave;
  }
  @Override
  public void setRevealChecker(ArrayList<Coordinate> revealAve,Coordinate checkerLoc) {
    this.isRevealChecker=true;
    this.revealCheckerLoc = checkerLoc;
    this.revealAve = revealAve;
  }
  public Coordinate getRevealCheckerLoc() {
    return this.revealCheckerLoc;
  }
  @Override
  public ArrayList<Coordinate> getRevealAve() {
    return this.revealAve;
  }
  public void unReveal() {
    this.isRevealChecker=false;
    this.revealAve =null;
    this.revealCheckerLoc = new Coordinate(-1,-1);
  }
  //Used for undo.
  public void unGrow() {
    this.firstMove=true;
  }
  @Override
  public ArrayList<Move> getPossibleMoves() {
    return this.possibleMoves;
  }
  @Override
  public void setPossibleMoves(ArrayList<Move> potentialMoves_2) {
    this.possibleMoves = potentialMoves_2;
  }
  @Override
  public void reset() {
    this.attackers = new ArrayList<>();
    this.protectors = new ArrayList<>();
    this.isPinned = false;
    this.pinAve = null;
    this.pinnerLoc = new Coordinate(-1, -1);
    this.isRevealChecker = false;
    this.revealAve = null;
    this.revealCheckerLoc = new Coordinate(-1, -1);
  }
  public void setProtectors(ArrayList<Piece> protectors){
    this.protectors = protectors;
  }
  public void setAttackers(ArrayList<Piece> attackers) {
    this.attackers = attackers;
  }
  @Override
  public Piece copyPiece() {
    Pawn copyPiece = new Pawn(this.curPos, this.isWhite);
    copyPiece.setPossibleMoves(this.possibleMoves);
    copyPiece.setProtectors(this.protectors);
    copyPiece.setAttackers(this.attackers);
    copyPiece.isRevealChecker = this.isRevealChecker;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.isPinned = this.isPinned;
    copyPiece.pinnerLoc = this.pinnerLoc.copyCoordinate();
    copyPiece.pinAve = this.pinAve;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.revealAve = this.revealAve;
    return copyPiece;
  }
  public char getSymbol() {
    return 'P';
  }
}
