package com.example.fishstock.Agents;

import com.example.fishstock.Board;
import com.example.fishstock.Move;
import java.util.ArrayList;

public class Human extends Agent{

  public Human(AgentType type, boolean isWhite) {
    super(type, isWhite);
  }

  public Move getMove(Board ChessBoard, ArrayList<Move> possibleMoves, ArrayList<Move> possibleMovesAdv){
    return null;
  }

  /**
   * System.out.println("");
   *       System.out.println("White: Input your Move:");
   *       Scanner whiteMove = new Scanner(System.in);
   *       String pieceName = whiteMove.next(); //First Word inputed by the user should be the piece name
   *       //HANDLING AN UNDO./////////////////////////////////////////
   *       if (pieceName.equalsIgnoreCase("undo")) {
   *         if (moveNum==1) {
   *           System.out.println("Cannot undo on the first Move doofus");
   *         }
   *         System.out.println("undoing the last moves.");
   *         int lastIndex = whitesMoves.size()-1;
   *         Move badMoveW = whitesMoves.get(lastIndex);
   *         Move badMoveB = blacksMoves.get(lastIndex);
   *         //undoing blacks last Move
   *         ChessBoard.board[badMoveB.fromCoord.rank][badMoveB.fromCoord.file].putPiece(badMoveB.piece);
   *         if (badMoveB.isCapture) {
   *           ChessBoard.board[badMoveB.toCoord.rank][badMoveB.toCoord.file].putPiece(badMoveB.getCapturable());
   *           ChessBoard.whitePieces.add(badMoveB.getCapturable());
   *         }else {
   *           ChessBoard.board[badMoveB.toCoord.rank][badMoveB.toCoord.file].empty();
   *         }
   *         int undoingIndex = Board.getIndex(ChessBoard.blackPieces, badMoveB.toCoord);
   *         if (undoingIndex==-1) {
   *           System.out.println("Wrong index");
   *         }
   *         if (badMoveB.piece.getName().equals("Pawn")) {
   *           if (badMoveB.fromCoord.rank-badMoveB.toCoord.rank==2) {
   *             ((Pawn)ChessBoard.blackPieces.get(undoingIndex)).unGrow();
   *           }
   *         }
   *         ChessBoard.blackPieces.get(undoingIndex).setPos(badMoveB.fromCoord);
   *         whitesMoves.remove(lastIndex);
   *         blacksMoves.remove(lastIndex);
   *
   *         //undoing whites last Move.
   *         ChessBoard.board[badMoveW.fromCoord.rank][badMoveW.fromCoord.file].putPiece(badMoveW.piece);
   *         if (badMoveW.isCapture) {
   *           ChessBoard.board[badMoveW.toCoord.rank][badMoveW.toCoord.file].putPiece(badMoveW.getCapturable());
   *           ChessBoard.blackPieces.add(badMoveW.getCapturable());
   *         }else {
   *           ChessBoard.board[badMoveW.toCoord.rank][badMoveW.toCoord.file].empty();
   *         }
   *         int undoingIndex2 = Board.getIndex(ChessBoard.whitePieces, badMoveW.toCoord);
   *         if (undoingIndex2==-1) {
   *           System.out.println("Wrong index");
   *         }
   *         ChessBoard.whitePieces.get(undoingIndex2).setPos(badMoveW.fromCoord);
   *         if (badMoveW.piece.getName().equals("Pawn") && ((badMoveW.toCoord.rank-badMoveW.fromCoord.rank)==2)){
   *           ((Pawn)badMoveW.piece).unGrow();
   *           ((Pawn)ChessBoard.whitePieces.get(undoingIndex2)).unGrow();
   *         }
   *         ChessBoard.printBoard(ChessBoard, whitesPOV);
   *         whitesPotentialMoves = generateMoves(ChessBoard.whitePieces,ChessBoard.blackPieces, ChessBoard.board,ChessBoard.blackPieces.get(0).getPos(), ChessBoard);
   *         blacksPotentialMoves = generateMoves(ChessBoard.blackPieces,ChessBoard.whitePieces,ChessBoard.board,ChessBoard.whitePieces.get(0).getPos(),ChessBoard);
   *         ChessBoard.board = updateBoardMeta(ChessBoard,whitesPotentialMoves,blacksPotentialMoves); 	//Updates the board so that each cell has number of white and black attackers and each piece has its numbner of attackers and defenders
   *         continue;
   *         //Not an undo
   *       }else {
   *         String fromFileName = whiteMove.next();
   *         String fromRankName = whiteMove.next();
   *         String fileName = whiteMove.next();
   *         String rankName = whiteMove.next();
   *         int fromFile = getFile(fromFileName);
   *         int fromRank = Integer.parseInt(fromRankName)-1;
   *         int file = getFile(fileName);
   *         int rank = Integer.parseInt(rankName)-1;
   *         if (file==-1 || !(0<=rank && rank<=8)) {
   *           System.out.println("invalid Move");
   *         }
   *         Coordinate fromCrd = new Coordinate(fromFile,fromRank);
   *         Coordinate crd = new Coordinate(file, rank);
   *         Move mewv  = new Move(fromCrd,crd,pieceName,false,true);
   *         if (mewv.piece==null) {
   *           System.out.println("Null Piece");
   *         }
   *
   *         //Loop through the potential moves.  If it finds a Move that matches it updates the board.  If not then the Move must have been invalid.
   *         for (Move mv1 : whitesPotentialMoves) {
   *           if (mv1.coverMove || mv1.protectionMove) {
   *             continue;
   *           }
   *           if (mv1.equals(mewv)){
   *             valid=true;
   *             curMove=mv1;
   *           }
   *         }
   *         if (!valid) {
   *           System.out.println("Invalid Move.  Please try again");
   *           continue;
   *         }break;
   *       }
   *     }
   */
  public String getName() {
    return "Human";
  }
}
