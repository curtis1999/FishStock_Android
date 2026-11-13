package com.example.fishstock;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Square;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a pasted game: a full PGN (tags, comments, variations, NAGs and all) or just the moves in
 * order ("1. e4 e5 2. Nf3", "e4 e5 Nf3", "e2e4 e7e5 g1f3", with or without move numbers).
 *
 * Only the main line is kept; comments {...} / ; ..., variations (...) and $n annotations are
 * dropped. A [FEN "..."] tag starts the game from that position. Each move may be SAN (Nf3, exd5,
 * O-O, 0-0-0, e8=Q, e8Q, Ng1f3) or UCI (g1f3, e7e8q).
 *
 * Pure Java (no Android), so it is unit-tested in PgnImportTest.
 */
public final class PgnImport {
  private PgnImport() {}

  /** What came out of the text: the moves as UCI, ready for GameAnalysis, or an error. */
  public static final class Result {
    public final String startFen;
    public final List<String> movesUci;
    /** "Magnus vs Hikaru  1-0", or "Imported game". */
    public final String title;
    /** Null when everything was read; otherwise what went wrong, for the person to fix. */
    public final String error;

    Result(String startFen, List<String> movesUci, String title, String error) {
      this.startFen = startFen;
      this.movesUci = movesUci;
      this.title = title;
      this.error = error;
    }

    public boolean ok() {
      return error == null;
    }

    /** "e2e4 e7e5 g1f3": the format GameAnalysis.EXTRA_MOVES takes. */
    public String movesText() {
      StringBuilder sb = new StringBuilder();
      for (String m : movesUci) sb.append(m).append(' ');
      return sb.toString().trim();
    }
  }

  private static final Pattern TAG = Pattern.compile("\\[\\s*(\\w+)\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\]");
  private static final Pattern UCI = Pattern.compile("[a-h][1-8][a-h][1-8][qrbn]?");
  /** Piece letter, optional from-file / from-rank, optional capture, target, optional promotion. */
  private static final Pattern SAN = Pattern.compile("([NBRQK])?([a-h])?([1-8])?(x|:)?([a-h][1-8])(?:=?([NBRQnbrq]))?");
  private static final Pattern MOVE_NUMBER = Pattern.compile("^\\d+\\.+");
  private static final Pattern RESULT = Pattern.compile("1-0|0-1|1/2-1/2|½-½|\\*");

  public static Result parse(String text) {
    if (text == null || text.trim().isEmpty()) return fail("Paste a PGN or a list of moves first.");

    // Tags
    String fen = Position.START_FEN;
    String white = null;
    String black = null;
    String result = null;
    Matcher tags = TAG.matcher(text);
    while (tags.find()) {
      String key = tags.group(1).toLowerCase(Locale.US);
      String value = tags.group(2).replace("\\\"", "\"").trim();
      if (key.equals("fen") && !value.isEmpty()) fen = value;
      else if (key.equals("white")) white = value;
      else if (key.equals("black")) black = value;
      else if (key.equals("result")) result = value;
    }
    String movetext = TAG.matcher(text).replaceAll(" ");

    Position pos;
    try {
      pos = Position.fromFen(fen);
    } catch (RuntimeException e) {
      return fail("The [FEN] tag isn't a position I can read:\n" + fen);
    }

    String cleaned = stripCommentsAndVariations(movetext);
    // "12.e4", "12...e5" and "12. e4" all become separate tokens; NAGs and results go.
    cleaned = cleaned.replaceAll("\\$\\d+", " ").replaceAll("(\\d+)\\.+", " $1. ");

    List<String> uci = new ArrayList<>();
    for (String raw : cleaned.trim().split("[\\s,]+")) {
      String token = raw.trim();
      if (token.isEmpty()) continue;
      if (MOVE_NUMBER.matcher(token).matches() || token.matches("\\d+")) continue;
      if (RESULT.matcher(token).matches()) break; // the game's over; anything after is noise
      if (token.equals("..") || token.equals("...") || token.equals("…")) continue;
      if (!MoveGenerator.hasLegalMove(pos)) {
        return fail("The game is already over before \"" + token + "\" (" + moveLabel(pos) + ").");
      }
      Move m;
      try {
        m = findMove(pos, token);
      } catch (Ambiguous a) {
        return fail("\"" + token + "\" at " + moveLabel(pos) + " could be more than one move. Add the file or rank of the piece, e.g. Nbd2.");
      }
      if (m == null) {
        return fail("Couldn't play \"" + token + "\" at " + moveLabel(pos)
            + (uci.isEmpty() ? "." : " (after " + uci.size() + " moves read fine).")
            + " Check for a typo, or that the game starts from the normal position.");
      }
      uci.add(m.toUci());
      pos.makeMove(m);
    }
    if (uci.isEmpty()) return fail("No moves found. Paste a PGN, or moves like: 1. e4 e5 2. Nf3 Nc6");

    String title;
    if (white != null && black != null && !white.equals("?") && !black.equals("?")) {
      title = white + " vs " + black + (result != null && !result.equals("*") ? "  " + result : "");
    } else {
      title = "Imported game";
    }
    return new Result(fen, uci, title, null);
  }

  /** "move 14 (White)" for the side to move in pos. */
  private static String moveLabel(Position pos) {
    return "move " + pos.fullmoveNumber() + (pos.whiteToMove() ? " (White)" : " (Black)");
  }

  private static Result fail(String why) {
    return new Result(null, new ArrayList<>(), null, why);
  }

  /** Removes {comments}, ; line comments, %escape lines and (nested (variations)). */
  static String stripCommentsAndVariations(String s) {
    StringBuilder out = new StringBuilder(s.length());
    int depth = 0;
    boolean brace = false;
    boolean lineComment = false;
    boolean lineStart = true;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (lineComment) {
        if (c == '\n' || c == '\r') {
          lineComment = false;
          out.append(' ');
          lineStart = true;
        }
        continue;
      }
      if (brace) {
        if (c == '}') brace = false;
        continue;
      }
      if (c == '{') {
        brace = true;
        out.append(' ');
      } else if (c == ';' || (c == '%' && lineStart)) {
        lineComment = true;
      } else if (c == '(') {
        depth++;
        out.append(' ');
      } else if (c == ')') {
        if (depth > 0) depth--;
        out.append(' ');
      } else if (depth == 0) {
        out.append(c);
      }
      lineStart = c == '\n' || c == '\r';
    }
    return out.toString();
  }

  private static final class Ambiguous extends Exception {
  }

  /** The legal move the token stands for, or null. Throws Ambiguous if SAN matches several. */
  static Move findMove(Position pos, String token) throws Ambiguous {
    List<Move> legal = MoveGenerator.legalMoves(pos);
    String t = token.replaceAll("[+#!?]", "").replace("e.p.", "").replace("ep", "").trim();
    if (t.isEmpty()) return null;

    // Castling, written with letter O or zero.
    String castle = t.toUpperCase(Locale.US).replace('0', 'O');
    if (castle.equals("O-O") || castle.equals("O-O-O")) {
      int file = castle.equals("O-O") ? 6 : 2;
      for (Move m : legal) if (m.isCastle() && Square.file(m.to) == file) return m;
      return null;
    }

    // UCI: e2e4, e7e8q (some sites also write the king taking its rook for castling: e1h1).
    String lower = t.toLowerCase(Locale.US);
    if (UCI.matcher(lower).matches()) {
      for (Move m : legal) if (m.toUci().equals(lower)) return m;
      for (Move m : legal) {
        if (m.isCastle() && Square.name(m.from).equals(lower.substring(0, 2))) {
          int rookFile = lower.charAt(2) - 'a';
          if ((rookFile == 7 && Square.file(m.to) == 6) || (rookFile == 0 && Square.file(m.to) == 2)) return m;
        }
      }
      // Fall through: "b1c3"-style strings can't be SAN, but "Bc4" etc. never match UCI anyway.
    }

    // SAN. A lower-case first letter other than a file means a piece written in lower case ("nf3").
    String san = t;
    if (san.length() >= 3 && "nrqk".indexOf(san.charAt(0)) >= 0) {
      san = Character.toUpperCase(san.charAt(0)) + san.substring(1);
    }
    Matcher sm = SAN.matcher(san);
    if (!sm.matches()) return null;
    int type = sm.group(1) == null ? Piece.PAWN : typeOf(sm.group(1).charAt(0));
    int fromFile = sm.group(2) == null ? -1 : sm.group(2).charAt(0) - 'a';
    int fromRank = sm.group(3) == null ? -1 : sm.group(3).charAt(0) - '1';
    int to = Square.parse(sm.group(5));
    int promotion = sm.group(6) == null ? 0 : typeOf(Character.toUpperCase(sm.group(6).charAt(0)));

    Move found = null;
    for (Move m : legal) {
      if (Piece.type(m.piece) != type || m.to != to || m.isCastle()) continue;
      if (fromFile >= 0 && Square.file(m.from) != fromFile) continue;
      if (fromRank >= 0 && Square.rank(m.from) != fromRank) continue;
      if (m.isPromotion()) {
        // A bare "e8" promotes to a queen, as most sites assume.
        int want = promotion == 0 ? Piece.QUEEN : promotion;
        if (m.promotion != want) continue;
      } else if (promotion != 0) {
        continue;
      }
      if (found != null) throw new Ambiguous();
      found = m;
    }
    return found;
  }

  private static int typeOf(char letter) {
    switch (letter) {
      case 'N':
        return Piece.KNIGHT;
      case 'B':
        return Piece.BISHOP;
      case 'R':
        return Piece.ROOK;
      case 'Q':
        return Piece.QUEEN;
      case 'K':
        return Piece.KING;
      default:
        return Piece.PAWN;
    }
  }
}
