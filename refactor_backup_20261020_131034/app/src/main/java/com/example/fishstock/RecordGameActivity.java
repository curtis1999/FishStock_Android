package com.example.fishstock;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fishstock.engine.Game;
import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.ui.BoardRenderer;
import com.example.fishstock.ui.PromotionDialog;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Record Game: log an over-the-board game by voice, then analyse it.
 *
 * Put the phone beside the board and press LISTEN. Each player says their move as they play it
 * ("e4", "knight f3", "e takes d5", "castles"); the phone checks what it heard against the legal
 * moves ({@link SpokenMoves}), plays it on the board, adds it to the move list and (optionally)
 * says it back, so you know it was heard right without looking. Anything that isn't a legal move
 * (talk, noise) is shown under the board and ignored. "Undo" takes the last move back. Moves can
 * also be tapped in on the board, e.g. if the room is noisy.
 *
 * The phone listens until LISTEN is pressed again or the game ends; the screen stays on meanwhile.
 * The game is saved after every move, so leaving the screen (or the app being closed) loses nothing.
 * ANALYZE opens the game on the analysis screen, reviewed from the side you choose.
 *
 * Speech recognition is Android's own (on Samsung phones, usually Google's). It works offline if
 * the phone has the offline English pack (Settings > General management > Google voice typing >
 * Offline speech recognition).
 */
public class RecordGameActivity extends AppCompatActivity {
  private static final String PREFS = "fishstock";
  private static final String KEY_MOVES = "record_moves";
  private static final String KEY_SPEAK_BACK = "record_speak_back";
  private static final int REQUEST_MIC = 41;
  /** How many guesses to ask the recogniser for: the second or third is often the chess one. */
  private static final int MAX_RESULTS = 5;
  private static final long RESTART_DELAY_MS = 150;
  private static final long BUSY_RETRY_MS = 700;

  private final Handler main = new Handler(Looper.getMainLooper());
  private Game game;
  private BoardRenderer board;
  private TextView statusText;
  private TextView heardText;
  private TextView movesText;
  private Button listenButton;
  private Switch speakBack;
  private Speaker speaker;
  private SpeechRecognizer recognizer;
  /** The person wants the phone listening (it stops briefly while speaking a move back). */
  private boolean listening;
  private int selectedSquare = -1;
  private boolean animating;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_record);
    setTitle("Record Game");
    board = new BoardRenderer(this);
    statusText = findViewById(R.id.recordStatus);
    heardText = findViewById(R.id.recordHeard);
    movesText = findViewById(R.id.recordMoves);
    listenButton = findViewById(R.id.recordListen);
    speakBack = findViewById(R.id.recordSpeakBack);
    speaker = new Speaker(this);

    speakBack.setChecked(prefs().getBoolean(KEY_SPEAK_BACK, true));
    speakBack.setOnCheckedChangeListener((b, on) -> prefs().edit().putBoolean(KEY_SPEAK_BACK, on).apply());
    listenButton.setOnClickListener(v -> {
      if (listening) stopListening();
      else startListening();
    });
    findViewById(R.id.recordUndo).setOnClickListener(v -> undo());
    findViewById(R.id.recordFlip).setOnClickListener(v -> {
      board.setFlipped(!board.isFlipped());
      redraw();
    });
    findViewById(R.id.recordNewGame).setOnClickListener(v -> confirmNewGame());
    findViewById(R.id.recordAnalyze).setOnClickListener(v -> analyze());
    board.setOnSquareClickListener(this::onSquareClicked);

    game = new Game();
    restore();
    redraw();
    showMoves();
    updateStatus();
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Android doesn't let an app use the microphone from the background; stop cleanly.
    if (listening) stopListening();
    speaker.stop();
    save();
  }

  @Override
  protected void onDestroy() {
    main.removeCallbacksAndMessages(null);
    if (recognizer != null) {
      recognizer.destroy();
      recognizer = null;
    }
    speaker.shutdown();
    board.cancelAnimation();
    super.onDestroy();
  }

  // ================================================================ listening

  private void startListening() {
    if (game.isOver()) {
      heardText.setText("The game is over. Press Analyze, or New Game.");
      return;
    }
    if (!SpeechRecognizer.isRecognitionAvailable(this)) {
      heardText.setText("This phone has no speech recognition. Install or enable the Google app, or tap the moves in on the board.");
      return;
    }
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
      return;
    }
    if (recognizer == null) {
      recognizer = SpeechRecognizer.createSpeechRecognizer(this);
      recognizer.setRecognitionListener(listener);
    }
    listening = true;
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    listenButton.setText("Stop Listening");
    updateStatus();
    listenOnce();
  }

  private void stopListening() {
    listening = false;
    main.removeCallbacksAndMessages(null);
    if (recognizer != null) recognizer.cancel();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    listenButton.setText("Listen");
    updateStatus();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (requestCode != REQUEST_MIC) return;
    if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startListening();
    else heardText.setText("Without the microphone the moves can only be tapped in on the board.");
  }

  /** One utterance: Android's recogniser stops after each phrase, so it is restarted every time. */
  private void listenOnce() {
    if (!listening || recognizer == null || isFinishing()) return;
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    // The parser understands English chess words.
    String lang = Locale.getDefault().getLanguage().equals("en") ? Locale.getDefault().toLanguageTag() : "en-US";
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS);
    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
    if (Build.VERSION.SDK_INT >= 33) {
      // A hint for recognisers that support it: these words are likely.
      intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, new ArrayList<>(Arrays.asList(
          "knight", "bishop", "rook", "queen", "king", "pawn", "takes", "castles", "kingside", "queenside",
          "promotes", "check", "undo", "e4", "d4", "c4", "f3", "c3", "e5", "d5", "c5", "f6", "c6")));
    }
    try {
      recognizer.startListening(intent);
    } catch (RuntimeException e) {
      scheduleListen(BUSY_RETRY_MS);
    }
  }

  private final Runnable listenAgain = this::listenOnce;

  /** Listens again after delayMs, replacing any restart already waiting (so they never pile up). */
  private void scheduleListen(long delayMs) {
    main.removeCallbacks(listenAgain);
    if (listening) main.postDelayed(listenAgain, delayMs);
  }

  private final RecognitionListener listener = new RecognitionListener() {
    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {
      if (!listening) return;
      if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
        stopListening();
        heardText.setText("The microphone permission was refused.");
        return;
      }
      // Silence (no match / timeout) is normal between moves: just listen again.
      long delay = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT
          || error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
          || error == SpeechRecognizer.ERROR_SERVER ? BUSY_RETRY_MS : RESTART_DELAY_MS;
      if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
        heardText.setText("No connection for speech recognition. Download the offline English pack, or tap moves in.");
      }
      scheduleListen(delay);
    }

    @Override
    public void onResults(Bundle results) {
      List<String> heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
      onHeard(heard == null ? Collections.<String>emptyList() : heard);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
  };

  /** The recogniser's guesses for one phrase, best first. */
  private void onHeard(List<String> hypotheses) {
    if (!listening) return;
    if (hypotheses.isEmpty()) {
      scheduleListen(RESTART_DELAY_MS);
      return;
    }
    String best = hypotheses.get(0);
    if (SpokenMoveParser.command(best) == SpokenMoveParser.Command.UNDO) {
      heardText.setText("Heard \"" + best + "\"");
      undo();
      speakThenListen(game.plyCount() == 0 ? "Undone. Back to the start." : "Undone.");
      return;
    }
    if (animating) {
      board.cancelAnimation();
      animating = false;
    }
    Position pos = game.position();
    SpokenMoves.Heard result = SpokenMoves.find(pos, hypotheses);
    if (result.move != null) {
      String words = SpokenMoves.say(pos, result.move);
      heardText.setText("Heard \"" + best + "\"  →  " + Notation.toSan(pos, result.move));
      play(result.move);
      if (game.isOver()) {
        String end = game.result().message;
        stopListening();
        if (speakBack.isChecked()) speaker.say(Arrays.asList(words + ".", end), 300, null);
        return;
      }
      if (speakBack.isChecked()) speakThenListen(words + ".");
      else scheduleListen(RESTART_DELAY_MS);
      return;
    }
    if (!result.options.isEmpty()) {
      heardText.setText("Heard \"" + best + "\": did you mean " + join(result.options)
          + "? Say it with the file or rank, e.g. \"knight b d2\", or tap it in.");
      speakThenListen("Which one? " + join(spokenOptions(result.options)) + "?");
      return;
    }
    // Not a legal move: probably talk. Show it but say nothing, so conversation doesn't trigger chatter.
    heardText.setText("Heard \"" + best + "\": not a legal move for "
        + (pos.whiteToMove() ? "White" : "Black") + ". Ignored.");
    scheduleListen(RESTART_DELAY_MS);
  }

  /** Stops listening while the phone talks (so it doesn't hear itself), then listens again. */
  private void speakThenListen(String sentence) {
    if (!speakBack.isChecked() || !speaker.isReady()) {
      scheduleListen(RESTART_DELAY_MS);
      return;
    }
    speaker.say(Collections.singletonList(sentence), 0, () -> {
      if (listening) scheduleListen(RESTART_DELAY_MS);
    });
  }

  private static List<String> spokenOptions(List<String> sans) {
    List<String> out = new ArrayList<>();
    for (String san : sans) out.add(SpokenMoveParser.sanToWords(san));
    return out;
  }

  private static String join(List<String> items) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) sb.append(i == items.size() - 1 ? " or " : ", ");
      sb.append(items.get(i));
    }
    return sb.toString();
  }

  // ================================================================ tapping moves in

  private void onSquareClicked(int sq) {
    if (game.isOver()) return;
    if (animating) {
      board.cancelAnimation();
      animating = false;
    }
    Position pos = game.position();
    if (selectedSquare >= 0) {
      for (Move m : MoveGenerator.legalMovesFrom(pos, selectedSquare)) {
        if (m.to != sq) continue;
        if (m.isPromotion()) askPromotion(selectedSquare, sq);
        else {
          selectedSquare = -1;
          heardText.setText("");
          play(m);
        }
        return;
      }
    }
    boolean ours = Piece.isColor(pos.pieceAt(sq), pos.whiteToMove());
    selectedSquare = sq != selectedSquare && ours && !MoveGenerator.legalMovesFrom(pos, sq).isEmpty() ? sq : -1;
    redraw();
  }

  private void askPromotion(final int from, final int to) {
    new PromotionDialog(this, game.position().whiteToMove(), type -> {
      selectedSquare = -1;
      if (type == 0) {
        redraw();
        return;
      }
      for (Move m : MoveGenerator.legalMovesFrom(game.position(), from)) {
        if (m.to == to && m.promotion == type) {
          play(m);
          return;
        }
      }
    }).show();
  }

  // ================================================================ the game

  private void play(Move m) {
    selectedSquare = -1;
    Position before = game.position().copy();
    game.play(m);
    save();
    showMoves();
    updateStatus();
    animating = true;
    board.animateMove(before, m, game.position(), () -> {
      animating = false;
      redraw();
    });
  }

  private void undo() {
    if (game.plyCount() == 0) return;
    board.cancelAnimation();
    animating = false;
    game.undo();
    selectedSquare = -1;
    save();
    redraw();
    showMoves();
    updateStatus();
  }

  private void confirmNewGame() {
    if (game.plyCount() == 0) return;
    new AlertDialog.Builder(this)
        .setMessage("Start a new recording? The moves recorded so far will be cleared.")
        .setPositiveButton("New Game", (d, w) -> {
          if (listening) stopListening();
          board.cancelAnimation();
          animating = false;
          game = new Game();
          selectedSquare = -1;
          heardText.setText("");
          save();
          redraw();
          showMoves();
          updateStatus();
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  /** Opens the analysis screen on this game, reviewed from the side chosen (that side at the bottom). */
  private void analyze() {
    if (game.plyCount() == 0) {
      heardText.setText("No moves recorded yet.");
      return;
    }
    if (listening) stopListening();
    new AlertDialog.Builder(this)
        .setTitle("Review the game for")
        .setItems(new String[] {"White", "Black", "Both players"}, (d, which) -> {
          int side = which == 0 ? 1 : which == 1 ? -1 : 0;
          StringBuilder uci = new StringBuilder();
          for (Move m : game.moves()) uci.append(m.toUci()).append(' ');
          Intent intent = new Intent(this, GameAnalysis.class);
          intent.putExtra(GameAnalysis.EXTRA_START_FEN, game.startFen());
          intent.putExtra(GameAnalysis.EXTRA_MOVES, uci.toString().trim());
          intent.putExtra(GameAnalysis.EXTRA_TITLE, "Recorded game, " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()));
          intent.putExtra(GameAnalysis.EXTRA_TRAIN_SIDE, side);
          intent.putExtra(GameAnalysis.EXTRA_FLIPPED, side == -1);
          startActivity(intent);
        })
        .show();
  }

  // ================================================================ showing it

  private void redraw() {
    Position pos = game.position();
    board.draw(pos);
    if (selectedSquare >= 0) board.highlight(pos, selectedSquare, MoveGenerator.legalMovesFrom(pos, selectedSquare));
  }

  /** "1. e4 e5  2. Nf3 Nc6  3. Bb5" */
  private void showMoves() {
    List<Move> moves = game.moves();
    if (moves.isEmpty()) {
      movesText.setText("No moves yet.");
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < moves.size(); i++) {
      Position before = game.positionAt(i);
      if (before.whiteToMove()) sb.append(i == 0 ? "" : "  ").append(before.fullmoveNumber()).append(". ");
      else if (i == 0) sb.append(before.fullmoveNumber()).append("... ");
      else sb.append(' ');
      sb.append(Notation.toSan(before, moves.get(i)));
    }
    if (game.isOver()) sb.append("\n").append(game.result().message);
    movesText.setText(sb.toString());
  }

  private void updateStatus() {
    if (game.isOver()) {
      statusText.setText(game.result().message + "\nPress Analyze to review the game.");
      return;
    }
    String side = game.position().whiteToMove() ? "White" : "Black";
    statusText.setText(listening ? "Listening... " + side + " to move" : side + " to move. Press Listen, or tap the move in.");
  }

  // ================================================================ saving

  private SharedPreferences prefs() {
    return getSharedPreferences(PREFS, MODE_PRIVATE);
  }

  private void save() {
    StringBuilder uci = new StringBuilder();
    for (Move m : game.moves()) uci.append(m.toUci()).append(' ');
    prefs().edit().putString(KEY_MOVES, uci.toString().trim()).apply();
  }

  /** Picks up the recording where it was left. */
  private void restore() {
    String saved = prefs().getString(KEY_MOVES, "");
    if (saved.isEmpty()) return;
    for (String uci : saved.split("\\s+")) {
      if (game.isOver()) break;
      Move m = game.findUci(uci);
      if (m == null) break;
      game.play(m);
    }
    if (game.plyCount() > 0) heardText.setText("Carrying on with the game recorded earlier. New Game starts a fresh one.");
  }
}
