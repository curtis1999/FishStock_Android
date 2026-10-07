package com.example.fishstock;

import com.example.fishstock.Agents.AgentType;
import com.example.fishstock.Pieces.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive test suite for Game undo/redo functionality.
 * Tests various scenarios to ensure board state management works correctly.
 */
public class TestUndo {

  /**
   * Test 1: Basic undo after one move
   */
  @Test
  public void testBasicUndo() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();

    // Add initial state
    game.boardStates.add(GameService.copyBoard(initialBoard));

    // Make a move (e2-e4)
    Board afterMove = GameService.copyBoard(initialBoard);
    Move testMove = new Move(new Coordinate(4, 1), new Coordinate(4, 3), "Pawn", false, true);
    GameService.makeMove(afterMove, testMove, true);
    game.addBoardState(GameService.copyBoard(afterMove));
    game.whitesMovesLog.add(testMove);

    assertEquals("Should have 2 states", 2, game.getTotalStates());
    assertEquals("Should be at state 1", 1, game.getCurrentStateIndex());

    // Undo the move
    Board undoneBoard = game.getPreviousBoard();

    assertEquals("Should be at state 0 after undo", 0, game.getCurrentStateIndex());
    assertTrue("Board should match initial state", Board.compareBoard(undoneBoard, initialBoard));
    assertEquals("White moves log should be empty", 0, game.whitesMovesLog.size());
  }

  /**
   * Test 2: Multiple undos
   */
  @Test
  public void testMultipleUndos() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();
    game.boardStates.add(GameService.copyBoard(initialBoard));

    // Make 3 moves
    Board currentBoard = GameService.copyBoard(initialBoard);

    // Move 1: e2-e4 (white)
    Move move1 = new Move(new Coordinate(4, 1), new Coordinate(4, 3), "Pawn", false, true);
    GameService.makeMove(currentBoard, move1, true);
    game.addBoardState(GameService.copyBoard(currentBoard));
    game.whitesMovesLog.add(move1);

    // Move 2: e7-e5 (black)
    Move move2 = new Move(new Coordinate(4, 6), new Coordinate(4, 4), "Pawn", false, false);
    GameService.makeMove(currentBoard, move2, false);
    game.addBoardState(GameService.copyBoard(currentBoard));
    game.blacksMovesLog.add(move2);

    // Move 3: Nf3 (white)
    Move move3 = new Move(new Coordinate(6, 0), new Coordinate(5, 2), "Knight", false, true);
    GameService.makeMove(currentBoard, move3, true);
    game.addBoardState(GameService.copyBoard(currentBoard));
    game.whitesMovesLog.add(move3);

    assertEquals("Should have 4 states (initial + 3 moves)", 4, game.getTotalStates());
    assertEquals("Should be at state 3", 3, game.getCurrentStateIndex());

    // Undo once
    game.getPreviousBoard();
    assertEquals("After 1st undo, should be at state 2", 2, game.getCurrentStateIndex());
    assertEquals("Should have 1 white move", 1, game.whitesMovesLog.size());
    assertEquals("Should have 1 black move", 1, game.blacksMovesLog.size());

    // Undo again
    game.getPreviousBoard();
    assertEquals("After 2nd undo, should be at state 1", 1, game.getCurrentStateIndex());
    assertEquals("Should have 1 white move", 1, game.whitesMovesLog.size());
    assertEquals("Should have 0 black moves", 0, game.blacksMovesLog.size());

    // Undo third time
    game.getPreviousBoard();
    assertEquals("After 3rd undo, should be at state 0", 0, game.getCurrentStateIndex());
    assertEquals("Should have 0 white moves", 0, game.whitesMovesLog.size());
    assertEquals("Should have 0 black moves", 0, game.blacksMovesLog.size());
  }

  /**
   * Test 3: Undo, make new move, then undo again
   * This is the critical test case that was failing
   */
  @Test
  public void testUndoMakeMoveThenUndo() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();
    game.boardStates.add(GameService.copyBoard(initialBoard));

    // Make move 1: e2-e4
    Board board1 = GameService.copyBoard(initialBoard);
    Move move1 = new Move(new Coordinate(4, 1), new Coordinate(4, 3), "Pawn", false, true);
    GameService.makeMove(board1, move1, true);
    game.addBoardState(GameService.copyBoard(board1));
    game.whitesMovesLog.add(move1);

    // Make move 2: e7-e5
    Board board2 = GameService.copyBoard(board1);
    Move move2 = new Move(new Coordinate(4, 6), new Coordinate(4, 4), "Pawn", false, false);
    GameService.makeMove(board2, move2, false);
    game.addBoardState(GameService.copyBoard(board2));
    game.blacksMovesLog.add(move2);

    assertEquals("Should have 3 states", 3, game.getTotalStates());

    // Undo move 2
    Board afterUndo = game.getPreviousBoard();
    assertEquals("Should be at state 1", 1, game.getCurrentStateIndex());
    assertTrue("Should match board after move 1", Board.compareBoard(afterUndo, board1));

    // Make a DIFFERENT move 2: d7-d5
    Board board2Alt = GameService.copyBoard(afterUndo);
    Move move2Alt = new Move(new Coordinate(3, 6), new Coordinate(3, 4), "Pawn", false, false);
    GameService.makeMove(board2Alt, move2Alt, false);
    game.addBoardState(GameService.copyBoard(board2Alt));
    game.blacksMovesLog.add(move2Alt);

    assertEquals("Should have 3 states (old move 2 replaced)", 3, game.getTotalStates());
    assertEquals("Should be at state 2", 2, game.getCurrentStateIndex());

    // Now undo this new move
    Board afterSecondUndo = game.getPreviousBoard();
    assertEquals("Should be back at state 1", 1, game.getCurrentStateIndex());
    assertTrue("Should match board after move 1", Board.compareBoard(afterSecondUndo, board1));
  }

  /**
   * Test 4: Undo at start (edge case)
   */
  @Test
  public void testUndoAtStart() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();
    game.boardStates.add(GameService.copyBoard(initialBoard));

    Board result = game.getPreviousBoard();

    assertEquals("Should remain at state 0", 0, game.getCurrentStateIndex());
    assertTrue("Should return initial board", Board.compareBoard(result, initialBoard));
  }

  /**
   * Test 5: Redo functionality
   */
  @Test
  public void testRedo() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();
    game.boardStates.add(GameService.copyBoard(initialBoard));

    // Make a move
    Board afterMove = GameService.copyBoard(initialBoard);
    Move move1 = new Move(new Coordinate(4, 1), new Coordinate(4, 3), "Pawn", false, true);
    GameService.makeMove(afterMove, move1, true);
    game.addBoardState(GameService.copyBoard(afterMove));

    assertEquals("Should be at state 1", 1, game.getCurrentStateIndex());
    assertTrue("Can undo should be true", game.canUndo());
    assertFalse("Can redo should be false", game.canRedo());

    // Undo
    game.undoMove();
    assertEquals("Should be at state 0", 0, game.getCurrentStateIndex());
    assertFalse("Can undo should be false", game.canUndo());
    assertTrue("Can redo should be true", game.canRedo());

    // Redo
    Board redoneBoard = game.redoMove();
    assertEquals("Should be at state 1", 1, game.getCurrentStateIndex());
    assertTrue("Should match board after move", Board.compareBoard(redoneBoard, afterMove));
  }

  /**
   * Test 6: Complex scenario - multiple undos and redos
   */
  @Test
  public void testComplexUndoRedoSequence() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();
    game.boardStates.add(GameService.copyBoard(initialBoard));

    // Make 4 moves
    for (int i = 0; i < 4; i++) {
      Board newBoard = GameService.copyBoard(game.boardStates.get(game.getCurrentStateIndex()));
      // Just make some dummy move
      Move move = new Move(new Coordinate(i % 8, 1), new Coordinate(i % 8, 2), "Pawn", false, i % 2 == 0);
      game.addBoardState(newBoard);
    }

    assertEquals("Should have 5 states", 5, game.getTotalStates());
    assertEquals("Should be at state 4", 4, game.getCurrentStateIndex());

    // Undo 2 times
    game.undoMove();
    game.undoMove();
    assertEquals("Should be at state 2", 2, game.getCurrentStateIndex());

    // Redo once
    game.redoMove();
    assertEquals("Should be at state 3", 3, game.getCurrentStateIndex());

    // Make new move (this should delete state 4)
    Board newBoard = GameService.copyBoard(game.boardStates.get(game.getCurrentStateIndex()));
    game.addBoardState(newBoard);

    assertEquals("Should have 5 states (4 was replaced)", 5, game.getTotalStates());
    assertEquals("Should be at state 4", 4, game.getCurrentStateIndex());
    assertFalse("Should not be able to redo", game.canRedo());
  }

  /**
   * Test 7: Verify board states remain independent
   */
  @Test
  public void testBoardStatesIndependent() throws CloneNotSupportedException {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board initialBoard = new Board();
    game.boardStates.add(GameService.copyBoard(initialBoard));

    // Make a move
    Board afterMove = GameService.copyBoard(initialBoard);
    Move move1 = new Move(new Coordinate(4, 1), new Coordinate(4, 3), "Pawn", false, true);
    GameService.makeMove(afterMove, move1, true);
    game.addBoardState(GameService.copyBoard(afterMove));

    // Modify the current board
    Board currentBoard = game.boardStates.get(1);
    Move extraMove = new Move(new Coordinate(5, 1), new Coordinate(5, 3), "Pawn", false, true);
    GameService.makeMove(currentBoard, extraMove, true);

    // The initial board should not be affected
    Board retrievedInitial = game.boardStates.get(0);
    assertFalse("Initial board should not be affected by modifications to later state",
        Board.compareBoard(retrievedInitial, currentBoard));
  }

  /**
   * Test 8: Test with actual game simulation
   */
  @Test
  public void testWithGameSimulation() {
    Game game = new Game(new Board(), AgentType.HUMAN, AgentType.HUMAN);
    Board board = new Board();
    game.boardStates.add(GameService.copyBoard(board));

    // Simulate a few opening moves
    String[] moves = {"e2-e4", "e7-e5", "Nf3", "Nc6", "Bc4"};

    for (int i = 0; i < 3; i++) {
      Board newBoard = GameService.copyBoard(board);
      // Simulate making a move (details don't matter for this test)
      game.addBoardState(newBoard);
    }

    int statesBeforeUndo = game.getTotalStates();
    int indexBeforeUndo = game.getCurrentStateIndex();

    // Undo all moves
    while (game.canUndo()) {
      game.undoMove();
    }

    assertEquals("Should be back at start", 0, game.getCurrentStateIndex());
    assertEquals("All states should still exist", statesBeforeUndo, game.getTotalStates());

    // Redo all moves
    while (game.canRedo()) {
      game.redoMove();
    }

    assertEquals("Should be back at final position", indexBeforeUndo, game.getCurrentStateIndex());
  }
}