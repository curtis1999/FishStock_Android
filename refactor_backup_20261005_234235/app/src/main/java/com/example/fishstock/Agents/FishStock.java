package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Cell;
import com.example.fishstock.Coordinate;
import com.example.fishstock.Move;
import com.example.fishstock.Pieces.*;
import com.example.fishstock.Status;

import java.util.ArrayList;


public class FishStock extends Agent{

  public FishStock(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }
  public Move getMove(Board ChessBoard, ArrayList<Move> posMoves, ArrayList<Move> advMoves) {
    double maxEval = -100;
    int maxIndex = 0;
    int index = 0;
    Piece capturedPiece = null;
    boolean justCaptured=false;
    Cell[][] copyBoard = new Cell[8][8];
    for (int i=0; i<8; i++) {
      for (int j=0; j<8;j++) {
        copyBoard[i][j]=Cell.copyCell(ChessBoard.board[i][j]);
      }
    }
    ArrayList<Piece> copyWhitesPieces = new ArrayList<Piece>();
    for (Piece p : ChessBoard.whitePieces) {
      copyWhitesPieces.add(p);
    }
    ArrayList<Piece> copyBlacksPieces = new ArrayList<Piece>();
    for (Piece p : ChessBoard.blackPieces) {
      copyBlacksPieces.add(p);
    }

    for (Move mv:posMoves) {
      if (mv.protectionMove || mv.coverMove) {
        index++;
        continue;
      }
      Cell oldCellTo = Cell.copyCell(ChessBoard.board[mv.toCoord.rank][mv.toCoord.file]);
      Cell oldCellFrom = Cell.copyCell(ChessBoard.board[mv.fromCoord.rank][mv.fromCoord.file]);
      if (isWhite) {
        if (!(ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].isEmpty)) {
          System.out.println("Could capture");
          int indexOfBlacksCapturedPiece = Board.getIndex(ChessBoard.blackPieces, mv.toCoord);
          capturedPiece=copyBlacksPieces.remove(indexOfBlacksCapturedPiece);
          justCaptured=true;
        }
      }else {
        if (!(ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].isEmpty)) {
          int indexOfWhitesCapturedPiece = Board.getIndex(ChessBoard.whitePieces, mv.toCoord);
          capturedPiece=copyWhitesPieces.remove(indexOfWhitesCapturedPiece);
          justCaptured=true;
        }
      }
      copyBoard[mv.toCoord.rank][mv.toCoord.file].putPiece(mv.piece);
      copyBoard[mv.fromCoord.rank][mv.fromCoord.file].empty();
      clearBoard(copyBoard);
      copyBoard = updateBoard(copyBoard, copyWhitesPieces, copyBlacksPieces, posMoves, advMoves);
      if (isWhite) {
        double curEval = evaluate(copyBoard,ChessBoard.whitePieces,ChessBoard.blackPieces, isWhite);
        mv.setEval(curEval);
        if (curEval>maxEval) {
          maxEval =curEval;
          maxIndex = index;
        }
      }else {
        double curEval = evaluate(copyBoard,ChessBoard.blackPieces,ChessBoard.whitePieces, isWhite);
        mv.setEval(curEval);
        if (curEval<maxEval) { //TODO: Should blacks eval be swapped.
          maxEval =curEval;
          maxIndex = index;
        }
      }
      copyBoard[mv.fromCoord.rank][mv.fromCoord.file] = Cell.copyCell(oldCellFrom);
      copyBoard[mv.toCoord.rank][mv.toCoord.file]=Cell.copyCell(oldCellTo); //Reset the copy board.
      if (capturedPiece!=null && justCaptured) {
        if (isWhite) {
          ChessBoard.blackPieces.add(capturedPiece); //Added
        }else {
          ChessBoard.whitePieces.add(capturedPiece);
        }
      }
      justCaptured=false;
      index++;
    }
    return posMoves.get(maxIndex);
  }

  public static double evaluate(Cell[][] board, ArrayList<Piece> ourPieces, ArrayList<Piece> advPieces, boolean isWhite) {
    double ourEval=0.0;
    double advEval=0.0;
    for (int i = 0; i<8; i++) {
      for (int j =0; j<8; j++) {
        if (board[i][j].PieceStatus== Status.EMPTY) {
          continue;
        }
        //The piece is one of FishStocks
        else if ((board[i][j].PieceStatus ==Status.WHITE && isWhite) || board[i][j].PieceStatus ==Status.BLACK && !isWhite) {
          Piece p= board[i][j].piece;
          if (board[i][j].piece.getName().equals("Pawn")) {
            double pawnEval=1.0;
            if (isWhite) {
              int numDefenders = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                pawnEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                pawnEval= pawnEval *1.2;
              }
            }else {
              int numDefenders = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                pawnEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                pawnEval= pawnEval *1.2;
              }
            }
            if (isCentralSquare(p.getPos())) {
              pawnEval = pawnEval*1.2;
            }
            ourEval+=pawnEval;
          }
          else if (p.getName().equals("Knight")) {
            double knightEval =3.0;
            if (isWhite) {
              int numDefenders = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                knightEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                knightEval= knightEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  knightEval = knightEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  knightEval = knightEval*1.2;
                }
              }
              if (isOutPost(p,board)) {
                knightEval = knightEval * 1.4;
              }
              //knightEval = knightEval * (p.getPossibleMoves().size()/5);
            }else {
              int numDefenders = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                knightEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                knightEval= knightEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  knightEval = knightEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  knightEval = knightEval*1.2;
                }
              }
              if (isOutPost(p,board)) {
                knightEval = knightEval * 1.4;
              }
              //knightEval = knightEval * (p.getPossibleMoves().size()/5);
            }
            ourEval +=knightEval;
          }
          else if (p.getName().equals("Bishop")) {
            double bishopEval = 3.0;
            if (isWhite) {
              int numDefenders = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                bishopEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                bishopEval= bishopEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  bishopEval = bishopEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  bishopEval = bishopEval*1.2;
                }
              }
              if (isLongDiagonal(p)) {
                bishopEval*=1.2;
              }
            }else {
              int numDefenders = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                bishopEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                bishopEval= bishopEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  bishopEval = bishopEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  bishopEval = bishopEval*1.2;
                }
              }
              if (isLongDiagonal(p)) {
                bishopEval*=1.2;
              }
            }
            ourEval+= bishopEval;
          }
          else if (p.getName().equals("Rook")) {
            double rookEval = 5.0;
            if (isWhite) {
              int numDefenders = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();//TODO: Error, counting itself as a defender?
              int numAttackers = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                rookEval =0;
              }
              if (numDefenders-numAttackers>1) { //overprotected.
                rookEval= rookEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  rookEval = rookEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  rookEval = rookEval*1.2;
                }
              }
              if (((Rook)p).isConnected) { //TODO: WHEN THE BOARD IS UPDATED, ENSURE THAT ROOKS GET LABELED AS CONNECTED IF THERE IS A PROTECTION MOVE FROM ROOK TO ROOK
                rookEval = rookEval*1.2;
              }
              if (p.getPos().rank==6) {	//Piggy on the 7th.
                rookEval = rookEval *1.4;
              }
            }else {
              int numDefenders = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                rookEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                rookEval= rookEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  rookEval = rookEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  rookEval = rookEval*1.2;
                }
              }
              if (((Rook)p).isConnected) { //TODO: WHEN THE BOARD IS UPDATED, ENSURE THAT ROOKS GET LABELED AS CONNECTED IF THERE IS A PROTECTION MOVE FROM ROOK TO ROOK
                rookEval = rookEval*1.2;
              }
              if (p.getPos().rank==1) {	//Piggy on the 2nd.
                rookEval = rookEval *1.4;
              }
            }
            ourEval+=rookEval;
          }
          else if (p.getName().equals("Queen")) {
            double queenEval=9.0;
            if (isWhite) {
              int numDefenders = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                queenEval =0;
              }
              if (numDefenders-numAttackers>1) {
                queenEval= queenEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  queenEval = queenEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  queenEval = queenEval*1.2;
                }
              }
            }else {
              int numDefenders = board[p.getPos().rank][p.getPos().file].blackAttackers.size();
              int numAttackers = board[p.getPos().rank][p.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                queenEval = 0;
              }
              if (numDefenders-numAttackers>1) {
                queenEval= queenEval *1.2;
              }
              for (Piece attackers:board[p.getPos().rank][p.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  queenEval = queenEval*0.1;
                }
              }
              for (Piece defenders:board[p.getPos().rank][p.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  queenEval = queenEval*1.2;
                }
              }
            }
            ourEval+=queenEval;
          }
        }

        //This piece is an adversary piece
        else if ((board[i][j].PieceStatus==Status.BLACK && isWhite) || board[i][j].PieceStatus==Status.WHITE && !isWhite) {
          Piece advP = board[i][j].piece;
          if (advP.getName().equals("Pawn")) {
            double pawnEval=1.0;
            if (!isWhite) {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                pawnEval = pawnEval*0.25;
              }
              if (numDefenders-numAttackers>1) {
                pawnEval= pawnEval *1.2;
              }
            }else {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                pawnEval = pawnEval*0.25;
              }
              if (numDefenders-numAttackers>1) {
                pawnEval= pawnEval *1.2;
              }
            }
            if (isCentralSquare(advP.getPos())) {
              pawnEval = pawnEval*1.2;
            }
            advEval+=pawnEval;
          }
          else if (advP.getName().equals("Knight")) {
            double knightEval =3.0;
            if (!isWhite) {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                knightEval = knightEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                knightEval= knightEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  knightEval = knightEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  knightEval = knightEval*1.2;
                }
              }
              if (isOutPost(advP,board)) {
                knightEval = knightEval * 1.4;
              }
              //knightEval = knightEval * (p.getPossibleMoves().size()/5);
            }else {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                knightEval = knightEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                knightEval= knightEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  knightEval = knightEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  knightEval = knightEval*1.2;
                }
              }
              if (isOutPost(advP,board)) {
                knightEval = knightEval * 1.4;
              }
              //knightEval = knightEval * (p.getPossibleMoves().size()/5);
            }
            advEval +=knightEval;
          }
          else if (advP.getName().equals("Bishop")) {
            double bishopEval = 3.0;
            if (!isWhite) {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                bishopEval = bishopEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                bishopEval= bishopEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  bishopEval = bishopEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  bishopEval = bishopEval*1.2;
                }
              }
              if (isLongDiagonal(advP)) {
                bishopEval*=1.2;
              }
            }else {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                bishopEval = bishopEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                bishopEval= bishopEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  bishopEval = bishopEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  bishopEval = bishopEval*1.2;
                }
              }
              if (isLongDiagonal(advP)) {
                bishopEval*=1.2;
              }
            }
            advEval+= bishopEval;
          }
          else if (advP.getName().equals("Rook")) {
            double rookEval = 5.0;
            if (!isWhite) {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                rookEval =rookEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                rookEval= rookEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  rookEval = rookEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  rookEval = rookEval*1.2;
                }
              }
              if (((Rook)advP).isConnected) { //TODO: WHEN THE BOARD IS UPDATED, ENSURE THAT ROOKS GET LABELED AS CONNECTED IF THERE IS A PROTECTION MOVE FROM ROOK TO ROOK
                rookEval = rookEval*1.2;
              }
              if (advP.getPos().rank==6) {	//Piggy on the 7th.
                rookEval = rookEval *1.4;
              }
            }else {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                rookEval = rookEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                rookEval= rookEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  rookEval = rookEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  rookEval = rookEval*1.2;
                }
              }
              if (((Rook)advP).isConnected) { //TODO: WHEN THE BOARD IS UPDATED, ENSURE THAT ROOKS GET LABELED AS CONNECTED IF THERE IS A PROTECTION MOVE FROM ROOK TO ROOK
                rookEval = rookEval*1.2;
              }
              if (advP.getPos().rank==1) {	//Piggy on the 2nd.
                rookEval = rookEval *1.4;
              }
            }
            advEval+=rookEval;
          }
          else if (advP.getName().equals("Queen")) {
            double queenEval=9.0;
            if (!isWhite) {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                queenEval =queenEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                queenEval= queenEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  queenEval = queenEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  queenEval = queenEval*1.2;
                }
              }
            }else {
              int numDefenders = board[advP.getPos().rank][advP.getPos().file].blackAttackers.size();
              int numAttackers = board[advP.getPos().rank][advP.getPos().file].whiteAttackers.size();
              if (numAttackers>numDefenders) {	//Can be captured next turn.
                queenEval = queenEval*0.1;
              }
              if (numDefenders-numAttackers>1) {
                queenEval= queenEval *1.2;
              }
              for (Piece attackers:board[advP.getPos().rank][advP.getPos().file].whiteAttackers) {	//Can be captured by a pawn.
                if (attackers.getName().equals("Pawn")) {
                  queenEval = queenEval*0.1;
                }
              }
              for (Piece defenders:board[advP.getPos().rank][advP.getPos().file].blackAttackers) {	//Is protected by a pawn
                if (defenders.getName().equals("Pawn")) {
                  queenEval = queenEval*1.2;
                }
              }
            }
            advEval+=queenEval;
          }
        }
      }

    }
    return ourEval - advEval;
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
  public static boolean isOutPost(Piece p, Cell[][] board) {
    return false;
  }
  public static boolean isLongDiagonal(Piece p) {
    return false;
  }

  //Empties the old list of attackers
  public static Cell[][] clearBoard(Cell[][] ChessBoard){
    for (int i=0; i<8; i++) {
      for (int j=0; j<8; j++) {
        ChessBoard[i][j].blackAttackers = new ArrayList<Piece>();
        ChessBoard[i][j].whiteAttackers = new ArrayList<Piece>();
      }
    }
    return ChessBoard;
  }
  //Updates the board and the Piece arrays.  Each Cell has an list of the white pieces and black pieces attacking that square.
  public static Cell[][] updateBoard(Cell[][] board, ArrayList<Piece> ourPieces, ArrayList<Piece> advPieces, ArrayList<Move> whiteMoves, ArrayList<Move>blackMoves){
    for (Move mv:whiteMoves) {
      //Skip these moves
      if (mv.piece==null || (mv.piece.getName().equals("Pawn")&& !(mv.coverMove || mv.protectionMove ||mv.isCapture))) {
        continue;
        //Extend the checking avenue one beyond the king so that that square is not available on black next Move
      }else if (mv.isCheck) {
        if (mv.piece.getName().equals("Rook")||mv.piece.getName().equals("Queen")) {
          //Check is along the same file
          if (mv.fromCoord.file==advPieces.get(0).getPos().file) {
            if (mv.fromCoord.rank>advPieces.get(0).getPos().rank) {
              if (advPieces.get(0).getPos().rank>0) {
                board[advPieces.get(0).getPos().rank-1][advPieces.get(0).getPos().file].setXRay();
              }
            }else {
              if (advPieces.get(0).getPos().rank<7) {
                board[advPieces.get(0).getPos().rank+1][advPieces.get(0).getPos().file].setXRay();

              }
            }
          }else if (mv.fromCoord.rank==advPieces.get(0).getPos().rank) {
            if (mv.fromCoord.file>advPieces.get(0).getPos().file) {
              if (advPieces.get(0).getPos().file>0) {
                board[advPieces.get(0).getPos().rank][advPieces.get(0).getPos().file-1].setXRay();
              }
            }else {
              if (advPieces.get(0).getPos().file<7) {
                board[advPieces.get(0).getPos().rank][advPieces.get(0).getPos().file+1].setXRay();
              }
            }
          }
        }
      } else if (mv.protectionMove) {
        board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
        if (board[mv.toCoord.rank][mv.toCoord.file].piece!=null) {
          board[mv.toCoord.rank][mv.toCoord.file].piece.addProtector(mv.piece);
        }
        continue;
      }else if (mv.isCapture){
        board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
        board[mv.toCoord.rank][mv.toCoord.file].piece.addAttacker(mv.piece);
        if (board[mv.toCoord.rank][mv.toCoord.file].piece.getColor()) {
          int index = Board.getIndex(ourPieces, mv.toCoord);
          if (index!=-1) {
            ourPieces.get(index).addAttacker(mv.piece);
          }
        }else {
          int index = Board.getIndex(advPieces, mv.toCoord);
          if (index==-1) {
            continue;
          }
          advPieces.get(index).addAttacker(mv.piece);
        }

      }else {
        board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
      }

    }
    for (Move mv: blackMoves) {
      if (mv.piece==null || (mv.piece.getName().equals("Pawn")&& !(mv.coverMove||mv.protectionMove||mv.isCapture))) {
        continue;
      }else if (mv.isCheck) {
        if (mv.piece.getName().equals("Rook")||mv.piece.getName().equals("Queen")) {
          //Check is along the same file
          if (mv.fromCoord.file==ourPieces.get(0).getPos().file) {
            if (mv.fromCoord.rank>ourPieces.get(0).getPos().rank) {
              if (ourPieces.get(0).getPos().rank>0) {
                board[ourPieces.get(0).getPos().rank-1][ourPieces.get(0).getPos().file].setXRay();
              }
            }else {
              if (ourPieces.get(0).getPos().rank<7) {
                board[ourPieces.get(0).getPos().rank+1][ourPieces.get(0).getPos().file].setXRay();
              }
            }
          }else if (mv.fromCoord.rank==ourPieces.get(0).getPos().rank) {
            if (mv.fromCoord.file>ourPieces.get(0).getPos().file) {
              if (ourPieces.get(0).getPos().file>0) {
                board[ourPieces.get(0).getPos().rank][ourPieces.get(0).getPos().file-1].setXRay();
              }
            }else {
              if (ourPieces.get(0).getPos().file<7) {
                board[ourPieces.get(0).getPos().rank][ourPieces.get(0).getPos().file+1].setXRay();
              }
            }
          }
        }
      }
      else if (mv.protectionMove) {
        board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
        Piece protectee = board[mv.toCoord.rank][mv.toCoord.file].piece;
        protectee.addProtector(mv.piece);
        continue;
      }else if (mv.isCapture){
        board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
        if (board[mv.toCoord.rank][mv.toCoord.file].piece != null) {
          board[mv.toCoord.rank][mv.toCoord.file].piece.addAttacker(mv.piece);
        }
        Piece attacked = board[mv.toCoord.rank][mv.toCoord.file].piece;
        if (attacked!=null) {
          attacked.addAttacker(mv.piece);

          if (attacked.getColor()) {
            int index = Board.getIndex(ourPieces, mv.toCoord);
            if (index==-1) {	//TODO: CAUSE INDEX OUT OF BOUNDS BECAUSE INDEX WAS -1
              continue;
            }
            ourPieces.get(index).addAttacker(mv.piece);

          }else {
            int index = Board.getIndex(advPieces, mv.toCoord);
            advPieces.get(index).addAttacker(mv.piece);
          }
        }

      }else {
        board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
      }
    }

    return board;
  }
  public String getName() {return "FishStock"; }
}
