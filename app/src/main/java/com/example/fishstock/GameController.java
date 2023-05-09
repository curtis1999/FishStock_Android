package com.example.fishstock;

import com.example.fishstock.Agents.Agent;
import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Agents.FishStock;
import com.example.fishstock.Agents.Human;
import com.example.fishstock.Agents.Randy;
import com.example.fishstock.Pieces.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GameController {
  //Game game;

  public static void main(String[] args) throws CloneNotSupportedException {
    /////////////////////WELCOME MESSAGE/////////////////////////////////
    System.out.println(
        "Welcome to FishStock Alpha: Input a command in the following format to start the game:");
    System.out.println("[Player1Name][Player2Name][POV]");
    System.out.println("Options for Player names are: Human/Random/FishStock");
    System.out.println("Options for POV are: White/Black");
    Scanner config = new Scanner(System.in);
    String player1 = config.next();
    String player2 = config.next();
    String POV = config.next();
    Agent whiteAgent = getAgent(player1, true);
    Agent blackAgent = getAgent(player2, false);
    boolean whitesPOV = POV.equalsIgnoreCase("white");
    ////////////INITIALIZE FIELDS//////////////////////////
    Board board = new Board();
    Game game = new Game(board, whiteAgent.type, blackAgent.type);
    Board ChessBoard = game.ChessBoard;
    ArrayList<Move> whitesPotentialMoves;
    ArrayList<Move> blacksPotentialMoves;
    ArrayList<Move> whitesMoves = new ArrayList<>();
    ArrayList<Move> blacksMoves = new ArrayList<>();
    List<Board> boardStates = new ArrayList<>();
    int moveNum = game.moveNum;
    ////////////////////////INITIALIZING THE BOARD'S META INFORMATION///////////////////////////////////////////////////

    Board.printBoard(ChessBoard, whitesPOV);
    whitesPotentialMoves =
        generateMoves(ChessBoard, true);
    blacksPotentialMoves =
        generateMoves(ChessBoard, false);
    ChessBoard = updateBoardMeta(ChessBoard);    //Updates the board so that each cell has number of white and black attackers and each piece has its numbner of attackers and defenders
    Move curMove;
    Agent curAgent = whiteAgent;
    //GAME LOOP///////////////////////////////////////////////////////////////////////////////////////////////

    while (true) {
      if (curAgent.isWhite) {
        //1. Get the Move from the Current Agent
        curMove = curAgent.getMove(ChessBoard, whitesPotentialMoves, blacksPotentialMoves);
        whitesMoves.add(curMove);
      } else {
        curMove = curAgent.getMove(ChessBoard, blacksPotentialMoves, whitesPotentialMoves);
        blacksMoves.add(curMove);
      }

      //3. Make the Move.
      makeMove(ChessBoard, curMove, curAgent.isWhite);
      if (curAgent.isWhite) {
        game.whitesMovesLog.add(curMove);
      } else {
        game.blacksMovesLog.add(curMove);
      }
      boardStates.add(copyBoard(board));

      //4. Update the board meta. (Pins, Reveal, Checks)
      updateBoardMeta(ChessBoard);

      //5. Check for a dead position (insufficient material)
      if (isDeadPosition(board.whitePieces, board.blackPieces)) {
        System.out.println("DRAW BY INSUFFICIENT MATERIAL");
        Board.printBoard(board, true);
        break;
      }
      if (isRepetition(boardStates, ChessBoard)) {
        System.out.println("DRAW BY REPETITION");
        Board.printBoard(board, true);
        break;
      }

      //6. Update the possible Moves
      if (curAgent.isWhite) {
          blacksPotentialMoves = generateMoves(ChessBoard, false);

        if (((King)ChessBoard.blackPieces.get(0)).isDoubleChecked) {
          System.out.println("DOUBLECHECK!!");
          blacksPotentialMoves = generateMovesDoubleCheck(ChessBoard, blacksPotentialMoves, false);
          if (blacksPotentialMoves.size() == 0) {
            System.out.println("CHECKMATE! WHITE WINS");
            Board.printBoard(ChessBoard, whitesPOV);
            break;
          }
        } else if (((King)ChessBoard.blackPieces.get(0)).isChecked) {
          System.out.println("CHECK!");
          blacksPotentialMoves = generateMovesCheck(ChessBoard, blacksPotentialMoves, false);
          if (blacksPotentialMoves.size() == 0) {
            System.out.println("CHECKMATE! WHITE WINS");
            Board.printBoard(ChessBoard, whitesPOV);
            break;
          }
        }
        if (blacksPotentialMoves.size() == 0) {
          System.out.println("STALEMATE! THE GAME ENDS IN A DRAW");
          Board.printBoard(ChessBoard, whitesPOV);
          break;
        }
        //BLack just played.
      } else {
        whitesPotentialMoves = generateMoves(ChessBoard, true);
        if (((King)ChessBoard.whitePieces.get(0)).isDoubleChecked) {
          System.out.println("DOUBLECHECK!!");
          whitesPotentialMoves = generateMovesDoubleCheck(ChessBoard, whitesPotentialMoves, true);
          if (whitesPotentialMoves.size() == 0) {
            System.out.println("CHECKMATE! BLACK WINS");
            Board.printBoard(ChessBoard, whitesPOV);
            break;
          }
        } else if (((King)ChessBoard.whitePieces.get(0)).isChecked) {
          System.out.println("CHECK!");
          whitesPotentialMoves = generateMovesCheck(ChessBoard, whitesPotentialMoves, true);
          if (whitesPotentialMoves.size() == 0) {
            System.out.println("CHECKMATE! BLACK WINS");
            Board.printBoard(ChessBoard, whitesPOV);
            break;
          }
        }
        if (whitesPotentialMoves.size() == 0) {
          System.out.println("STALEMATE! THE GAME ENDS IN A DRAW");
          Board.printBoard(ChessBoard, whitesPOV);
          break;
        }
      }

      //7. Changes the current Player
      if (curAgent.isWhite) {
        curAgent = blackAgent;
      } else {
        curAgent = whiteAgent;
      }
      Board.printBoard(ChessBoard, whitesPOV);
      System.out.println(moveNum);
      if (curAgent.isWhite) { moveNum++; }
    }
    System.out.println("GG");
  }


  public static boolean isRepetition(List<Board> boardStates, Board board) {
    int numRepetitions = 0;
    for (Board curBoard : boardStates) {
      if (Board.compareBoard(board, curBoard)) {
        numRepetitions ++;
      }
    }
    return numRepetitions >= 3;

  }

  /**
   * Updates the board given a Move.
   *
   * @param ChessBoard
   * @param curMove
   * @param isWhite
   */
  public static void makeMove(Board ChessBoard, Move curMove, boolean isWhite) {
    //The id of the piece being moved this turn.
    int pieceIndex = 0;
    if (isWhite) {
      pieceIndex = Board.getIndex(ChessBoard.whitePieces, curMove.fromCoord);
    } else {
      pieceIndex = Board.getIndex(ChessBoard.blackPieces, curMove.fromCoord);
    }

    //1. Updates the board position of the moved piece.
    ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].putPiece(curMove.piece);
    ChessBoard.board[curMove.fromCoord.rank][curMove.fromCoord.file].empty();
    if (isWhite) {
      ChessBoard.whitePieces.get(pieceIndex).setPos(curMove.toCoord);
      ((King)ChessBoard.whitePieces.get(0)).unCheck(); //NOTE: I UNCHECKED THE KING'S HERE
    } else {
      ChessBoard.blackPieces.get(pieceIndex).setPos(curMove.toCoord);
      ((King)ChessBoard.blackPieces.get(0)).unCheck();
    }
    //2. If there is a capture in the move, then remove the adversary piece.
    if (curMove.isCapture) {
      if (isWhite) {
        int indexOfBlacksCapturedPiece = Board.getIndex(ChessBoard.blackPieces, curMove.capturablePiece.getPos());
        if (indexOfBlacksCapturedPiece < 0) {
          int a=0;
        }
        ChessBoard.blackPieces.remove(indexOfBlacksCapturedPiece);
        //If the move is EnPassant, then the piece must be manually removed from the board.
        if (curMove.isEnPassant) {
          ChessBoard.board[curMove.capturablePiece.getPos().rank][curMove.capturablePiece.getPos().file].empty();
        }
      } else {
        int indexOfWhitesCapturedPiece = Board.getIndex(ChessBoard.whitePieces, curMove.capturablePiece.getPos());
        if (indexOfWhitesCapturedPiece < 0) {
          int a=0;
        }
        ChessBoard.whitePieces.remove(indexOfWhitesCapturedPiece);
        if (curMove.isEnPassant) {
          ChessBoard.board[curMove.capturablePiece.getPos().rank][curMove.capturablePiece.getPos().file].empty();
        }
      }
    }

    //4. Castle.
    if (curMove.isCastle) {
      //4.1 Short Castle
      System.out.println("Castling");
        if (curMove.toCoord.file == 1) {
          if (isWhite) {
          ((King) ChessBoard.whitePieces.get(0)).moved(); //Cannot castle again.
          int indexRook = Board.getIndex(ChessBoard.whitePieces, new Coordinate(0, 0));
          ChessBoard.whitePieces.get(indexRook).setPos(new Coordinate(2, 0));
          ChessBoard.board[0][2].putRook(true, ChessBoard.whitePieces.get(indexRook));
          ChessBoard.board[0][0].empty();
          }
          else {
            ((King) ChessBoard.blackPieces.get(0)).moved(); //Cannot castle again.
            int indexRook = Board.getIndex(ChessBoard.blackPieces, new Coordinate(0, 7));
            ChessBoard.blackPieces.get(indexRook).setPos(new Coordinate(2, 7));
            ChessBoard.board[7][2].putRook(true, ChessBoard.blackPieces.get(indexRook));
            ChessBoard.board[7][0].empty();
          }
      }
          //4.2: Long Castle
          else if (curMove.toCoord.file == 5) {
            if (isWhite){
              ((King) ChessBoard.whitePieces.get(0)).moved(); //Cannot castle again.
              int indexRook = Board.getIndex(ChessBoard.whitePieces, new Coordinate(7, 0));

              ChessBoard.whitePieces.get(indexRook).setPos(new Coordinate(4, 0));
              ChessBoard.board[0][4].putRook(true, ChessBoard.whitePieces.get(indexRook));
              ChessBoard.board[0][7].empty();
            }
            else {
              ((King) ChessBoard.blackPieces.get(0)).moved(); //Cannot castle again.
              int indexRook = Board.getIndex(ChessBoard.blackPieces, new Coordinate(7, 7));

              ChessBoard.blackPieces.get(indexRook).setPos(new Coordinate(4, 7));
              ChessBoard.board[7][4].putRook(true, ChessBoard.blackPieces.get(indexRook));
              ChessBoard.board[7][7].empty();
            }
          }
      }
    //5. If the current move is a promotion.
      if (curMove.isPromotion) {
        int promotionIndex;
        if (isWhite) {
          promotionIndex = Board.getIndex(ChessBoard.whitePieces, curMove.toCoord);
          ChessBoard.whitePieces.remove(promotionIndex);
          curMove.promotionPiece.setPos(curMove.toCoord);
          ChessBoard.whitePieces.add(curMove.promotionPiece);

        } else {
          promotionIndex = Board.getIndex(ChessBoard.blackPieces, curMove.toCoord);
          ChessBoard.blackPieces.remove(promotionIndex);
          curMove.promotionPiece.setPos(curMove.toCoord);
          ChessBoard.blackPieces.add(curMove.promotionPiece);
        }
        ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].piece = curMove.promotionPiece; //TODO: May be redundant
        ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].Symbol = curMove.promotionPiece.getSymbol();
      }

       //6. Final Updates.
      ((King)ChessBoard.blackPieces.get(0)).unCheck();
      if (curMove.piece.getName().equals("Pawn") && !curMove.isPromotion) {
        ((Pawn) ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].piece).growUp();
        if (isWhite) {
          ((Pawn) ChessBoard.whitePieces.get(pieceIndex)).growUp();
        } else {
          ((Pawn) ChessBoard.blackPieces.get(pieceIndex)).growUp();
        }
        if (curMove.toCoord.rank - curMove.fromCoord.rank == 2 || curMove.toCoord.rank - curMove.fromCoord.rank == -2 && curMove.piece.getName().equals("Pawn")) {
            ((Pawn) ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].piece).setEnPassantable();
            if (isWhite) {
              ((Pawn) ChessBoard.whitePieces.get(pieceIndex)).setEnPassantable();
            } else {
              ((Pawn) ChessBoard.blackPieces.get(pieceIndex)).setEnPassantable();
            }
        }
      }
    //King or rook moves then castling rights are revoked
    if (curMove.piece.getName().equals("Rook")) {
      ((Rook) ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].piece).moved();
    }
    if (curMove.piece.getName().equals("King")) {
      ((King) ChessBoard.board[curMove.toCoord.rank][curMove.toCoord.file].piece).moved();
      if (isWhite) {
        ((King) ChessBoard.whitePieces.get(pieceIndex)).moved();
      } else {
        ((King) ChessBoard.blackPieces.get(pieceIndex)).moved();
      }
    }
  }

  /**
   * Generates all the possible moves if you are not in check.
   *
   * @param ChessBoard The Board.
   * @return The List of all valid moves
   * @throws CloneNotSupportedException
   */
  public static ArrayList<Move> generateMoves(Board ChessBoard, boolean isWhite) throws CloneNotSupportedException{
    ArrayList<Move> moves = new ArrayList<>();
    if (isWhite) {
      for (Piece piece : ChessBoard.whitePieces) {
        if (!piece.getPin()) {
          ArrayList<Move> filteredMoves = filterMoves(piece.generateMoves(piece.getPos(), ChessBoard.board));
          for (Move aMove : filteredMoves) {
            if (aMove.isPromotion) {
              ArrayList<Move> promotionMoves = new ArrayList<>();
              Move knightPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, true);
              if (aMove.isCapture){
                knightPromotion.setCapture(aMove.capturablePiece);
              }
              knightPromotion.setPromotion(new Knight(aMove.fromCoord, true));
              Move BishopPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, true);
              BishopPromotion.setPromotion(new Bishop(aMove.toCoord, true));
              if (aMove.isCapture){
                BishopPromotion.setCapture(aMove.capturablePiece);
              }
              Move RookPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, true);
              RookPromotion.setPromotion(new Rook(aMove.fromCoord, true));
              if (aMove.isCapture){
                RookPromotion.setCapture(aMove.capturablePiece);
              }
              Move QueenPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, true);
              QueenPromotion.setPromotion(new Queen(aMove.fromCoord, true));
              if (aMove.isCapture){
                QueenPromotion.setCapture(aMove.capturablePiece);
              }
              promotionMoves.add(knightPromotion);
              promotionMoves.add(BishopPromotion);
              promotionMoves.add(RookPromotion);
              promotionMoves.add(QueenPromotion);
              moves.addAll(promotionMoves);
            } else {
              moves.add(aMove);
            }
          }
        }
      }
    } else {
      for (Piece piece : ChessBoard.blackPieces) {
        if (!piece.getPin()) {
          ArrayList<Move> filteredMoves = filterMoves(piece.generateMoves(piece.getPos(), ChessBoard.board));
          for (Move aMove : filteredMoves) {
            if (aMove.isPromotion) {
              ArrayList<Move> promotionMoves = new ArrayList<>();
              Move knightPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, false);
              knightPromotion.setPromotion(new Knight(aMove.toCoord, false));
              if (aMove.isCapture){
                knightPromotion.setCapture(aMove.capturablePiece);
              }
              Move BishopPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, false);
              BishopPromotion.setPromotion(new Bishop(aMove.toCoord, false));
              if (aMove.isCapture){
                BishopPromotion.setCapture(aMove.capturablePiece);
              }
              Move RookPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, false);
              RookPromotion.setPromotion(new Rook(aMove.fromCoord, false));
              if (aMove.isCapture){
                RookPromotion.setCapture(aMove.capturablePiece);
              }
              Move QueenPromotion = new Move(aMove.fromCoord, aMove.toCoord,"Pawn", aMove.isCapture, false);
              QueenPromotion.setPromotion(new Queen(aMove.fromCoord, false));
              if (aMove.isCapture){
                QueenPromotion.setCapture(aMove.capturablePiece);
              }
              promotionMoves.add(knightPromotion);
              promotionMoves.add(BishopPromotion);
              promotionMoves.add(RookPromotion);
              promotionMoves.add(QueenPromotion);
              moves.addAll(promotionMoves);
            } else {
              moves.add(aMove);
            }
          }
        }
      }
    }
    return moves;
  }

  /**
   * Generates the possible moves if the player's king is double checked.  (Only king moves)
   * @param chessBoard
   * @param possibleMoves
   */
  public static ArrayList<Move> generateMovesDoubleCheck(Board chessBoard, ArrayList<Move> possibleMoves, boolean isWhite) {
    ArrayList<Move> possibleMovesCheck = new ArrayList<>();
    ArrayList<Coordinate> checkingAve1;
    if (isWhite){
      checkingAve1 = ((King)chessBoard.whitePieces.get(0)).getCheckingAve();
    } else {
      checkingAve1 = ((King)chessBoard.blackPieces.get(0)).getCheckingAve();
    }
    ArrayList<Coordinate> checkingAve2 = null; //TODO:!!!
    boolean inCheckingAve = false;
    for (Move mv : possibleMoves) {
      if (mv.piece.getName().equals("King")) {
        if (mv.piece.getColor()) {
          if (chessBoard.board[mv.toCoord.rank][mv.toCoord.file].blackAttackers.size()==0) {
            for (Coordinate coord : checkingAve1){
              if (mv.toCoord.rank == coord.rank && mv.toCoord.file == coord.file) {
                inCheckingAve = true;
                break;
              }
            }
            if (!inCheckingAve) {
              possibleMovesCheck.add(mv);
            }
          }
        } else {
          if (chessBoard.board[mv.toCoord.rank][mv.toCoord.file].whiteAttackers.size()==0) {
            for (Coordinate coord : checkingAve1){
              if (mv.toCoord.rank == coord.rank && mv.toCoord.file == coord.file) {
                inCheckingAve = true;
                break;
              }
            }
            if (!inCheckingAve) {
              possibleMovesCheck.add(mv);
            }
          }
        }
      }else {
        continue;
      }
    }
    return possibleMovesCheck;
  }


  /**
   * Generates all the possible moves if the player is in check. (Take the checking piece, block the checking avenue or Move the king)
   * @param chessBoard
   * @param possibleMoves
   * @return
   */
  public static ArrayList<Move> generateMovesCheck(Board chessBoard, ArrayList<Move> possibleMoves, boolean isWhite){
    Coordinate checkerLoc;
    Piece checkingPiece;
    Coordinate kingLoc;
    //PART 1: GET CHECKING INFO
    if (isWhite) {
      checkerLoc = ((King)chessBoard.whitePieces.get(0)).checkerLoc;
      checkingPiece = chessBoard.board[checkerLoc.rank][checkerLoc.file].piece;
      if (checkingPiece == null) {
        checkingPiece = chessBoard.board[checkerLoc.rank][checkerLoc.file].piece;
      }
      kingLoc = new Coordinate(chessBoard.whitePieces.get(0).getPos().file, chessBoard.whitePieces.get(0).getPos().rank);
    } else {
      checkerLoc = ((King)chessBoard.blackPieces.get(0)).checkerLoc;
      checkingPiece = chessBoard.board[checkerLoc.rank][checkerLoc.file].piece;
      if (checkingPiece == null) {
        int a = 1;
      }
      kingLoc = new Coordinate(chessBoard.blackPieces.get(0).getPos().file, chessBoard.blackPieces.get(0).getPos().rank);
    }
    ArrayList<Move> possibleMovesCheck = new ArrayList<Move>();
    ArrayList<Coordinate> CheckingAvenue = getCheckingAvenue(checkingPiece,checkerLoc, kingLoc);

    //PART 2: ITERATE AND FILTER THE MOVES.
    for (Move mv : possibleMoves) {
      if (mv.piece.getName().equals("King")) {
        if (isWhite) {
          if (chessBoard.board[mv.toCoord.rank][mv.toCoord.file].blackAttackers.size() > 0) {
            continue;
          }
        } else {
          if (chessBoard.board[mv.toCoord.rank][mv.toCoord.file].whiteAttackers.size() > 0) {
            continue;
          }
        }
          //PART 1: IF THE KING STEPS OUT OF THE CHECKING AVENUE.
          boolean awayFromCheck=true;
          //1.1: The King moves off of the checking avenue.
          for (Coordinate checkCell: CheckingAvenue) {
            if (mv.toCoord.file == checkCell.file && mv.toCoord.rank == checkCell.rank) {
              awayFromCheck = false;
              break;
            }
          }
          if (awayFromCheck) {
            possibleMovesCheck.add(mv);
          }
          //Part 2: The King captures the checking piece.
          if (mv.isCapture) {
            if (chessBoard.board[mv.toCoord.rank][mv.toCoord.file].piece.equals(checkingPiece)) {
              possibleMovesCheck.add(mv);
            }
          }
        } else {
          boolean blocksCheck = false;
          for (Coordinate crd:CheckingAvenue) {
            if (crd.file == mv.toCoord.file && crd.rank == mv.toCoord.rank) {
              blocksCheck = true;
              break;
            }
          }
          if (blocksCheck) {
            possibleMovesCheck.add(mv);
        }
      }
    }
    return possibleMovesCheck;
  }

  /**
   * Returns all the Cells along the Checking Avenue.  (All cells between the King and Checking piece)
   * @param checkingPiece
   * @param checkerLoc
   * @param kingLoc
   * @return
   */
  public static ArrayList<Coordinate> getCheckingAvenue(Piece checkingPiece, Coordinate checkerLoc, Coordinate kingLoc) {
    ArrayList<Coordinate> checkingAvenue = new ArrayList<>();
    if (checkingPiece.getName().equals("Knight") || checkingPiece.getName().equals("Pawn")) {
      checkingAvenue.add(checkerLoc);
      checkingAvenue.add(kingLoc);

      //'LONG RANGE' PIECES (ROOK, BISHOP, QUEEN)
    }else {
      checkingAvenue.add(checkerLoc); //Note: square of the checking piece is also added to checkingAvenue
      //Case 1: Down the same file.
      if (checkerLoc.file==kingLoc.file) {
        if (checkerLoc.rank>kingLoc.rank) {
          Coordinate tempLoc = new Coordinate(checkerLoc.file,checkerLoc.rank-1);
          while (tempLoc.rank > 0 && tempLoc.rank >= kingLoc.rank-1) {
            checkingAvenue.add(tempLoc);
            tempLoc = new Coordinate(tempLoc.file,tempLoc.rank-1);
          }
          //Case 1.1 Up the same rank.
        }else {
          Coordinate tempLoc = new Coordinate(checkerLoc.file, checkerLoc.rank+1);
          while (tempLoc.rank < 8 && tempLoc.rank <= kingLoc.rank + 1) {
            checkingAvenue.add(tempLoc);
            tempLoc = new Coordinate(tempLoc.file,tempLoc.rank+1);
          }
        }
      //Case 2: Along the same rank.
      }else if (checkerLoc.rank==kingLoc.rank) {
        //2.1 down the file
        if (checkerLoc.file>kingLoc.file) {
          Coordinate tempLoc = new Coordinate(checkerLoc.file-1,checkerLoc.rank);
          while (tempLoc.file > 0 && tempLoc.file >= kingLoc.file - 1) {
            checkingAvenue.add(tempLoc);
            tempLoc = new Coordinate(tempLoc.file-1,tempLoc.rank);
          }
          //2.2: Up the file.
        }else {
          Coordinate tempLoc = new Coordinate(checkerLoc.file+1, checkerLoc.rank);
          while (tempLoc.file < 8 && tempLoc.file <= kingLoc.file + 1) {
            checkingAvenue.add(tempLoc);
            tempLoc = new Coordinate(tempLoc.file+1,tempLoc.rank+1);
          }
        }
      }
      //Same Diagonal //TODO: EXTEND THE CHECKING AVENUE BY ONE.
      else if((checkerLoc.file+checkerLoc.rank)==kingLoc.file+kingLoc.rank) {
        if (checkerLoc.rank>kingLoc.rank) {
          Coordinate temp = new Coordinate(checkerLoc.file+1, checkerLoc.rank-1);
          while (temp.rank > 0 && temp.rank >= kingLoc.rank - 1) {
            checkingAvenue.add(temp);
            temp = new Coordinate(temp.file+1,temp.rank-1);
          }
        }else {
          Coordinate temp = new Coordinate(checkerLoc.file-1, checkerLoc.rank+1);
          while (temp.rank < 8 && temp.rank <= kingLoc.rank + 1) {
            checkingAvenue.add(temp);
            temp = new Coordinate(temp.file-1,temp.rank+1);
          }
        }
      }
      else if((checkerLoc.file-checkerLoc.rank)==kingLoc.file-kingLoc.rank) {
        if (checkerLoc.rank>kingLoc.rank) {
          Coordinate temp = new Coordinate(checkerLoc.file-1, checkerLoc.rank-1);
          while (temp.rank > 0 && temp.rank >= kingLoc.rank - 1) {
            checkingAvenue.add(temp);
            temp = new Coordinate(temp.file-1,temp.rank-1);
          }
        }else {
          Coordinate temp = new Coordinate(checkerLoc.file+1, checkerLoc.rank+1);
          while (temp.rank < 8 && temp.rank <= kingLoc.rank + 1) {
            checkingAvenue.add(temp);
            temp = new Coordinate(temp.file+1,temp.rank+1);
          }
        }
      }
    }
    return checkingAvenue;
  }

  /**
   * Iterates over all the pieces and updates the Cell's of the board and sets pieces to Pinned/Reveal Checkers.
   * ALSO SETS CHECKS IF A KING CAN BE CAPTURED.
   * @param ChessBoard
   * @return
   */
  public static Board updateBoardMeta(Board ChessBoard) {
    ChessBoard = clearBoard(ChessBoard);
    for (Piece piece : ChessBoard.whitePieces) {
      ArrayList<Move> rawMoves = piece.generateMoves(piece.getPos(), ChessBoard.board);
      for (Move mv : rawMoves) {
        if (mv.piece==null || (mv.piece.getName().equals("Pawn") && !(mv.coverMove || mv.protectionMove ||mv.isCapture))) {
          //Extend the checking avenue one beyond the king so that that square is not available on black next Move
          //1. If the move is a protection move. Update the board, the boards piece and the Piece.
        }else if (mv.protectionMove) {
          ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
          ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].piece.addProtector(mv.piece);

          //2 If the move is a capture.  Update the board.
        }else if (mv.isCapture){
          if (mv.capturablePiece == null) {
            int a =1;
          }
          //2.1 If the captured piece is a King, then set the opponent to in Check!!
          if (mv.capturablePiece.getName().equals("King")){
            ((King)ChessBoard.blackPieces.get(0)).setCheck(mv.fromCoord, getCheckingAvenue(mv.piece, mv.fromCoord, ChessBoard.blackPieces.get(0).getPos()));
          }
            ChessBoard.board[mv.capturablePiece.getPos().rank][mv.capturablePiece.getPos().file].addAttacker(mv.piece);
            ChessBoard.board[mv.capturablePiece.getPos().rank][mv.capturablePiece.getPos().file].piece.addAttacker(mv.piece);


          //2.2: If the Piece is currently X-Ray's the King through an adversary piece then set the opponent's piece to pinned
          if (mv.isPin) {
            int pinnedIndex = Board.getIndex(ChessBoard.blackPieces, mv.getPinLoc());
            ChessBoard.blackPieces.get(pinnedIndex).setPin(mv.pinAvenue, mv.piece.getPos());
          }
          //3: Otherwise (Not in contact with a piece, add an attacker to this square.
        }else {
          ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
        }
      }
    }
    for (Piece piece : ChessBoard.blackPieces) {
      ArrayList<Move> rawMoves = piece.generateMoves(piece.getPos(), ChessBoard.board);
      for (Move mv : rawMoves) {

        if (mv.piece==null || (mv.piece.getName().equals("Pawn") && !(mv.coverMove || mv.protectionMove ||mv.isCapture))) {
          continue;
          //1 IF the move is a protection move. Update the board, the boards piece and the Piece.
        }else if (mv.protectionMove) {
          ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
          ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].piece.addProtector(mv.piece);

          //2 IF the move is a capture.  Update the board, the Piece
        }else if (mv.isCapture){
          if (mv.capturablePiece == null) {
            Board.printBoard(ChessBoard, true);
            int s=1;
          }
          //CONVERT TO MOVE.capturablePiece.location!!
          //2.1 If the captured piece is a King, then set the opponent to in Check!!
          if (mv.capturablePiece.getName().equals("King")){
            ((King)ChessBoard.whitePieces.get(0)).setCheck(mv.fromCoord, getCheckingAvenue(mv.piece, mv.fromCoord, ChessBoard.whitePieces.get(0).getPos()));
          }
            ChessBoard.board[mv.capturablePiece.getPos().rank][mv.capturablePiece.getPos().file].addAttacker(mv.piece);
            ChessBoard.board[mv.capturablePiece.getPos().rank][mv.capturablePiece.getPos().file].piece.addAttacker(mv.piece);

            //2.1: If the Move is currently pinning a piece (X-Ray's the King through an adversary piece) then set the opponent's piece to pinned
          if (mv.isPin) {
            int pinnedIndex = Board.getIndex(ChessBoard.whitePieces, mv.getPinLoc());
            ChessBoard.whitePieces.get(pinnedIndex).setPin(mv.pinAvenue, mv.piece.getPos());
          }
          //3: Otherwise (Not in contact with a piece, add an attacker to this square.
        }else {
          ChessBoard.board[mv.toCoord.rank][mv.toCoord.file].addAttacker(mv.piece);
        }
      }
    }
    return ChessBoard;
  }
  /**
   * Resets the lists of attackers and protectors.
   * @param ChessBoard
   * @return
   */
  public static Board clearBoard(Board ChessBoard){
    for (int i=0; i<8; i++) {
      for (int j=0; j<8; j++) {
        ChessBoard.board[i][j].blackAttackers = new ArrayList<Piece>();
        ChessBoard.board[i][j].whiteAttackers = new ArrayList<Piece>();
      }
    }
    for (Piece piece : ChessBoard.whitePieces) {
      piece.reset();
    }
    for (Piece piece : ChessBoard.blackPieces) {
      piece.reset();
    }
    return ChessBoard;
  }

  public static Agent getAgent(String name, boolean isWhite) {
    switch(name) {
      case "Random":
        return new Randy(AgentType.RANDY, isWhite);
      case "FishStock":
        return new FishStock(AgentType.FISHSTOCK, isWhite);
      default:
        return new Human(AgentType.HUMAN, isWhite);
      }
    }
    public static ArrayList<Move> filterMoves (ArrayList<Move> rawMoves) {
      ArrayList<Move> filteredMoves = new ArrayList<>();
      for (Move move : rawMoves) {
        if (move.protectionMove || move.coverMove) {
          continue;
        } else {
          filteredMoves.add(move);
        }
      }
      return filteredMoves;
    }

  /**
   * A helper function which checks if the current move is a check by looping through all the moves after applying and seeing if the adversary King can be taken.
   * @param ChessBoard
   * @param move
   * @return
   */
  public static boolean checkForCheck(Board ChessBoard, Move move) {
    //1. Copy the whole ChessBoard.
    Board copyBoard = copyBoard(ChessBoard);
    //2. Make the move on the copied board.
    makeMove(copyBoard, move, move.piece.getColor());
    //3. Update the copy board's meta.
    updateBoardMeta(copyBoard);
    //4. Generate the pieces moves after making the initial moves (making two moves in a row with the same piece)
    ArrayList<Move> l2Moves = move.piece.generateMoves(move.piece.getPos(), copyBoard.board);
    for (Move l2move : l2Moves) {
      //5. If on the second move, the piece can capture the king, then the original piece is checking.
      if (l2move.isCapture && l2move.capturablePiece.getName().equals("King")){
        return true;
      }
    }
    return false;
  }
  /**
   * Copies a board
   *
   * @param ChessBoard
   * @return
   */
  public static Board copyBoard(Board ChessBoard) {
    ArrayList<Piece> copyWhitePieces = new ArrayList<>();
    ArrayList<Piece> copyBlackPieces = new ArrayList<>();
    Cell [][] copyBoard = new Cell[8][8];
    for (int i = 0; i < 8; i++) {
      for (int j=0; j<8; j++){
        copyBoard[i][j] = Cell.copyCell(ChessBoard.board[i][j]);
      }
    }
    for (Piece piece : ChessBoard.whitePieces) {
      copyWhitePieces.add(piece.copyPiece());
    }
    for (Piece piece : ChessBoard.blackPieces) {
      copyBlackPieces.add(piece.copyPiece());
    }
    return new Board(copyBoard, copyWhitePieces, copyBlackPieces);
  }
  public static boolean isDeadPosition(List<Piece> whitesPieces, List<Piece> blackPieces) {
    boolean isWhiteDead = false;
    boolean isBlackDead = false;
    if (whitesPieces.size()==1) {
      isWhiteDead = true;
    } else if (whitesPieces.size() == 2) {
      if (whitesPieces.get(1).getName().equals("Bishop")
          || whitesPieces.get(1).getName().equals("Knight")){
        if (blackPieces.size() == 1) {
          isWhiteDead = true;
        }
      }
    }
    if (blackPieces.size()==1) {
      isBlackDead = true;
    } else if (blackPieces.size() == 2) {
      if (blackPieces.get(1).getName().equals("Bishop")
          || blackPieces.get(1).getName().equals("Knight")){
        if (whitesPieces.size() == 1) {
          isBlackDead = true;
        }
      }
    }
    return isWhiteDead && isBlackDead;
  }
}
