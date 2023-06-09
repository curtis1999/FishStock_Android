package com.example.fishstock.Pieces;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.GameService;
import com.example.fishstock.Move;
import com.example.fishstock.Status;

import java.util.ArrayList;
import java.util.List;

public class Knight implements Piece {
  Coordinate fromPos;
  Coordinate curPos;
  boolean isWhite;
  ArrayList<Move> legalMoves;
  Status stat;
  ArrayList<Piece> protectors = new ArrayList<>();
  ArrayList<Piece> attackers = new ArrayList<>();
  boolean isRevealChecker = false;
  boolean isPinned=false;
  public ArrayList<Coordinate> pinAve;
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
  public int forkingValue = 0;
  public int overLoadingValue = 0;
  public Knight (Coordinate curPos, boolean isWhite) {
    this.fromPos =curPos;
    this.curPos = curPos;
    this.isWhite=isWhite;
    if (isWhite) {
      this.stat = Status.WHITE;
    } else {
      this.stat = Status.BLACK;
    }
  }
  public Knight(Coordinate fromCoord,Coordinate pos, boolean isWhite) {
    this.fromPos =fromCoord;
    this.curPos = pos;
    this.isWhite=isWhite;
    if (isWhite) {
      this.stat = Status.WHITE;
    } else {
      this.stat = Status.BLACK;
    }
  }

  public String getName() {
    return "Knight";
  }
  public Coordinate getPos() {
    return this.curPos;
  }
  public ArrayList<Move> generateMoves(Coordinate pos, Cell[][] board) {
    ArrayList<Move> legalMoves = new ArrayList<>();
    Coordinate pos1 = new Coordinate(pos.file+1, pos.rank+2);
    Move mv1 = new Move(pos, pos1,"Knight",false,this.isWhite);
    if (pos1.rank>=0 && pos1.rank<8 && pos1.file>=0 && pos1.file<8) {
      if (board[pos1.rank][pos1.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv1);
      }else if (board[pos1.rank][pos1.file].PieceStatus!=this.stat) {
        mv1.setCapture(board[pos1.rank][pos1.file].piece);
        if (board[pos1.rank][pos1.file].piece.getName().equals("King")) {
          mv1.setCheck(mv1.fromCoord, generateAvenue(mv1.fromCoord, mv1.toCoord));
        }
        legalMoves.add(mv1);
      }else {
        mv1.setProtectionMove(pos1);
        legalMoves.add(mv1);
      }
    }
    Coordinate pos2 = new Coordinate(pos.file+2, pos.rank+1);
    Move mv2 = new Move(pos,pos2,"Knight",false,this.isWhite);
    if (pos2.rank>=0 && pos2.rank<8 && pos2.file>=0 && pos2.file<8) {
      if (board[pos2.rank][pos2.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv2);
      } else if (!(board[pos2.rank][pos2.file].PieceStatus==this.stat)) {
        mv2.setCapture(board[pos2.rank][pos2.file].piece);
        if (board[pos2.rank][pos2.file].piece.getName().equals("King")) {
          mv2.setCheck(mv2.fromCoord, generateAvenue(mv2.fromCoord, mv2.toCoord));
        }
        legalMoves.add(mv2);
      }else {
        mv2.setProtectionMove(pos2);
        legalMoves.add(mv2);
      }
    }
    Coordinate pos3 = new Coordinate(pos.file-1, pos.rank+2);
    Move mv3 = new Move(pos,pos3,"Knight",false,this.isWhite);
    if (pos3.rank>=0 && pos3.rank<8 && pos3.file>=0 && pos3.file<8) {
      if (board[pos3.rank][pos3.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv3);
      } else if (!(board[pos3.rank][pos3.file].PieceStatus==this.stat)) {
        mv3.setCapture(board[pos3.rank][pos3.file].piece);
        if (board[pos3.rank][pos3.file].piece.getName().equals("King")) {
          mv3.setCheck(mv3.fromCoord, generateAvenue(mv3.fromCoord, mv3.toCoord));
        }
        legalMoves.add(mv3);
      }else {
        mv3.setProtectionMove(pos3);
        legalMoves.add(mv3);
      }
    }

    Coordinate pos4 = new Coordinate(pos.file+2, pos.rank-1);
    Move mv4  = new Move(pos,pos4, "Knight",false,this.isWhite);
    if (pos4.rank>=0 && pos4.rank<8 && pos4.file>=0 && pos4.file<8) {
      if (board[pos4.rank][pos4.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv4);
      } else if (!(board[pos4.rank][pos4.file].PieceStatus==this.stat)) {
        mv4.setCapture(board[pos4.rank][pos4.file].piece);
        if (board[pos4.rank][pos4.file].piece.getName().equals("King")) {
          mv4.setCheck(mv4.fromCoord, generateAvenue(mv4.fromCoord, mv4.toCoord));
        }
        legalMoves.add(mv4);
      }else {
        mv4.setProtectionMove(pos4);
        legalMoves.add(mv4);
      }
    }
    Coordinate pos5 = new Coordinate(pos.file-2, pos.rank+1);
    Move mv5  = new Move(pos,pos5, "Knight",false,this.isWhite);
    if (pos5.rank>=0 && pos5.rank<8 && pos5.file>=0 && pos5.file<8) {
      if (board[pos5.rank][pos5.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv5);
      } else if (!(board[pos5.rank][pos5.file].PieceStatus==this.stat)) {
        mv5.setCapture(board[pos5.rank][pos5.file].piece);
        if (board[pos5.rank][pos5.file].piece.getName().equals("King")) {
          mv5.setCheck(mv5.fromCoord, generateAvenue(mv5.fromCoord, mv5.toCoord));
        }
        legalMoves.add(mv5);
      }else {
        mv5.setProtectionMove(pos5);
        legalMoves.add(mv5);
      }
    }
    Coordinate pos6 = new Coordinate(pos.file+1, pos.rank-2);
    Move mv6  = new Move(pos,pos6, "Knight",false,this.isWhite);
    if (pos6.rank>=0 && pos6.rank<8 && pos6.file>=0 && pos6.file<8) {
      if (board[pos6.rank][pos6.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv6);
      } else if (!(board[pos6.rank][pos6.file].PieceStatus==this.stat)) {
        mv6.setCapture(board[pos6.rank][pos6.file].piece);
        if (board[pos6.rank][pos6.file].piece.getName().equals("King")) {
          mv6.setCheck(mv6.fromCoord, generateAvenue(mv6.fromCoord, mv6.toCoord));
        }
        legalMoves.add(mv6);
      }
      else {
        mv6.setProtectionMove(pos6);
        legalMoves.add(mv6);
      }
    }
    Coordinate pos7 = new Coordinate(pos.file-2, pos.rank-1);
    Move mv7  = new Move(pos,pos7, "Knight",false,this.isWhite);
    if (pos7.rank>=0 && pos7.rank<8 && pos7.file>=0 && pos7.file<8) {
      if (board[pos7.rank][pos7.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv7);
      } else if (!(board[pos7.rank][pos7.file].PieceStatus==this.stat)) {
        mv7.setCapture(board[pos7.rank][pos7.file].piece);
        if (board[pos7.rank][pos7.file].piece.getName().equals("King")) {
          mv7.setCheck(mv7.fromCoord, generateAvenue(mv7.fromCoord, mv7.toCoord));
        }
        legalMoves.add(mv7);
      }
      else {
        mv7.setProtectionMove(pos7);
        legalMoves.add(mv7);
      }

    }
    Coordinate pos8 = new Coordinate(pos.file-1, pos.rank-2);
    Move mv8  = new Move(pos,pos8, "Knight",false,this.isWhite);
    if (pos8.rank>=0 && pos8.rank<8 && pos8.file>=0 && pos8.file<8)  {
      if (board[pos8.rank][pos8.file].PieceStatus==Status.EMPTY) {
        legalMoves.add(mv8);
      } else if (!(board[pos8.rank][pos8.file].PieceStatus==this.stat)) {
        mv8.setCapture(board[pos8.rank][pos8.file].piece);
        if (board[pos8.rank][pos8.file].piece.getName().equals("King")) {
          mv8.setCheck(mv8.fromCoord, generateAvenue(mv8.fromCoord, mv8.toCoord));
        }

        legalMoves.add(mv8);
      }else {
        mv8.setProtectionMove(pos8);
        legalMoves.add(mv8);
      }
    }
    if (legalMoves.size()==0) {
      legalMoves.add(new Move(false));
    }
    this.possibleMoves=legalMoves;
    return legalMoves;
  }
  public double evaluateSimple(Board board) {
    double eval = 3.05;
    Cell curCell = board.board[curPos.rank][curPos.file];
    //EVAL 1: IF THE PIECE IS PINNED CUT THE EVAL IN HALF
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
    //EVAL 3: INCREASE OR DECREASE BASED ON THE NUMBER OF POSSIBLE MOVES. (GRIM ON RIM and happy in center)
    double numMoves = GameService.filterMoves(possibleMoves).size();
    eval += (numMoves/8.0) - (5.0/8.0);

    //EVAL 4: IF THE PIECE IS AN OUTPOST, THEN INCREASE BY 1.
    if (isOutPost(board.board)) {
      eval += 1;
    }
    return eval;
  }
  public double evaluate(Board board) {
    double eval = 3.05;
    Cell curCell = board.board[curPos.rank][curPos.file];
    //EVAL 1: IF THE PIECE IS PINNED CUT THE EVAL IN HALF
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
    //EVAL 3: INCREASE OR DECREASE BASED ON THE NUMBER OF POSSIBLE MOVES. (GRIM ON RIM and happy in center)
    double numMoves = GameService.filterMoves(possibleMoves).size();
    eval += (numMoves/8.0) - (5.0/8.0);

    //EVAL 4: IF THE PIECE IS AN OUTPOST, THEN INCREASE BY 1.
    if (isOutPost(board.board)) {
      eval += 1;
    }

    //EVAL 4: Piece safety.
    eval *= evaluateSafety(curCell);
    //Add the forking value.
    eval += forkingValue;
    //Subtract the OverLoadingValue
    eval -= overLoadingValue;
    return eval;
  }

  public boolean isOutPost(Cell[][] board) {
    if (curPos.rank < 2 || curPos.rank > 5) {
      return false;
    }
    if (isWhite) {
      if (Board.countAlongFile(board, "Pawn", false, curPos.rank + 1, curPos.file + 1, true) == 0
          && Board.countAlongFile(board, "Pawn", false, curPos.rank + 1, curPos.file - 1, true) == 0) {
        return true;
      } else {
        return false;
      }
    } else {
      if (Board.countAlongFile(board, "Pawn", true, curPos.rank - 1, curPos.file + 1, false) == 0
          && Board.countAlongFile(board, "Pawn", true, curPos.rank - 1, curPos.file - 1, false) == 0) {
        return true;
      } else {
        return false;
      }
    }
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
      if (attackers.size() > protectors.size()) {
        return -1;
      } else {
        return -0.5;
      }
    }
    if (countByType(protectors, "Pawn") == 2) {
      return 1.25;
    }
    if (countByType(protectors, "Pawn") == 1){
      return 1.15;
    }
    //No pawns attacking or defending.
    if (countByType(protectors, "Knight") > countByType(attackers, "knight")) {
      if (attackers.size() > protectors.size()) {
        //Could give up a rook for two minor pieces.
        if (countByType(attackers, "Rook") > 0) {
          return 0.8;
        } else {
          return 1.1;
        }
      } else {
        return 1.15;
      }
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

  public boolean getColor() {
    return this.isWhite;
  }

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
    Knight copyPiece = new Knight(this.curPos, this.isWhite);
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
  public char getSymbol() {
    return 'N';
  }
  public void addCriticalAttack(Piece piece) {
    this.criticallyAttacking.add(piece);
    if (criticallyAttacking.size() > 1) {
      forkingValue = GameService.getSecondHighestValue(criticallyAttacking);
    }
  }
  public void addCriticalDefenence(Piece piece) {
    this.criticallyDefending.add(piece);
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
    return 3;
  }
  @Override
  public Coordinate getFromPos() {
    return this.fromPos;
  }

}
