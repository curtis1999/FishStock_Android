package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;


public class Queen implements Piece {
  Coordinate fromPos;
  Coordinate curPos;
  boolean isWhite;
  Status stat;
  public ArrayList<Move> legalMoves= new ArrayList<>();
  ArrayList<Piece> protectors = new ArrayList<>();
  ArrayList<Piece> attackers = new ArrayList<>();
  boolean isRevealChecker = false;
  boolean isPinned=false;
  ArrayList<Coordinate> pinAve = new ArrayList<>();
  private Coordinate pinnerLoc;
  private Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve;
  ArrayList<Move>possibleMoves = new ArrayList<>();
  boolean isPinnedToQueen;
  boolean isRevealQueenChecker;
  public ArrayList<Piece> criticallyAttacking = new ArrayList<>();
  public ArrayList<Piece> criticallyDefending = new ArrayList<>();
  public List<Integer> criticallyAttackingValues = new ArrayList<>();
  public List<Integer> criticallyDefendingValues = new ArrayList<>();

  public Queen(Coordinate curPos, boolean isWhite) {
    this.fromPos=curPos;
    this.curPos=curPos;
    this.isWhite=isWhite;
    if (this.isWhite) {
      this.stat = Status.WHITE;
    }else {
      this.stat = Status.BLACK;
    }
  }
  public Queen(Coordinate fromCoord,Coordinate coord, boolean isWhite) {
    this.fromPos=fromCoord;
    this.curPos=coord;
    this.isWhite=isWhite;
    if (this.isWhite) {
      this.stat = Status.WHITE;
    }else {
      this.stat = Status.BLACK;
    }
  }

  @Override
  public String getName() {
    return "Queen";
  }
  @Override
  public Coordinate getPos() {
    return this.curPos;
  }
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> possibleMoves = new ArrayList<>();
    //Moving up the files (Towards h)
    if (pos.file<7) {
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank);
      boolean xRay=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov1 = new Move(false);
      while (pos1.file<=7) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            xRay = true;
            revealLoc = pos1;
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file+1,pos1.rank);
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Queen",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
          }
          //X-RAY vision.  Looking for pins/reveals (To King or Queen).
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
            //Can see an adversary piece through the X-RAY.
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              if (mov1.protectionMove) {
                mov1.setReveal(revealLoc);
              } else { //(A pin)
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos1);
                mov1.setPin(pinneeLoc, pinAve);
              }
            } else if (board[pos1.rank][pos1.file].piece.getName().equals("Queen")) {
              if (mov1.protectionMove) {
                mov1.setRevealQueen(revealLoc);
              } else { //(A pin)
                mov1.setPinQueen(pinneeLoc);
              }
            }
            break;
          }
        }
      }
    }
    //Moving down the files (towards a)
    if (pos.file>0) {
      Coordinate pos1 = new Coordinate(pos.file-1,pos.rank);
      boolean xRay=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov1 = new Move(false);
      while (pos1.file>=0) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            xRay = true;
            revealLoc = pos1;
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file-1,pos1.rank);
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Queen",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
          }
          //X-RAY
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              if (mov1.protectionMove) {
                mov1.setReveal(revealLoc);
              } else { //(A pin)
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos1);
                mov1.setPin(pinneeLoc, pinAve);
              }
            } else if (board[pos1.rank][pos1.file].piece.getName().equals("Queen")) {
              if (mov1.protectionMove) {
                mov1.setRevealQueen(revealLoc);
              } else { //(A pin)
                mov1.setPinQueen(pinneeLoc);
              }
            }
            break;
          }
        }
      }
    }
    //Going towards the 8th rank
    if (pos.rank<7) {
      Coordinate pos1 = new Coordinate(pos.file,pos.rank+1);
      boolean xRay=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov1 = new Move(false);
      while (pos1.rank<=7) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            xRay = true;
            revealLoc = pos1;
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file,pos1.rank+1);
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Queen",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
          }
          //Could be a pin.
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              if (mov1.protectionMove) {
                mov1.setReveal(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos1);
                mov1.setPin(pinneeLoc, pinAve);
              }
            } else if (board[pos1.rank][pos1.file].piece.getName().equals("Queen")) {
              if (mov1.protectionMove) {
                mov1.setRevealQueen(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos1);
                mov1.setPinQueen(pinneeLoc);
              }
            }
            break;
          }
        }
      }
    }
    //Going towards the 1st rank.
    if (pos.rank>0) {
      Coordinate pos1 = new Coordinate(pos.file,pos.rank-1);
      boolean xRay=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov1 = new Move(false);
      while (pos1.rank>=0) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            xRay = true;
            revealLoc = pos1;
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file,pos1.rank-1);
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Queen",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
          }
          //Could be a pin.
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              if (mov1.protectionMove) {
                mov1.setReveal(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos1);
                mov1.setPin(pinneeLoc, pinAve);
              }
            } else if (board[pos1.rank][pos1.file].piece.getName().equals("Queen")) {
              if (mov1.protectionMove) {
                mov1.setRevealQueen(revealLoc);
              } else {
                mov1.setPinQueen(pinneeLoc);
              }
            }
            break;
          }
        }
      }
    }

    //Towards a8
    if (pos.file<7 && pos.rank<7) {
      boolean xRay = false;
      Coordinate pinLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1 , -1);
      Move mov1 = new Move(false);
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank+1);
      while (pos1.file<=7 && pos1.rank<=7) {
        if (!xRay) {
          if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            mov1 = new Move(pos,pos1,"Queen",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate(pos1.file+1,pos1.rank+1);
          }else if (board[pos1.rank][pos1.file].PieceStatus!=this.stat) {
            mov1 = new Move(pos,pos1,"Queen",true, this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinLoc = new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file+1,pos1.rank+1);
          }else {
            mov1 = new Move(pos,pos1,"Queen",false, this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            xRay = true;
            revealLoc = pos1;
            pos1 = new Coordinate(pos1.file+1,pos1.rank+1);
          }
        }//x-ray "moves".
        else {
          if (board[pos1.rank][pos1.file].PieceStatus == this.stat) {
            break;
          } else if (board[pos1.rank][pos1.file].PieceStatus == Status.EMPTY) {
            pos1 = new Coordinate(pos1.file + 1, pos1.rank + 1);
          } else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              if (mov1.protectionMove) {
                mov1.setReveal(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos1);
                mov1.setPin(pinLoc, pinAve);
              }
            } else if (board[pos1.rank][pos1.file].piece.getName().equals("Queen")) {
              if (mov1.protectionMove) {
                mov1.setRevealQueen(revealLoc);
              } else {
                mov1.setPinQueen(pinLoc);
              }
            }
            break;
          }
        }
      }
    }
    //Towards a1
    if (pos.file<7 && pos.rank>0) {
      Coordinate pos2 = new Coordinate(pos.file+1,pos.rank-1);
      Coordinate pinLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov2 = new Move(false);
      boolean xRay = false;
      while (pos2.file<=7 && pos2.rank>=0) {
        if (!xRay) {
          if (board[pos2.rank][pos2.file].PieceStatus==Status.EMPTY) {
            mov2 = new Move(pos,pos2,"Queen",false,this.isWhite);
            possibleMoves.add(mov2);
            pos2= new Coordinate(pos2.file+1,pos2.rank-1);
            //If there is an enemy piece in the way.
          }else if (board[pos2.rank][pos2.file].PieceStatus!=this.stat) {
            mov2 = new Move(pos,pos2,"Queen",true, this.isWhite);
            mov2.setCapture(board[pos2.rank][pos2.file].piece);
            if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
              mov2.setCheck(mov2.fromCoord, generateAvenue(mov2.fromCoord, mov2.toCoord));
            }
            possibleMoves.add(mov2);
            xRay=true;
            pinLoc = new Coordinate(pos2.file, pos2.rank);
            pos2 = new Coordinate(pos2.file+1,pos2.rank-1);
          }else {
            mov2 = new Move(pos,pos2,"Queen",false, this.isWhite);
            mov2.setProtectionMove(pos2);
            possibleMoves.add(mov2);
            xRay = true;
            revealLoc = pos2;
            pos2 = new Coordinate(pos2.file+1,pos2.rank-1);
          }
        } else {
          if (board[pos2.rank][pos2.file].PieceStatus == this.stat) {
            break;
          } else if (board[pos2.rank][pos2.file].PieceStatus == Status.EMPTY) {
            pos2 = new Coordinate(pos2.file + 1, pos2.rank - 1);
          } else {
            if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
              if (mov2.protectionMove) {
                mov2.setReveal(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos2);
                mov2.setPin(pinLoc, pinAve);
              }
            } else if (board[pos2.rank][pos2.file].piece.getName().equals("Queen")) {
              if (mov2.protectionMove) {
                mov2.setRevealQueen(revealLoc);
              } else {
                mov2.setPinQueen(pinLoc);
              }
            }
            break;
          }
        }
      }
    }

    //Towards h8
    if (pos.file>0 && pos.rank<7) {
      Coordinate pos3 = new Coordinate(pos.file-1,pos.rank+1);
      Coordinate pinLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov3 = new Move(false);
      boolean xRay=false;
      while (pos3.file>=0 && pos3.rank<=7) {
        if (!xRay) {
          if (board[pos3.rank][pos3.file].PieceStatus==Status.EMPTY) {
            mov3 = new Move(pos,pos3,"Queen",false,this.isWhite);
            possibleMoves.add(mov3);
            pos3 = new Coordinate(pos3.file-1, pos3.rank+1);
            //If there is an enemy piece in the way.
          }else if (board[pos3.rank][pos3.file].PieceStatus!=this.stat) {
            mov3 = new Move(pos,pos3,"Queen",true, this.isWhite);
            mov3.setCapture(board[pos3.rank][pos3.file].piece);
            if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
              mov3.setCheck(mov3.fromCoord, generateAvenue(mov3.fromCoord, mov3.toCoord));
            }
            possibleMoves.add(mov3);
            xRay=true;
            pinLoc = new Coordinate(pos3.file,pos3.rank);
            pos3 = new Coordinate(pos3.file-1, pos3.rank+1);
            //Your own piece in the way
          }else {
            mov3 = new Move(pos,pos3,"Queen",false, this.isWhite);
            mov3.setProtectionMove(pos3);
            possibleMoves.add(mov3);
            xRay = true;
            revealLoc = pos3;
            pos3 = new Coordinate(pos3.file-1,pos3.rank+1);
          }
        }else {
          if (board[pos3.rank][pos3.file].PieceStatus == this.stat) {
            break;
          } else if (board[pos3.rank][pos3.file].PieceStatus == Status.EMPTY) {
            pos3 = new Coordinate(pos3.file - 1, pos3.rank + 1);
          } else {
            if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
              if (mov3.protectionMove) {
                mov3.setReveal(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos3);
                mov3.setPin(pinLoc, pinAve);
              }
            } else if (board[pos3.rank][pos3.file].piece.getName().equals("Queen")) {
              if (mov3.protectionMove) {
                mov3.setRevealQueen(revealLoc);
              } else {
                mov3.setPinQueen(pinLoc);
              }
            }
            break;
          }
        }
      }
    }

    //Towards h1
    if (pos.file>0 && pos.rank>0) {
      Coordinate pos4 = new Coordinate(pos.file-1,pos.rank-1);
      Coordinate pinLoc = new Coordinate(-1,-1);
      Coordinate revealLoc = new Coordinate(-1, -1);
      Move mov4 = new Move(false);
      boolean xRay=false;
      while (pos4.file>=0 && pos4.rank>=0) {
        if (!xRay) {
          if (board[pos4.rank][pos4.file].PieceStatus==Status.EMPTY) {
            mov4 = new Move(pos,pos4,"Queen",false,this.isWhite);
            possibleMoves.add(mov4);
            pos4= new Coordinate(pos4.file-1, pos4.rank-1);
            //If there is an enemy piece in the way.
          }else if (board[pos4.rank][pos4.file].PieceStatus!=this.stat) {
            mov4 = new Move(pos,pos4,"Queen",true, this.isWhite);
            mov4.setCapture(board[pos4.rank][pos4.file].piece);
            if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
              mov4.setCheck(mov4.fromCoord, generateAvenue(mov4.fromCoord, mov4.toCoord));
            }
            possibleMoves.add(mov4);
            xRay = true;
            pinLoc = new Coordinate(pos4.file,pos4.rank);
            pos4= new Coordinate(pos4.file-1, pos4.rank-1);
            //Your own piece in the way
          }else {
            mov4= new Move(pos,pos4,"Queen",false, this.isWhite);
            mov4.setProtectionMove(pos4);
            possibleMoves.add(mov4);
            xRay = true;
            revealLoc = pos4;
            pos4 = new Coordinate(pos4.file-1,pos4.rank-1);
          }
        }else {
          if (board[pos4.rank][pos4.file].PieceStatus == this.stat) {
            break;
          } else if (board[pos4.rank][pos4.file].PieceStatus == Status.EMPTY) {
            pos4 = new Coordinate(pos4.file - 1, pos4.rank - 1);
          } else {
            if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
              if (mov4.protectionMove) {
                mov4.setReveal(revealLoc);
              } else {
                ArrayList<Coordinate> pinAve = generateAvenue(pos, pos4);
                mov4.setPin(pinLoc, pinAve);
              }
            } else if (board[pos4.rank][pos4.file].piece.getName().equals("Queen")) {
              if (mov4.protectionMove) {
                mov4.setRevealQueen(revealLoc);
              } else {
                mov4.setPinQueen(pinLoc);
              }
            }
            break;
          }
        }
      }

    }
    if (possibleMoves.size()==0) {
      possibleMoves.add(new Move(false));	//Avoids null pointer exception.
    }
    this.possibleMoves=possibleMoves;
    return possibleMoves;


  }

  public ArrayList<Coordinate> generateAvenue(Coordinate pinnerLoc, Coordinate kingLoc){
    ArrayList<Coordinate> pinningAve = new ArrayList<>();
    if (pinnerLoc.file==kingLoc.file) {
      if (pinnerLoc.rank>kingLoc.rank) {
        Coordinate tempLoc = new Coordinate(pinnerLoc.file,pinnerLoc.rank-1);
        while (tempLoc.rank>kingLoc.rank) {
          pinningAve.add(tempLoc);
          tempLoc = new Coordinate(tempLoc.file,tempLoc.rank-1);
        }
      }else {
        Coordinate tempLoc = new Coordinate(pinnerLoc.file, pinnerLoc.rank+1);
        while (tempLoc.rank<kingLoc.rank) {
          pinningAve.add(tempLoc);
          tempLoc = new Coordinate(tempLoc.file,tempLoc.rank+1);
        }
      }
    }else if (pinnerLoc.rank==kingLoc.rank) {
      if (pinnerLoc.file>kingLoc.file) {
        Coordinate tempLoc = new Coordinate(pinnerLoc.file-1,pinnerLoc.rank);
        while (tempLoc.file>kingLoc.file) {
          pinningAve.add(tempLoc);
          tempLoc = new Coordinate(tempLoc.file-1,tempLoc.rank);
        }
      }else {
        Coordinate tempLoc = new Coordinate(pinnerLoc.file+1, pinnerLoc.rank);
        while (tempLoc.file<kingLoc.file) {
          pinningAve.add(tempLoc);
          tempLoc = new Coordinate(tempLoc.file+1,tempLoc.rank+1);
        }
      }
    }
    //Same Diagonal
    else if((pinnerLoc.file+pinnerLoc.rank)==kingLoc.file+kingLoc.rank) {
      if (pinnerLoc.rank>kingLoc.rank) {
        Coordinate temp = new Coordinate(pinnerLoc.file+1, pinnerLoc.rank-1);
        while (temp.rank>kingLoc.rank) {
          pinningAve.add(temp);
          temp = new Coordinate(temp.file+1,temp.rank-1);
        }
      }else {
        Coordinate temp = new Coordinate(pinnerLoc.file-1, pinnerLoc.rank+1);
        while (temp.rank<kingLoc.rank) {
          pinningAve.add(temp);
          temp = new Coordinate(temp.file-1,temp.rank+1);
        }
      }
    }
    else if((pinnerLoc.file-pinnerLoc.rank)==kingLoc.file-kingLoc.rank) {
      if (pinnerLoc.rank>kingLoc.rank) {
        Coordinate temp = new Coordinate(pinnerLoc.file-1, pinnerLoc.rank-1);
        while (temp.rank>kingLoc.rank) {
          pinningAve.add(temp);
          temp = new Coordinate(temp.file-1,temp.rank-1);
        }
      }else {
        Coordinate temp = new Coordinate(pinnerLoc.file+1, pinnerLoc.rank+1);
        while (temp.rank<kingLoc.rank) {
          pinningAve.add(temp);
          temp = new Coordinate(temp.file+1,temp.rank+1);
        }
      }
    }
    return pinningAve;
  }

  public char getSymbol() {
    return 'Q';
  }

  @Override
  public double evaluateSimple(Board board) {
    return 9.5;
  }

  public double evaluate(Board board) {
    Cell curCell = board.board[curPos.rank][curPos.file];
    double eval = 9.5;
    if (isPinned) {
      eval *= 0.5;
    }
    //EVAL 2: IF THE PIECE IS A REVEAL CHECKER, INCREASE THE EVAL BY 2.
    if (isRevealChecker) {
      eval *= 1.5;
    }
    if (isPinnedToQueen) {
      eval *= 2.0/3.0;
    }
    if (isRevealQueenChecker) {
      eval *= 1.25;
    }
    eval *= evaluateSafety(curCell);
    return eval;
  }

  //Analyses the list of protectors and defenders and returns a scaling factor for the eval funtion.
  public double evaluateSafety(Cell curCell) {
    ArrayList<Piece> attackers;
    ArrayList<Piece> protectors;
    if (isWhite) {
      attackers = curCell.blackAttackers;
      protectors = curCell.whiteAttackers;
    } else {
      attackers = curCell.whiteAttackers;
      protectors = curCell.blackAttackers;
    }
    //A hanging piece which can be taken.
    if (attackers.size() > 0 && protectors.size() == 0) {
      return -1;
    }
    //If it can be captured by a pawn -> bad eval.
    if (countByType(attackers, "Pawn") > 0) {
      return 0;
    }
    if (countByType(attackers, "Knight") > 0) {
      if (attackers.size() > protectors.size()) {
        return -1;
      } else {
        return -0.5;
      }
    }
    if (countByType(attackers, "Rook") > 0) {
      if (attackers.size() > protectors.size()) {
        return 0;
      } else {
        return 0.15;
      }
    }
    if (countByType(protectors, "Pawn") == 2) {
      return 1.2;
    }
    if (countByType(protectors, "Pawn") == 1){
      return 1.1;
    }
    return 1.0;
  }

  public static int countByType(ArrayList<Piece> pieces, String pieceName) {
    int num = 0;
    for (Piece piece : pieces) {
      if (piece.getName().equals(pieceName)) {
        num++;
      }
    }
    return num;
  }
  //NOTE: pieceName of Knight for both Bishops and knights.
  public boolean removeByName(List<Piece> pieces, String pieceName) {
    for (Piece piece: pieces) {
      if (piece.getName().equals(pieceName) || piece.getName().equals("Bishop") && pieceName.equals("Knight")) {
        pieces.remove(piece);
        return true;
      }
    }
    return false;
  }
  @Override
  public void setQueenPin() {
    this.isPinnedToQueen = true;
  }
  public void setRevealQueenChecker() {
    this.isRevealQueenChecker = true;
  }
  @Override
  public void setReveal() {
    this.isRevealChecker = true;
  }

  @Override
  public void setRevealQueen() {
    this.isRevealQueenChecker = true;
  }
  public boolean isRevealChecker(){
    return this.isRevealChecker;
  }

  public boolean isRevealQueenChecker(){
    return this.isRevealQueenChecker;
  }
  public boolean isPinnedToQueen() {
    return this.isPinnedToQueen;
  }

  @Override
  public boolean getColor() {
    // TODO Auto-generated method stub
    return this.isWhite;
  }

  @Override
  public void setPos(Coordinate coord) {
    this.fromPos = curPos;
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

  public boolean isPinned() {
    return this.isPinned;
  }


  public ArrayList<Coordinate> getPinAvenue() {
    return this.pinAve;
  }
  public void unPin() {
    this.isPinned=false;
    this.pinAve = null;
  }
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
  public void setRevealChecker(ArrayList<Coordinate> revealAve,Coordinate checkerLoc) {
    this.isRevealChecker=true;
    this.revealCheckerLoc = checkerLoc;
    this.revealAve = revealAve;
  }
  public Coordinate getRevealCheckerLoc() {
    return this.revealCheckerLoc;
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
    this.criticallyAttacking = new ArrayList<>();
    this.criticallyDefending = new ArrayList<>();
  }
  public void setProtectors(ArrayList<Piece> protectors){
    this.protectors = protectors;
  }
  public void setAttackers(ArrayList<Piece> attackers) {
    this.attackers = attackers;
  }
  @Override
  public Piece copyPiece() {
    Queen copyPiece = new Queen(this.curPos, this.isWhite);
    copyPiece.setPossibleMoves(this.possibleMoves);
    copyPiece.setProtectors(this.protectors);
    copyPiece.setAttackers(this.attackers);
    copyPiece.isRevealChecker = this.isRevealChecker;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.isPinned = this.isPinned;
    copyPiece.pinnerLoc = this.pinnerLoc;
    copyPiece.pinAve = this.pinAve;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.revealAve = this.revealAve;
    return copyPiece;
  }
  public void addCriticalAttack(Piece piece) {
    this.criticallyAttacking.add(piece);
  }
  public void addCriticalDefenence(Piece piece) {
    this.criticallyDefending.add(piece);
  }

  @Override
  public void addOverloadValue(int value) {
    this.criticallyDefendingValues.add(value);
  }

  @Override
  public void addForkValue(int value) {
    this.criticallyAttackingValues.add(value);
  }

  public int getValue() {
    return 9;
  }
  @Override
  public Coordinate getFromPos() {
    return this.fromPos;
  }
}
