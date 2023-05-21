package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;
import java.util.ArrayList;
import java.util.List;

public class King implements Piece {
  Coordinate curPos;
  Coordinate fromPos;
  public boolean isWhite;
  public boolean isChecked;
  public boolean isDoubleChecked;
  public Coordinate checkerLoc;
  public Coordinate checkerLoc2;
  Status stat;
  ArrayList<Move> possibleMoves= new ArrayList<>();
  public ArrayList<Piece> attackers = new ArrayList<>();
  public ArrayList<Piece> protectors = new ArrayList<>();
  boolean isRevealChecker = false;
  boolean isPinned=false;
  private Object pinAve;
  private Object pinnerLoc;
  public boolean hasMoved; //Cannot castle after moving your king.
  public Coordinate revealCheckerLoc;
  private ArrayList<Coordinate> revealAve;
  ArrayList<Coordinate> checkAve =  new ArrayList<>();
  ArrayList<Coordinate> checkAve2 = new ArrayList<>();
  boolean isPinnedToQueen;
  boolean isRevealQueenChecker;
  public ArrayList<Piece> xRayingPieces = new ArrayList<>();
  public ArrayList<Piece> criticallyAttacking = new ArrayList<>();
  public ArrayList<Piece> criticallyDefending = new ArrayList<>();
  public List<Integer> criticallyAttackingValues = new ArrayList<>();
  public List<Integer> criticallyDefendingValues = new ArrayList<>();
  public int forkingValue = 0;
  public int overLoadingValue = 0;

  public King(Coordinate crd, boolean isWhite) {
    this.curPos =crd;
    this.fromPos = crd;
    this.isWhite=isWhite;
    this.isChecked =false;
    this.isDoubleChecked = false;
    if (isWhite) {
      this.stat=Status.WHITE;
    }else {
      this.stat = Status.BLACK;
    }
    this.hasMoved=false;
  }

  @Override
  public String getName() {
    return "King";
  }
  @Override
  public Coordinate getPos() {
    return this.curPos;
  }
  @Override
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> possibleMoves = new ArrayList<Move>();
    if (pos.file<7) {
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
            mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
        //Adversary Piece on this square.
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true, this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        }//A protected piece on this square. Add a cover move.
       else {
          Move mov1 = new Move(pos, pos1, "King", false, this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Moving down the files (towards a)
    if (pos.file>0) {
      Coordinate pos1 = new Coordinate(pos.file-1,pos.rank);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos, pos1, "King", false, this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)) {
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
        //Adversary on the square
      } else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Going towards the 8th rank
    if (pos.rank<7) {
      Coordinate pos1 = new Coordinate (pos.file,pos.rank+1);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Going towards the 1st rank. (Towards white)
    if (pos.rank>0) {
      Coordinate pos1 = new Coordinate (pos.file,pos.rank-1);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Towards a8
    if (pos.rank<7 && pos.file<7) {
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank+1);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Towards a1
    if (pos.rank>0 && pos.file<7) {
      Coordinate pos1 = new Coordinate(pos.file+1,pos.rank-1);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Towards h8
    if (pos.rank<7 && pos.file>0) {
      Coordinate pos1 = new Coordinate(pos.file-1,pos.rank+1);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    //Towards h1
    if (pos.rank>0 && pos.file>0) {
      Coordinate pos1 = new Coordinate(pos.file-1,pos.rank-1);
      if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
      }if (board[pos1.rank][pos1.file].PieceStatus==this.stat) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        mov1.setProtectionMove(pos1);
        possibleMoves.add(mov1);
      }
      else if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
        if ((this.isWhite && board[pos1.rank][pos1.file].blackAttackers.size() > 0)
            || (!this.isWhite && board[pos1.rank][pos1.file].whiteAttackers.size() > 0)){
          mov1.setCoverMove();
        }
        possibleMoves.add(mov1);
      }else {
        if(board[pos1.rank][pos1.file].piece.getProtectors().size()==0) {
          Move mov1 = new Move(pos,pos1,"King",true,this.isWhite);
          mov1.setCapture(board[pos1.rank][pos1.file].piece);
          possibleMoves.add(mov1);
        } else {
          Move mov1 = new Move(pos,pos1,"King",false,this.isWhite);
          mov1.setCoverMove();
          possibleMoves.add(mov1);
        }
      }
    }
    if (!hasMoved && !isChecked) {
      //CASTING MOVES.
      if (isWhite) {
        //Part 1: short castle:
        // Check1: If the King and Rook are on starting squares and have not moved.
        if (board[0][0].PieceStatus == this.stat
            && board[0][0].piece.getName().equals("Rook") && !((Rook) board[0][0].piece).hasMoved) {
          //Check 2: No Pieces or black attackers within the castling lane.
          if (board[0][2].blackAttackers.size() == 0 && board[0][2].PieceStatus == Status.EMPTY &&
              board[0][1].blackAttackers.size() == 0 && board[0][1].PieceStatus == Status.EMPTY) {
            Move shortcastleMove = new Move(this.curPos, new Coordinate(1, 0), "King", false, true);
            shortcastleMove.setCastle();
            possibleMoves.add(shortcastleMove);
          }
        }
        //Part 2: Long Castle
        if (board[0][7].PieceStatus == this.stat
            && board[0][7].piece.getName().equals("Rook") && !((Rook) board[0][7].piece).hasMoved) {

          if (board[0][4].blackAttackers.size() == 0 && board[0][4].PieceStatus == Status.EMPTY
              && board[0][5].blackAttackers.size() == 0 && board[0][5].PieceStatus == Status.EMPTY
              && board[0][6].PieceStatus.equals(Status.EMPTY)) {
            Move longCastleMove = new Move(this.curPos, new Coordinate(5, 0), "King", false, true);
            longCastleMove.setCastle();
            possibleMoves.add(longCastleMove);
          }
        }
      } else {
        //Part 1: short castle:
        // Check1: If the King and Rook are on starting squares and have not moved.
        if (!hasMoved && board[7][0].PieceStatus == this.stat
            && board[7][0].piece.getName().equals("Rook") && !((Rook) board[7][0].piece).hasMoved) {
          //Check 2: No black attackers within the castling lane.
          if (board[7][2].whiteAttackers.size() == 0 && board[7][2].PieceStatus == Status.EMPTY
              && board[7][1].whiteAttackers.size() == 0 && board[7][2].PieceStatus == Status.EMPTY) {
            Move shortcastleMove = new Move(this.curPos, new Coordinate(1, 7), "King", false, false);
            shortcastleMove.setCastle();
            possibleMoves.add(shortcastleMove);
          }
        }
        //Part 2: Long Castle
        if (!hasMoved && board[7][7].PieceStatus == this.stat
            && board[7][7].piece.getName().equals("Rook") && !((Rook) board[7][7].piece).hasMoved) {
          if (board[7][4].whiteAttackers.size() == 0 && board[7][4].PieceStatus == Status.EMPTY
              && board[7][5].whiteAttackers.size() == 0 && board[7][5].PieceStatus == Status.EMPTY
              && board[7][6].PieceStatus.equals(Status.EMPTY)) {
            Move longCastleMove = new Move(this.curPos, new Coordinate(5, 7), "King", false, false);
            longCastleMove.setCastle();
            possibleMoves.add(longCastleMove);
          }
        }
      }
    }
    this.possibleMoves=possibleMoves;
    return possibleMoves;
  }
  public double evaluateSafety(Board board) {
    double a = 1;
    double b = 1;
    double c = 1;
    double openFactor;
    double moveFactor;
    double xRayFactor;
    boolean isOnOpenFile = false;
    boolean isOnOpenDiagonalUp = false;
    boolean isOnOpenDiagonalDown = false;
    if (Board.countAlongFile(board.board, "Pawn", isWhite, curPos.rank, curPos.file, isWhite) == 0) {
      isOnOpenFile = true;
    }
    if (Board.countAlongDiagonal(board.board, "Pawn", curPos, true, isWhite) == 0) {
      isOnOpenDiagonalUp = true;
    }
    if (Board.countAlongDiagonal(board.board, "Pawn", curPos, false, isWhite) == 0) {
      isOnOpenDiagonalDown = true;
    }
    if (!isOnOpenFile && !isOnOpenDiagonalDown && !isOnOpenDiagonalUp) {
      openFactor = 1;
    }
    else if (isOnOpenFile && isOnOpenDiagonalDown && isOnOpenDiagonalUp) {
      openFactor = -1;
    }
    else if (!isOnOpenFile) {
      openFactor = 0;
    } else {
      openFactor = -0.5;
    }

    int numMoves = GameService.filterMoves(possibleMoves).size();
    if (numMoves < 2) {
      moveFactor = -1;
    } else {
      moveFactor = 1;
    }
    if (xRayingPieces.size() == 0) {
      xRayFactor = 1;
    } else {
      xRayFactor = -0.5 * (xRayingPieces.size());
    }
    return (a * openFactor) + (b * moveFactor) + (c * xRayFactor);
  }

  public boolean getColor() {
    return this.isWhite;
  }

  public void setPos(Coordinate coord) {
    this.fromPos = coord;
    this.curPos =coord;
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

  public void setCheck(Coordinate checkerLoc, ArrayList<Coordinate> checkAve) {
    if (this.isChecked) {
      this.isDoubleChecked = true;
      this.checkerLoc2 = checkerLoc;
      this.checkAve2 = checkAve;
    } else {
      this.isChecked = true;
      this.checkerLoc=checkerLoc;
      this.checkAve=checkAve;
    }

  }
  public Coordinate getCheckerLoc() {
    return this.checkerLoc;
  }
  public void unCheck() {
    this.isChecked = false;
    this.isDoubleChecked=false;
  }
  public void setRevealChecker() {
    this.isRevealChecker=true;
  }
  public Coordinate getRevealCheckerLoc() {
    return this.revealCheckerLoc;
  }

  public boolean isPinned() {
    return this.isPinned;
  }

  public void unPin() {
  }
  public void setPin(ArrayList<Coordinate> pinAve, Coordinate pinnerLoc) {
  }
  public Coordinate getPinnerLoc() {
    return null;
  }

  public void moved() {
    this.hasMoved=true;
  }

  @Override
  public ArrayList<Coordinate> generateAvenue(Coordinate c1, Coordinate c2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setRevealChecker(ArrayList<Coordinate> revealAve,Coordinate revealCheckerLoc) {
    this.isRevealChecker=true;
    this.revealCheckerLoc = revealCheckerLoc;
    this.revealAve=revealAve;
  }
  @Override
  public void setQueenPin() {
    this.isPinnedToQueen = true;
  }
  public void setRevealQueenChecker() {
    this.isRevealQueenChecker = true;
  }

  public void setDoubleChecked(Coordinate pos, Coordinate toCoord) {
    this.isDoubleChecked=true;
    this.checkerLoc=pos;
    this.checkerLoc2 = toCoord;
  }
  public boolean isDoubleChecked() {
    return this.isDoubleChecked;
  }
  public void setCheckAve(ArrayList<Coordinate> checkAve) {
    this.checkAve=checkAve;
  }
  public ArrayList<Coordinate> checkAve(){
    return this.checkAve;
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
    this.xRayingPieces = new ArrayList<>();
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
    King copyPiece = new King(this.curPos, this.isWhite);
    copyPiece.setPossibleMoves(this.possibleMoves);
    copyPiece.setProtectors(this.protectors);
    copyPiece.setAttackers(this.attackers);
    copyPiece.isRevealChecker = this.isRevealChecker;
    copyPiece.revealCheckerLoc = this.revealCheckerLoc;
    copyPiece.revealAve = this.revealAve;
    copyPiece.hasMoved = this.hasMoved;
    return copyPiece;
  }

  public ArrayList<Coordinate> getCheckingAve() {
    return this.checkAve;
  }
  public char getSymbol() {
    return 'K';
  }

  public double evaluate(Board board) {
    if (isDoubleChecked) {
      return -5.0;
    } else if (isChecked) {
      return -2.5;
    } else {
      return 0.0;
    }

  }

  @Override
  public double evaluateSimple(Board board) {
    if (isDoubleChecked) {
      return -5.0;
    } else if (isChecked) {
      return -2.5;
    } else {
      return 0.0;
    }
  }

  public ArrayList<Coordinate> getCheckingAve2() {
    return this.checkAve2;
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
  public void setXRay(Piece piece) {
    this.xRayingPieces.add(piece);
  }
  public void addCriticalAttack(Piece piece) {
    this.criticallyAttacking.add(piece);
    if (criticallyAttacking.size() > 1) {
      forkingValue = GameService.getSecondHighestValue(criticallyAttacking);
    }
  }
  public void addCriticalDefenence(Piece piece) {
    this.criticallyDefending.add(piece);
    //TODO: Add case where the two protecting pieces are beside each other.
    if (criticallyDefending.size() > 1) {
      overLoadingValue = GameService.getSecondHighestValue(criticallyDefending);
    }
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
    return 999;
  }
  @Override
  public Coordinate getFromPos() {
    return this.fromPos;
  }
}
