package com.example.fishstock.analysis;

import com.example.fishstock.engine.Move;
import com.example.fishstock.engine.MoveGenerator;
import com.example.fishstock.engine.Notation;
import com.example.fishstock.engine.Piece;
import com.example.fishstock.engine.Position;
import com.example.fishstock.engine.Rules;
import com.example.fishstock.engine.Square;
import com.example.fishstock.eval.BoardAnalysis;
import com.example.fishstock.eval.Category;
import com.example.fishstock.eval.EvalBreakdown;
import com.example.fishstock.eval.EvalWeights;
import com.example.fishstock.eval.Evaluator;
import com.example.fishstock.eval.Term;
import com.example.fishstock.puzzles.Theme;
import com.example.fishstock.puzzles.ThemeDetector;
import com.example.fishstock.search.AlphaBetaSearch;
import com.example.fishstock.search.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Judges one move of a game, chess.com style, and says why in words.
 *
 * <b>Verdicts.</b> Scores are turned into a winning chance, w(s) = 1 / (1 + e^(-0.368 s)) for a
 * score of s pawns (mates count as 0 or 1), and the verdict depends on how much winning chance the
 * move threw away compared with the engine's best move:
 *  - BRILLIANT: the best move (or as good), and it gives up material ({@link Theme#SACRIFICE}),
 *    in a position that wasn't already won, and the mover is not worse afterwards;
 *  - BEST: the engine's move, or within 2% of it;
 *  - OKAY: lost less than {@link #MISTAKE_LOSS} (10%);
 *  - MISTAKE: lost {@link #MISTAKE_LOSS} to {@link #BLUNDER_LOSS};
 *  - BLUNDER: lost more than {@link #BLUNDER_LOSS} (20%);
 *  - MISS: there was a winning move (+2 or a mate) and the move played let it slip, without
 *    going on to lose (a move that also loses is a blunder).
 *
 * <b>Why.</b> A move that mates, or forces mate, is simply "Great move, you found the checkmate!"
 * (or "it forces checkmate in N"). Otherwise three kinds of reason are looked for, from the most concrete:
 *  1. Tactics: for a mistake or blunder, the opponent's best reply line is passed to
 *     {@link ThemeDetector} ("it allows a fork"); for a miss, the best line is ("you missed a fork");
 *     a piece left en prise or an allowed mate are spelled out.
 *  2. The evaluation function itself: the position after the move played is compared, term by
 *     term ({@link Evaluator#explain}), with the position after the best move. The term that got
 *     most worse (if it is a structural one: king safety, pawns, piece placement...) and that the
 *     move itself caused gives "it weakens your king (opens a file next to your king)".
 *  3. Otherwise the score difference.
 *
 * Pure Java; the analysis screen calls {@link #review} for every move in the background.
 */
public final class MoveReview {
  public static final double MISTAKE_LOSS = 0.10;
  public static final double BLUNDER_LOSS = 0.20;
  private static final double BEST_LOSS = 0.02;
  private static final double MISS_WIN = 2.0;
  private static final double MISS_DROP = 1.5;
  private static final double ALREADY_WON = 4.0;
  /** A term must move by at least this many pawns to be mentioned. */
  private static final double TERM_THRESHOLD = 0.15;

  public enum Verdict {
    BRILLIANT("Brilliant", "!!", 0xFF1BACA6),
    BEST("Best", "★", 0xFF12CC2B),
    OKAY("Okay", "", 0xFF808080),
    MISTAKE("Mistake", "?", 0xFFFF9800),
    BLUNDER("Blunder", "??", 0xFFE40D0D),
    MISS("Miss", "×", 0xFFD0459B);

    public final String label;
    public final String symbol;
    /** Colour used for the label and the graph node. */
    public final int colour;

    Verdict(String label, String symbol, int colour) {
      this.label = label;
      this.symbol = symbol;
      this.colour = colour;
    }
  }

  /** Everything known about one move. */
  public static final class Result {
    public final Verdict verdict;
    /** "24. Nf3" or "24... Nf6". */
    public final String moveText;
    /** The engine's preferred move in the same notation (same as moveText if it was played). */
    public final String bestText;
    /** Scores in pawns from the mover's point of view. */
    public final double bestScore;
    public final double playedScore;
    /** Winning chance thrown away, 0..1. */
    public final double winChanceLost;
    /** Ideas found in the relevant line (the opponent's punishment, or the missed win). */
    public final List<Theme> tactics;
    /** The sentence(s) to show. */
    public final String explanation;

    Result(Verdict verdict, String moveText, String bestText, double bestScore, double playedScore,
           double winChanceLost, List<Theme> tactics, String explanation) {
      this.verdict = verdict;
      this.moveText = moveText;
      this.bestText = bestText;
      this.bestScore = bestScore;
      this.playedScore = playedScore;
      this.winChanceLost = winChanceLost;
      this.tactics = tactics;
      this.explanation = explanation;
    }

    /** "24. Nf3?? Blunder" */
    public String headline() {
      return moveText + verdict.symbol + "  " + verdict.label;
    }
  }

  private MoveReview() {}

  /**
   * @param before       position before the move
   * @param played       the move played
   * @param beforeSearch search of {@code before} (best move, score for the mover, best line)
   * @param afterSearch  search of the position after the move (opponent to move), or null if the
   *                     game ended there
   * @param evaluator    the evaluation whose terms are compared
   */
  public static Result review(Position before, Move played, SearchResult beforeSearch, SearchResult afterSearch,
                              Evaluator evaluator) {
    boolean white = before.whiteToMove();
    Position after = before.copy();
    after.makeMove(played);
    String moveText = moveText(before, played);
    Move best = beforeSearch.bestMove != null ? beforeSearch.bestMove : played;
    boolean playedBest = best.sameAs(played);
    String bestText = moveText(before, best);

    double bestScore = beforeSearch.score;
    double playedScore;
    if (afterSearch == null || !MoveGenerator.hasLegalMove(after)) {
      playedScore = Rules.isCheckmate(after) ? AlphaBetaSearch.MATE - 1 : 0;
    } else {
      playedScore = -afterSearch.score;
    }
    // The two searches see different distances ahead; when the engine's own move was played,
    // trust the search that chose it.
    if (playedBest) playedScore = bestScore;
    else if (playedScore > bestScore) bestScore = playedScore;
    double loss = Math.max(0, winChance(bestScore) - winChance(playedScore));
    boolean asGoodAsBest = playedBest || loss < BEST_LOSS;

    boolean bestWins = isMate(bestScore) ? bestScore > 0 : bestScore >= MISS_WIN;
    boolean playedWinsToo = isMate(bestScore) && bestScore > 0 ? isMate(playedScore) && playedScore > 0
        : playedScore >= bestScore - MISS_DROP;
    boolean playedLoses = playedScore <= -MISS_WIN;

    Verdict v;
    if (!asGoodAsBest && bestWins && !playedWinsToo && !playedLoses) v = Verdict.MISS;
    else if (loss >= BLUNDER_LOSS) v = Verdict.BLUNDER;
    else if (loss >= MISTAKE_LOSS) v = Verdict.MISTAKE;
    else if (asGoodAsBest) v = Verdict.BEST;
    else v = Verdict.OKAY;
    if (v == Verdict.BEST && isSacrifice(before, played, after, afterSearch)
        && bestScore < ALREADY_WON && playedScore > -0.5 && MoveGenerator.legalMoves(before).size() > 1) {
      v = Verdict.BRILLIANT;
    }

    // ---------------------------------------------------------------- why
    List<Theme> tactics = Collections.emptyList();
    StringBuilder why = new StringBuilder();
    // Checkmate trumps everything else: say so plainly.
    if (Rules.isCheckmate(after)) {
      if (v != Verdict.BRILLIANT) v = Verdict.BEST;
      return new Result(v, moveText, bestText, bestScore, playedScore, 0,
          Collections.singletonList(Theme.MATE), "Great move, you found the checkmate!");
    }
    if ((v == Verdict.BEST || v == Verdict.BRILLIANT) && isMate(playedScore) && playedScore > 0) {
      String text = String.format(Locale.US, "Great move: it forces checkmate in %d.", mateIn(playedScore));
      if (v == Verdict.BRILLIANT) text = moveText + " is brilliant: it gives up material to force checkmate in " + mateIn(playedScore) + ".";
      return new Result(v, moveText, bestText, bestScore, playedScore, loss, Collections.singletonList(Theme.MATE), text);
    }
    String positional = positionalNote(before, after, best, white, evaluator, v);
    switch (v) {
      case BRILLIANT:
        why.append(moveText).append(" is brilliant: it gives up material, and it is still the best move.");
        tactics = themesOf(before, withReply(played, afterSearch));
        if (!tactics.isEmpty()) why.append(" The idea: ").append(lower(tactics.get(0).label)).append('.');
        break;
      case BEST:
        why.append(playedBest ? "Best move." : "As good as the engine's choice.");
        if (bestScore >= MISS_WIN) {
          // Only name the tactic when it actually wins something.
          tactics = themesOf(before, withReply(played, afterSearch));
          if (!tactics.isEmpty()) why.append(" It sets up ").append(article(tactics.get(0))).append('.');
        }
        if (positional != null) why.append(" It ").append(positional).append('.');
        break;
      case OKAY:
        why.append("Okay, but ").append(bestText).append(" was better");
        if (positional != null) why.append(": this move is not bad, but it ").append(positional);
        why.append('.');
        break;
      case MISS: {
        List<Move> line = beforeSearch.principalVariation;
        tactics = themesOf(before, line);
        if (isMate(bestScore)) {
          why.append(String.format(Locale.US, "You missed mate in %d with %s.", mateIn(bestScore), bestText));
        } else if (!tactics.isEmpty()) {
          why.append("You missed ").append(article(tactics.get(0))).append(": ").append(bestText)
              .append(" wins (").append(pawns(bestScore)).append(").");
        } else {
          why.append("You missed ").append(bestText).append(", which wins (").append(pawns(bestScore)).append(").");
        }
        why.append(" After ").append(moveText).append(" the advantage is ").append(pawns(playedScore)).append('.');
        break;
      }
      case MISTAKE:
      case BLUNDER:
      default: {
        String what = v == Verdict.BLUNDER ? "a blunder" : "a mistake";
        List<Move> punish = afterSearch == null ? Collections.<Move>emptyList() : afterSearch.principalVariation;
        tactics = themesOf(after, punish);
        String reason = punishment(after, afterSearch, punish, tactics, white);
        why.append("This move is ").append(what);
        if (reason != null) why.append(" because ").append(reason);
        why.append('.');
        if (positional != null && reason == null) why.append(" It ").append(positional).append('.');
        why.append(" Better was ").append(bestText).append(" (").append(pawns(bestScore)).append(" instead of ")
            .append(pawns(playedScore)).append(").");
      }
    }
    return new Result(v, moveText, bestText, bestScore, playedScore, loss, tactics, why.toString());
  }

  // ================================================================ tactics

  /** Themes of a line that starts with the side to move in {@code start} (trimmed to odd length, max 5). */
  private static List<Theme> themesOf(Position start, List<Move> line) {
    if (line == null || line.isEmpty()) return Collections.emptyList();
    int n = Math.min(line.size(), 5);
    if (n % 2 == 0) n--;
    List<Theme> all = ThemeDetector.detect(start, line.subList(0, n), line.subList(n, line.size()));
    List<Theme> out = new ArrayList<>();
    for (Theme t : all) {
      if (t != Theme.ENDGAME && t != Theme.QUIET_MOVE && t != Theme.SACRIFICE) out.add(t);
    }
    return out;
  }

  private static List<Move> withReply(Move played, SearchResult afterSearch) {
    List<Move> l = new ArrayList<>();
    l.add(played);
    if (afterSearch != null) l.addAll(afterSearch.principalVariation);
    return l;
  }

  /** "it allows a fork: 24... Nd3+", "it allows mate in 2", "it leaves your knight on f3 hanging". */
  private static String punishment(Position after, SearchResult afterSearch, List<Move> punish, List<Theme> tactics,
                                   boolean moverWhite) {
    if (afterSearch != null && afterSearch.isMateScore() && afterSearch.mateIn() > 0) {
      String first = punish.isEmpty() ? "" : ": " + Notation.line(after, punish.subList(0, 1));
      return "it allows mate in " + afterSearch.mateIn() + first;
    }
    String reply = punish.isEmpty() ? "" : Notation.line(after, punish.subList(0, 1));
    // A piece left where it can be won (the plainest reason, so a hung queen isn't called a "deflection").
    BoardAnalysis a = new BoardAnalysis(after, EvalWeights.defaults());
    int worstSq = -1;
    double worst = 0.9;
    for (int sq = 0; sq < 64; sq++) {
      int p = after.pieceAt(sq);
      if (!Piece.isColor(p, moverWhite) || Piece.type(p) == Piece.KING) continue;
      double l = a.exchangeLoss(sq);
      if (l > worst) {
        worst = l;
        worstSq = sq;
      }
    }
    String hanging = null;
    if (worstSq >= 0) {
      String piece = Piece.name(after.pieceAt(worstSq)).toLowerCase(Locale.US);
      boolean loose = !a.isDefended(worstSq);
      hanging = "it leaves your " + piece + " on " + Square.name(worstSq) + (loose ? " hanging" : " to be won")
          + (reply.isEmpty() ? "" : " (" + reply + ")");
      if (worst >= 2.5) return hanging;
    }
    for (Theme t : tactics) {
      if (t == Theme.MATE || t == Theme.PROMOTION) continue;
      return "it allows " + article(t) + (reply.isEmpty() ? "" : ": " + reply);
    }
    if (hanging != null) return hanging;
    if (!reply.isEmpty() && !punish.isEmpty() && punish.get(0).isCapture()) return reply + " wins material";
    return null;
  }

  /**
   * Gives up 2+ points for good: counting the move and the next four plies of the expected line,
   * the mover is still 2+ points down (so a trade that is won back isn't a sacrifice). A piece
   * left en prise that the opponent declines to take also counts.
   */
  private static boolean isSacrifice(Position before, Move played, Position after, SearchResult afterSearch) {
    List<Move> pv = afterSearch == null ? Collections.<Move>emptyList() : afterSearch.principalVariation;
    if (ThemeDetector.offersMaterial(played, after) && (pv.isEmpty() || pv.get(0).to != played.to)) return true;
    int net = played.isCapture() ? Piece.displayValue(played.captured) : 0;
    for (int i = 0; i < pv.size() && i < 4; i++) {
      Move m = pv.get(i);
      int v = m.isCapture() ? Piece.displayValue(m.captured) : 0;
      net += i % 2 == 0 ? -v : v; // even = opponent's moves
    }
    return net <= -2;
  }

  // ================================================================ the evaluation terms

  /**
   * The evaluation term that differs most between "after the move played" and "after the best
   * move", if it's a structural one and the move itself changed it; phrased for the mover.
   * For BEST moves, the term the move improved most.
   */
  private static String positionalNote(Position before, Position after, Move best, boolean white,
                                       Evaluator evaluator, Verdict v) {
    if (evaluator == null) return null;
    EvalBreakdown now = evaluator.explain(after);
    EvalBreakdown was = evaluator.explain(before);
    double sign = white ? 1 : -1;
    if (v == Verdict.BEST || v == Verdict.BRILLIANT) {
      Term top = null;
      double topGain = TERM_THRESHOLD;
      for (Term t : PHRASES.keySet()) {
        double gain = sign * (now.contribution(t) - was.contribution(t));
        if (gain > topGain) {
          topGain = gain;
          top = t;
        }
      }
      return top == null ? null : PHRASES.get(top)[1] + detail(top, topGain);
    }
    Position alt = before.copy();
    alt.makeMove(best);
    EvalBreakdown bestBd = evaluator.explain(alt);
    Term top = null;
    double topDrop = -TERM_THRESHOLD;
    for (Term t : PHRASES.keySet()) {
      double vsBest = sign * (now.contribution(t) - bestBd.contribution(t));
      double vsBefore = sign * (now.contribution(t) - was.contribution(t));
      if (vsBest < topDrop && vsBefore < -0.1) {
        topDrop = vsBest;
        top = t;
      }
    }
    return top == null ? null : PHRASES.get(top)[0] + detail(top, topDrop);
  }

  /** " (king safety -0.45)" */
  private static String detail(Term t, double amount) {
    return String.format(Locale.US, " (%s %+.2f)", CATEGORY_NAMES.get(t.category), amount);
  }

  /** Term -> {what it means when it gets worse, what it means when it gets better}. */
  private static final Map<Term, String[]> PHRASES = new EnumMap<>(Term.class);
  private static final Map<Category, String> CATEGORY_NAMES = new EnumMap<>(Category.class);

  static {
    phrase(Term.PAWN_SHIELD_BONUS, "weakens your king by moving a pawn from its shelter", "strengthens your king's pawn shelter");
    phrase(Term.OPEN_FILE_NEAR_KING_PENALTY, "weakens your king by opening a file next to it", "closes the files near your king");
    phrase(Term.KING_ZONE_ATTACK_PENALTY, "weakens your king: more enemy pieces now aim at it", "takes pressure off your king");
    phrase(Term.KING_FEW_ESCAPE_SQUARES_PENALTY, "leaves your king short of escape squares", "gives your king more room");
    phrase(Term.XRAY_ON_KING_PENALTY, "lines your king up with an enemy piece", "steps your king off a dangerous line");
    phrase(Term.CASTLED_KING_BONUS, "takes your king out of its shelter", "tucks your king away safely");
    phrase(Term.LOST_CASTLING_PENALTY, "gives up the right to castle", "keeps your king safe");
    phrase(Term.DOUBLED_PAWN_PENALTY, "damages your pawn structure by doubling your pawns", "undoubles your pawns");
    phrase(Term.ISOLATED_PAWN_PENALTY, "damages your pawn structure by leaving an isolated pawn", "repairs an isolated pawn");
    phrase(Term.BACKWARD_PAWN_PENALTY, "leaves you with a backward pawn", "fixes a backward pawn");
    phrase(Term.CONNECTED_PAWN_BONUS, "splits up your pawns", "connects your pawns");
    phrase(Term.CENTRAL_PAWN_BONUS, "gives up your pawn centre", "takes the centre with a pawn");
    phrase(Term.PASSED_PAWN_BONUS, "gives up a passed pawn", "creates a passed pawn");
    phrase(Term.PASSED_PAWN_ADVANCE_BONUS, "slows down your passed pawn", "pushes your passed pawn closer to queening");
    phrase(Term.KNIGHT_OUTPOST_BONUS, "gives up a strong outpost for your knight", "puts your knight on a strong outpost");
    phrase(Term.BISHOP_OUTPOST_BONUS, "gives up an outpost for your bishop", "puts your bishop on an outpost");
    phrase(Term.BISHOP_PAIR_BONUS, "gives up the bishop pair", "keeps the bishop pair");
    phrase(Term.KNIGHT_RIM_PENALTY, "puts a knight on the rim", "brings a knight back from the rim");
    phrase(Term.KNIGHT_CENTRAL_BONUS, "takes a knight away from the centre", "centralises a knight");
    phrase(Term.ROOK_OPEN_FILE_BONUS, "takes a rook off an open file", "puts a rook on an open file");
    phrase(Term.ROOK_SEVENTH_RANK_BONUS, "takes a rook off the seventh rank", "puts a rook on the seventh rank");
    phrase(Term.CONNECTED_ROOKS_BONUS, "disconnects your rooks", "connects your rooks");
    phrase(Term.UNDEVELOPED_MINOR_PENALTY, "undevelops a piece", "develops a piece");
    phrase(Term.EARLY_QUEEN_PENALTY, "brings the queen out too early", "keeps the queen back until the pieces are out");
    phrase(Term.KNIGHT_MOBILITY, "makes a knight less active", "activates a knight");
    phrase(Term.BISHOP_MOBILITY, "makes a bishop less active", "activates a bishop");
    phrase(Term.ROOK_MOBILITY, "makes a rook less active", "activates a rook");
    phrase(Term.PINNED_TO_KING_FACTOR, "walks into a pin", "gets out of a pin");
    phrase(Term.KING_CENTRALISATION_BONUS, "keeps your king out of play", "brings your king into play");
    CATEGORY_NAMES.put(Category.MATERIAL, "material");
    CATEGORY_NAMES.put(Category.PIECE_SAFETY, "piece safety");
    CATEGORY_NAMES.put(Category.MOBILITY, "mobility");
    CATEGORY_NAMES.put(Category.PINS_AND_REVEALS, "pins");
    CATEGORY_NAMES.put(Category.KING_SAFETY, "king safety");
    CATEGORY_NAMES.put(Category.PAWN_STRUCTURE, "pawn structure");
    CATEGORY_NAMES.put(Category.OUTPOSTS, "outposts");
    CATEGORY_NAMES.put(Category.PIECE_PLACEMENT, "piece placement");
    CATEGORY_NAMES.put(Category.INITIATIVE, "initiative");
    CATEGORY_NAMES.put(Category.ENDGAME, "endgame");
  }

  private static void phrase(Term t, String worse, String better) {
    PHRASES.put(t, new String[] {worse, better});
  }

  // ================================================================ helpers

  /** Winning chance for a score in pawns (mover's view). */
  public static double winChance(double score) {
    if (isMate(score)) return score > 0 ? 1.0 : 0.0;
    double s = Math.max(-20, Math.min(20, score));
    return 1.0 / (1.0 + Math.exp(-0.368 * s));
  }

  private static boolean isMate(double s) {
    return Math.abs(s) > AlphaBetaSearch.MATE_THRESHOLD;
  }

  private static int mateIn(double s) {
    int plies = (int) Math.round(AlphaBetaSearch.MATE - Math.abs(s));
    return (plies + 1) / 2;
  }

  private static String pawns(double s) {
    if (isMate(s)) return s > 0 ? "mate in " + mateIn(s) : "getting mated";
    return String.format(Locale.US, "%+.1f", s);
  }

  private static String moveText(Position before, Move m) {
    return before.fullmoveNumber() + (before.whiteToMove() ? ". " : "... ") + Notation.toSan(before, m);
  }

  private static String article(Theme t) {
    String l = lower(t.label);
    if (t == Theme.MATE) return "checkmate";
    if (t == Theme.REMOVE_THE_DEFENDER) return "a 'remove the defender' tactic";
    return ("aeiou".indexOf(l.charAt(0)) >= 0 ? "an " : "a ") + l;
  }

  private static String lower(String s) {
    return s.toLowerCase(Locale.US);
  }
}
