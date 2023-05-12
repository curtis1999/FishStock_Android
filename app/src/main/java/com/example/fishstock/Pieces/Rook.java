package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.*;

public class Rook implements Piece {
  Coordinate fromCoord;
  Coordinate curPos;
  boolean isWhite;
  ArrayList<Move> legalMoves = new ArrayList<>();
  Status stat;
  ArrayList<Piece> protectors = new ArrayList<>();
  ArrayList<Piece> attackers = new ArrayList<>();
  boolean isRevealChecker = false;
  boolean isPinned;
  public boolean hasMoved; //Needed for caslting.
  Coordinate pinnerLoc;
  Coordinate revealCheckerLoc;
  ArrayList<Coordinate> pinAve = new ArrayList<>();
  ArrayList<Coordinate> revealAve = new ArrayList<>();
  ArrayList<Move>possibleMoves;
  public boolean isConnected; //True if the rooks are connected

  public Rook (Coordinate curPos, boolean isWhite) {
    this.fromCoord = curPos;
    this.curPos = curPos;
    this.isWhite=isWhite;
    this.hasMoved = false;
    this.isPinned=false;
    if (isWhite) {
      stat=Status.WHITE;
    } else {
      stat=Status.BLACK;
    }
  }
  public Rook(Coordinate fromCoord,Coordinate p, boolean isWhite) {
    this.fromCoord = fromCoord;
    this.curPos = p;
    this.isWhite=isWhite;
    this.hasMoved = false;
    this.isPinned=false;
    if (isWhite) {
      stat=Status.WHITE;
    } else {
      stat=Status.BLACK;
    }
  }

  public String getName() {
    return "Rook";
  }
  public Coordinate getPos() {
    return curPos;
  }
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> possibleMoves = new ArrayList<>();
    //Moving up the files (Towards h)
    if (pos.file<7) {
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank);
      boolean xRay=false;
      boolean revealer=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealerLoc = new Coordinate(-1,-1);
      Move mov1 = new Move(false);
      while (pos1.file<=7) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            revealer=true; //Could lead to a reveal check.
            xRay=true;
            revealerLoc = new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
            continue;
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file+1,pos1.rank);
            continue;
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Rook",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
            continue;
          }
          //Could be a pin.
        }else if (xRay && revealer){
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> revealAve = generateAvenue(pos,pos1);
              mov1.setReveal(revealerLoc, revealAve);
              break;
            }else {
              break;
            }
          }
          //Could be a pin.
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file+1,pos1.rank);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAve = generateAvenue(pos,pos1);
              mov1.setPin(pinneeLoc, pinAve);
              break;
            }else {
              break;
            }
          }
        }
      }
    }
    //Moving down the files (towards a)
    if (pos.file>0) {
      Coordinate pos1 = new Coordinate(pos.file-1,pos.rank);
      boolean xRay=false;
      boolean revealer=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealerLoc = new Coordinate(-1,-1);
      Move mov1 = new Move(false);
      while (pos1.file>=0) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            revealerLoc = new Coordinate(pos1.file,pos1.rank);
            revealer=true;
            xRay=true;
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
            continue;
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file-1,pos1.rank);
            continue;
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Rook",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
            continue;
          }
          //Could be a pin.
        }
        else if (xRay && revealer){
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> revealAve = generateAvenue(pos,pos1);
              mov1.setReveal(revealerLoc, revealAve);
              break;
            }else {
              break;
            }
          }
        }
        else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file-1,pos1.rank);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAve = generateAvenue(pos,pos1);
              mov1.setPin(pinneeLoc, pinAve);
              break;
            }
            else {
              break;
            }
          }
        }
      }
    }
    //Going towards the 8th rank
    if (pos.rank<7) {
      Coordinate pos1 = new Coordinate(pos.file,pos.rank+1);
      boolean xRay=false;
      boolean revealer=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealerLoc = new Coordinate(-1,-1);
      Move mov1 = new Move(false);
      while (pos1.rank<=7) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            revealerLoc = new Coordinate(pos1.file,pos1.rank);
            revealer=true;
            xRay=true;
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
            continue;
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file,pos1.rank+1);
            continue;
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Rook",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
            continue;
          }
          //Could be a pin.
        }else if (xRay && revealer){
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> revealAve = generateAvenue(pos,pos1);
              mov1.setReveal(revealerLoc, revealAve);
              break;
            }else {
              break;
            }
          }
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file,pos1.rank+1);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAve = generateAvenue(pos,pos1);
              mov1.setPin(pinneeLoc, pinAve);
              break;
            }else {
              break;
            }
          }
        }
      }
    }
    //Going towards the 1st rank. (Towards white)
    if (pos.rank>0) {
      Coordinate pos1 = new Coordinate(pos.file,pos.rank-1);
      boolean xRay=false;
      boolean revealer=false;
      Coordinate pinneeLoc = new Coordinate(-1,-1);
      Coordinate revealerLoc = new Coordinate(-1,-1);
      Move mov1 = new Move(false);
      while (pos1.rank>=0) {
        if (!xRay) {
          //Protection "Move".
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            mov1.setProtectionMove(pos1);
            possibleMoves.add(mov1);
            revealerLoc = new Coordinate(pos1.file,pos1.rank);
            revealer=true;
            xRay=true;
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
            continue;
          }
          //Empty cell
          else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY){
            mov1 = new Move(pos,pos1,"Rook",false,this.isWhite);
            possibleMoves.add(mov1);
            pos1 = new Coordinate (pos1.file,pos1.rank-1);
            continue;
            //Attacking Move
          }else {
            mov1 = new Move(pos,pos1,"Rook",true,this.isWhite);
            mov1.setCapture(board[pos1.rank][pos1.file].piece);
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              mov1.setCheck(mov1.fromCoord, generateAvenue(mov1.fromCoord, mov1.toCoord));
            }
            possibleMoves.add(mov1);
            xRay=true;
            pinneeLoc=new Coordinate(pos1.file,pos1.rank);
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
            continue;
          }
          //Could be a pin.
        }else if (xRay && revealer){
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> revealAve = generateAvenue(pos,pos1);
              mov1.setReveal(revealerLoc, revealAve);
              break;
            }else {
              break;
            }
          }
        }else {
          if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
            break;
          }else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
            pos1 = new Coordinate(pos1.file,pos1.rank-1);
            continue;
          }else {
            if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
              ArrayList<Coordinate> pinAve = generateAvenue(pos,pos1);
              mov1.setPin(pinneeLoc, pinAve);
              break;
            }else {
              break;
            }
          }
        }
      }
    }
    if (legalMoves.size()==0) {
      legalMoves.add(new Move(false));
    }
    this.legalMoves=possibleMoves;
    this.possibleMoves = possibleMoves;
    return possibleMoves;
  }

  //Generates a list of all coordinates between the pinning piece and the pinned piece.
  public ArrayList<Coordinate> generateAvenue(Coordinate pinnerLoc, Coordinate pinneeLoc){
    ArrayList<Coordinate> pinAvenue = new ArrayList<>();
    //Pin along the same rank
    if (pinnerLoc.rank==pinneeLoc.rank) {
      if (pinnerLoc.file>pinneeLoc.file) {
        Coordinate temp = new Coordinate(pinnerLoc.file-1, pinnerLoc.rank);
        while (temp.file>pinneeLoc.file) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file-1,temp.rank);
        }
      }else {
        Coordinate temp = new Coordinate(pinnerLoc.file+1, pinnerLoc.rank);
        while (temp.file<pinneeLoc.file) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file+1,temp.rank);
        }
      }
      //Pin is along the same file.
    }else if (pinnerLoc.file==pinneeLoc.file){
      if (pinnerLoc.rank>pinneeLoc.rank) {
        Coordinate temp = new Coordinate(pinnerLoc.file, pinnerLoc.rank-1);
        while (temp.rank>pinneeLoc.rank) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file,temp.rank-1);
        }
      }else {
        Coordinate temp = new Coordinate(pinnerLoc.file, pinnerLoc.rank+1);
        while (temp.rank<pinneeLoc.rank) {
          pinAvenue.add(temp);
          temp = new Coordinate(temp.file,temp.rank+1);
        }
      }
    }
    return pinAvenue;
  }

  public boolean getColor() {
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


  @Override
  public boolean getPin() {
    return this.isPinned;
  }
  public void moved() {
    this.hasMoved=true;
  }

  @Override
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc) {
    this.isPinned=true;
    this.pinAve=pinAve;
    this.pinnerLoc=pinnerLoc;
  }
  public void setRevealCheck(Coordinate checkerLoc) {
    this.isRevealChecker=true;
    this.revealCheckerLoc = checkerLoc;
  }

  @Override
  public Coordinate getPinnerLoc() {
    return this.pinnerLoc;
  }

  @Override
  public void unPin() {
    this.isPinned=false;
  }

  @Override
  public ArrayList<Coordinate> getPinAvenue() {
    return this.pinAve;
  }

  @Override
  public Coordinate getCheckerLoc() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setRevealChecker(ArrayList<Coordinate> revealAve,Coordinate checkerLoc) {
    this.isRevealChecker=true;
    this.revealCheckerLoc=checkerLoc;
    this.revealAve=revealAve;
  }

  @Override
  public Coordinate getRevealCheckerLoc() {
    return this.revealCheckerLoc;
  }

  @Override
  public ArrayList<Coordinate> getRevealAve() {
    return this.revealAve;
  }

  @Override
  public void unReveal() {
    this.isRevealChecker=false;
    this.revealAve=null;
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
  public void connectRooks() {
    this.isConnected=true;
  }
  public boolean isConnected() {
    return this.isConnected;
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
    Rook copyPiece = new Rook(this.curPos, this.isWhite);
    copyPiece.setPossibleMoves(this.possibleMoves);
    copyPiece.setProtectors(this.protectors);
    copyPiece.setAttackers(this.attackers);
    copyPiece.isRevealChecker = this.isRevealChecker;
    //copyPiece.revealCheckerLoc = this.revealCheckerLoc.();
    copyPiece.isPinned = this.isPinned;
    copyPiece.pinnerLoc = this.pinnerLoc;
    copyPiece.pinAve = this.pinAve;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.revealAve = this.revealAve;
    copyPiece.hasMoved = this.hasMoved;
    return copyPiece;
  }
  public char getSymbol() {
    return 'R';
  }

  public double evaluate(Board board) {
    double eval = 5.63;
    Cell curCell = board.board[curPos.rank][curPos.file];
    eval *= evaluateSafety();
    return eval;
  }

  //Analyses the list of protectors and defenders and returns a scaling factor for the eval funtion.
  public double evaluateSafety() {
    double eval = 1.0;
    //PART 1: Cancel all matches from both lists.
    ArrayList<Piece> copyProtectors = (ArrayList<Piece>) protectors.clone();
    ArrayList<Piece> copyAttackers = (ArrayList<Piece>) attackers.clone();
    for (Piece piece : copyProtectors) {
      if (piece.getName().equals("Pawn")) {
        if (removeByName(copyAttackers, "Pawn")) {
          removeByName(copyProtectors, "Pawn");
        }
      } else if (piece.getName().equals("Knight") || piece.getName().equals("Bishop")) {
        if (removeByName(copyAttackers, "Knight")){
          removeByName(copyProtectors, "Knight");
        }
      } else if (piece.getName().equals("Rook")) {
        if (removeByName(copyAttackers, "Rook")){
          removeByName(copyProtectors, "Rook");
        }
      } else if (piece.getName().equals("Queen")) {
        if (removeByName(copyAttackers, "Queen")){
          removeByName(copyProtectors, "Queen");
        }
      } else {
        if (removeByName(copyAttackers, "King")){
          removeByName(copyProtectors, "King");
        }
      }
    }
    //PART 2: evaluate the results.
    //2.1.1 BEST CASE: PROTECTED BY 2 PAWNS. (without any pawn attackers.
    if (countByType(copyProtectors, "Pawn") == 2) {
      return 1.75 + 0.15 * (protectors.size() - (1+attackers.size()));
    }
    //2.1.2WORST CASE: ATTACKED BY 2 PAWNS. (Without any pawn defenders
    if (countByType(copyAttackers, "Pawn") == 2) {
      return 0.6 - 0.15 * (attackers.size() - (1+ protectors.size()));
    }

    //2.2.1: Protected by one pawn
    if (countByType(copyProtectors, "Pawn") == 1) {
      return 1.4 + 0.15 * (protectors.size() - (1+attackers.size()));
    }
    //2.2.2: attacked by one pawn
    if (countByType(copyAttackers, "Pawn") == 1) {
      return 0.7 - 0.15 * (attackers.size() - (1+ protectors.size()));
    }
    //2.3.1: Protected by a bishop/knight
    if (countByType(copyProtectors, "Knight") + countByType(copyProtectors, "Bishop") > 0) {
      return 1.25 + 0.15 * (protectors.size() - (attackers.size()));
    }
    //2.3.1: Protected by a bishop/knight
    if (countByType(copyAttackers, "Knight") + countByType(copyAttackers, "Bishop") > 0) {
      return 0.8 - 0.15 * (protectors.size() - (attackers.size()));
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
    boolean removed = false;
    for (Piece piece: pieces) {
      if (piece.getName().equals(pieceName) || piece.getName().equals("Bishop") && pieceName.equals("Knight")) {
        pieces.remove(piece);
        removed = true;
      }
    }
    return removed;
  }
}
