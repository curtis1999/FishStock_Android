package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Bishop implements Piece {
  Coordinate fromPos;
  Coordinate curPos;
  boolean isWhite;
  ArrayList<Move> possibleMoves = new ArrayList<>();
  Status stat;
  ArrayList<Piece> protectors = new ArrayList<>();
  ArrayList<Piece> attackers = new ArrayList<>();
  boolean isRevealChecker = false;
  boolean isPinned=false;
  Coordinate pinnerLoc;
  ArrayList<Coordinate> pinAve = new ArrayList<>();
  private Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve;

  public Bishop (Coordinate curPos, boolean isWhite) {
    this.fromPos= curPos;
    this.curPos=curPos;
    this.isWhite=isWhite;
    if (isWhite) {
      this.stat = Status.WHITE;
    }else {
      this.stat = Status.BLACK;
    }
  }
  //TODO: REMOVE THIS CONSRUCTOR.
  public Bishop(Coordinate fromPos,Coordinate coord, boolean isWhite) {
    this.fromPos= coord;
    this.curPos=coord;
    this.isWhite=isWhite;
    if (isWhite) {
      this.stat = Status.WHITE;
    }else {
      this.stat = Status.BLACK;
    }
  }
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<Move>();
    //Towards a8
    if (pos.file<7 && pos.rank<7) {
      boolean xRay = false;
      Coordinate pinLoc = new Coordinate(-1,-1);
      Move mv1 = new Move(false);
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank+1);
      while (pos1.file<=7 && pos1.rank<=7) {
        if (!xRay) {
          if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            mv1 = new Move(pos,pos1,"Bishop",false,this.isWhite);
            legalMoves.add(mv1);
            pos1 = new Coordinate(pos1.file+1,pos1.rank+1);
            continue;
          }else if (board[pos1.rank][pos1.file].PieceStatus!=this.stat) {
            mv1 = new Move(pos,pos1,"Bishop",true, this.isWhite);
            mv1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mv1.setCheck(mv1.fromCoord, generateAvenue(mv1.fromCoord, mv1.toCoord));
            }
            legalMoves.add(mv1);
            xRay=true;
            pinLoc = new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file+1,pos1.rank+1);
            continue;
          }else {
            mv1 = new Move(pos,pos1,"Bishop",false, this.isWhite);
            mv1.setProtectionMove(pos1);
            legalMoves.add(mv1);
            break;
          }
        }				//x-ray "moves".
        else {
          if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file+1,pos1.rank+1);
            continue;
          } else if (board[pos1.rank][pos1.file].PieceStatus != this.stat) {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) { //Pins the adversary piece
              ArrayList<Coordinate> pinAvenue = generateAvenue(pos,pos1);
              //board[pinLoc.rank][pinLoc.file].piece.setPin(pinAvenue,pos);
              mv1.setPin(pinLoc,pinAvenue);
              break;
            }else {
              break;
            }
          }else {	//TODO: ADD FOR REVEAL CHECKS
            break;
          }
        }
      }
    }

    //Towards a 1
    if (pos.file<7 && pos.rank>0) {
      Coordinate pos2 = new Coordinate(pos.file+1,pos.rank-1);
      Coordinate pinLoc = new Coordinate(-1,-1);
      Move mv2 = new Move(false);
      boolean xRay = false;
      while (pos2.file<=7 && pos2.rank>=0) {
        if (!xRay) {
          if (board[pos2.rank][pos2.file].PieceStatus==Status.EMPTY) {
            mv2 = new Move(pos,pos2,"Bishop",false,this.isWhite);
            legalMoves.add(mv2);
            pos2= new Coordinate(pos2.file+1,pos2.rank-1);
            continue;
            //If there is an enemy piece in the way.
          }else if (board[pos2.rank][pos2.file].PieceStatus!=this.stat) {
            mv2 = new Move(pos,pos2,"Bishop",true, this.isWhite);
            mv2.setCapture(board[pos2.rank][pos2.file].piece);
            if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
              mv2.setCheck(mv2.fromCoord, generateAvenue(mv2.fromCoord, mv2.toCoord));
            }
            legalMoves.add(mv2);
            xRay=true;
            pinLoc = new Coordinate(pos2.file, pos2.rank);
            pos2 = new Coordinate(pos2.file+1,pos2.rank-1);
            continue;
          }else {
            mv2 = new Move(pos,pos2,"Bishop",false, this.isWhite);
            mv2.setProtectionMove(pos2);
            legalMoves.add(mv2);
            break;
          }
        } else {
          if (board[pos2.rank][pos2.file].PieceStatus==Status.EMPTY) {
            pos2= new Coordinate(pos2.file+1,pos2.rank-1);
            continue;
            //If there is an enemy piece in the way.
          } else if (board[pos2.rank][pos2.file].PieceStatus!=this.stat) {
            if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAvenue = generateAvenue(pos,pos2);
              //board[pinLoc.rank][pinLoc.file].piece.setPin(pinAvenue,pos);
              mv2.setPin(pinLoc,pinAvenue);
              break;
            }else {
              break;
            }
          }else {
            break;
          }
        }
      }
    }
    //Towards h8
    if (pos.file>0 && pos.rank<7) {
      Coordinate pos3 = new Coordinate(pos.file-1,pos.rank+1);
      Coordinate pinLoc = new Coordinate(-1,-1);
      Move mv3 = new Move(false);
      boolean xRay=false;
      while (pos3.file>=0 && pos3.rank<=7) {
        if (!xRay) {
          if (board[pos3.rank][pos3.file].PieceStatus==Status.EMPTY) {
            mv3 = new Move(pos,pos3,"Bishop",false,this.isWhite);
            legalMoves.add(mv3);
            pos3 = new Coordinate(pos3.file-1, pos3.rank+1);
            continue;
            //If there is an enemy piece in the way.
          }else if (board[pos3.rank][pos3.file].PieceStatus!=this.stat) {
            mv3 = new Move(pos,pos3,"Bishop",true, this.isWhite);
            mv3.setCapture(board[pos3.rank][pos3.file].piece);
            if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
              mv3.setCheck(mv3.fromCoord, generateAvenue(mv3.fromCoord, mv3.toCoord));
            }
            legalMoves.add(mv3);
            xRay=true;
            pinLoc = new Coordinate(pos3.file,pos3.rank);
            pos3 = new Coordinate(pos3.file-1, pos3.rank+1);
            continue;
            //Your own piece in the way
          }else {
            mv3 = new Move(pos,pos3,"Bishop",false, this.isWhite);
            mv3.setProtectionMove(pos3);
            legalMoves.add(mv3);
            break;
          }
        }else {
          if (board[pos3.rank][pos3.file].PieceStatus==Status.EMPTY) {
            pos3 = new Coordinate(pos3.file-1, pos3.rank+1);
            continue;
          }else if (board[pos3.rank][pos3.file].PieceStatus!=this.stat) {
            if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAvenue = generateAvenue(pos,pos3);
              //board[pinLoc.rank][pinLoc.file].piece.setPin(pinAvenue,pos);
              mv3.setPin(pinLoc, pinAvenue);
              break;
            }else {
              break;
            }
          }else {
            break;
          }
        }
      }
    }
    //Towards h1
    if (pos.file>0 && pos.rank>0) {
      Coordinate pos4 = new Coordinate(pos.file-1,pos.rank-1);
      Coordinate pinLoc = new Coordinate(-1,-1);
      Move mv4 = new Move(false);
      boolean xRay=false;
      while (pos4.file>=0 && pos4.rank>=0) {
        if (!xRay) {
          if (board[pos4.rank][pos4.file].PieceStatus==Status.EMPTY) {
            mv4 = new Move(pos,pos4,"Bishop",false,this.isWhite);
            legalMoves.add(mv4);
            pos4= new Coordinate(pos4.file-1, pos4.rank-1);
            continue;
            //If there is an enemy piece in the way.
          }else if (board[pos4.rank][pos4.file].PieceStatus!=this.stat) {
            mv4 = new Move(pos,pos4,"Bishop",true, this.isWhite);
            mv4.setCapture(board[pos4.rank][pos4.file].piece);
            if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
              mv4.setCheck(mv4.fromCoord, generateAvenue(mv4.fromCoord, mv4.toCoord));
            }
            legalMoves.add(mv4);
            xRay = true;
            pinLoc = new Coordinate(pos4.file,pos4.rank);
            pos4= new Coordinate(pos4.file-1, pos4.rank-1);
            continue;
            //Your own piece in the way
          }else {
            mv4= new Move(pos,pos4,"Bishop",false, this.isWhite);
            mv4.setProtectionMove(pos4);
            legalMoves.add(mv4);
            break;
          }
        }else {
          if (board[pos4.rank][pos4.file].PieceStatus==Status.EMPTY) {
            pos4 = new Coordinate(pos4.file-1, pos4.rank-1);
            continue;
          }else if (board[pos4.rank][pos4.file].PieceStatus!=this.stat) {
            if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAvenue = generateAvenue(pos,pos4);
              //board[pinLoc.rank][pinLoc.file].piece.setPin(pinAvenue,pos);
              mv4.setPin(pinLoc,pinAvenue);
              break;
            }else {
              break;
            }
          }else {
            break;
          }
        }
      }

    }
    if (legalMoves.size()==0) {
      legalMoves.add(new Move(false));
    }
    this.possibleMoves=legalMoves;
    return legalMoves;
  }


  public ArrayList<Coordinate> generateAvenue(Coordinate pinnerLoc, Coordinate kingLoc){
    ArrayList<Coordinate> pinAvenue = new ArrayList<>();
    //pinAvenue.add(pinnerLoc);
    if ((pinnerLoc.file + pinnerLoc.rank)==(kingLoc.file+kingLoc.rank)) {
      if (pinnerLoc.rank>kingLoc.rank) {
        Coordinate temp = new Coordinate(pinnerLoc.file+1, pinnerLoc.rank-1);
        while (temp.rank>kingLoc.rank) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file+1,temp.rank-1);
        }
      }else {
        Coordinate temp = new Coordinate(pinnerLoc.file-1, pinnerLoc.rank+1);
        while (temp.rank<kingLoc.rank) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file-1,temp.rank+1);
        }
      }
    }else {
      if (pinnerLoc.rank>kingLoc.rank) {
        Coordinate temp = new Coordinate(pinnerLoc.file-1, pinnerLoc.rank-1);
        while (temp.rank>kingLoc.rank) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file-1,temp.rank-1);
        }
      }else {
        Coordinate temp = new Coordinate(pinnerLoc.file+1, pinnerLoc.rank+1);
        while (temp.rank<kingLoc.rank) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file+1,temp.rank+1);
        }
      }
    }
    return pinAvenue;
  }
  @Override
  public String getName() {
    return "Bishop";
  }

  public Coordinate getPos() {
    return this.curPos;
  }

  public boolean getColor() {
    // TODO Auto-generated method stub
    return this.isWhite;
  }

  public void setPos(Coordinate coord) {
    this.curPos=coord;
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
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc) {
    this.isPinned = true;
    this.pinAve=pinAve;
    this.pinnerLoc = pinnerLoc;
  }

  public boolean getPin() {
    return this.isPinned;
  }
  public ArrayList<Coordinate> getPinAvenue() {
    return this.pinAve;
  }
  public void unPin() {
    this.isPinned=false;
    this.pinAve = null;
  }
  @Override
  public Coordinate getPinnerLoc() {
    return this.pinnerLoc;
  }
  @Override
  public Coordinate getCheckerLoc() {
    // TODO Auto-generated method stub
    return null;
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
    Bishop copyPiece = new Bishop(this.curPos, this.curPos, this.isWhite);
    copyPiece.setPossibleMoves(this.possibleMoves);
    copyPiece.setProtectors(this.protectors);
    copyPiece.setAttackers(this.attackers);
    copyPiece.isRevealChecker = this.isRevealChecker;
    copyPiece.isPinned = this.isPinned;
    copyPiece.pinnerLoc = this.pinnerLoc;
    copyPiece.pinAve = this.pinAve;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.revealAve = this.revealAve;
    return copyPiece;
  }
  public char getSymbol() {
    return 'B';
  }

  public int evaluate(Board board) {
    int eval = 3;
    Cell curCell = board.board[curPos.rank][curPos.file];
    //Eval 1: Update the eval based on the number of attackers relative to defenders.
    List<Piece> defenders;
    List<Piece> attackers;
    if (isWhite) {
      defenders = curCell.whiteAttackers;
      attackers = curCell.blackAttackers;
    } else {
      defenders = curCell.blackAttackers;
      attackers = curCell.whiteAttackers;
    }
    if (attackers.size() > defenders.size()) {
      eval -= (attackers.size() - defenders.size());
    }
    if (attackers.size() < defenders.size()) {
      eval += (defenders.size() - attackers.size());
    }
    return eval;
  }
}
