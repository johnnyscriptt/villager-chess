package com.foldpaper.villagerchess.chess.engine;

import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.Move;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

/** Negamax alpha-beta search with iterative deepening and per-level weakening. */
public final class Engine {
	private static final int MATE = 32000;
	private static final int INF = 32500;
	private static final int BLUNDER_WINDOW_CP = 300;
	private static final int MAX_QUIESCE_PLY = 40;

	private Engine() {}

	/**
	 * Picks a move for the side to move, or {@link Move#NONE} if there is none.
	 * Never mutates the caller's board. Deterministic for a given seed and position.
	 */
	public static int findMove(Board board, DifficultyProfile profile, Random random, BooleanSupplier stop) {
		Board b = board.copy();
		int[] rootMoves = b.legalMoves();
		if (rootMoves.length == 0) return Move.NONE;
		if (rootMoves.length == 1) return rootMoves[0];

		Search search = new Search(b, profile.quiescence(),
				System.nanoTime() + profile.timeBudgetMs() * 1_000_000L, stop);
		int[] scores = new int[rootMoves.length];
		int[] iterScores = new int[rootMoves.length];
		boolean completedAny = false;
		for (int depth = 1; depth <= profile.maxDepth(); depth++) {
			if (!search.searchRoot(rootMoves, iterScores, depth)) break;
			System.arraycopy(iterScores, 0, scores, 0, scores.length);
			completedAny = true;
		}
		if (!completedAny) return rootMoves[random.nextInt(rootMoves.length)];

		return pick(rootMoves, scores, profile, random);
	}

	private static int pick(int[] moves, int[] scores, DifficultyProfile profile, Random random) {
		int best = scores[0];
		for (int score : scores) best = Math.max(best, score);

		if (profile.blunderChance() > 0 && random.nextDouble() < profile.blunderChance()) {
			List<Integer> pool = new ArrayList<>();
			for (int i = 0; i < moves.length; i++) {
				if (scores[i] >= best - BLUNDER_WINDOW_CP) pool.add(moves[i]);
			}
			return pool.get(random.nextInt(pool.size()));
		}

		int noise = profile.evalNoiseCp();
		int bestMove = moves[0], bestNoisy = Integer.MIN_VALUE;
		for (int i = 0; i < moves.length; i++) {
			int noisy = scores[i] + (noise > 0 ? random.nextInt(2 * noise + 1) - noise : 0);
			if (noisy > bestNoisy) {
				bestNoisy = noisy;
				bestMove = moves[i];
			}
		}
		return bestMove;
	}

	private static final class Search {
		private final Board b;
		private final boolean quiescence;
		private final long deadline;
		private final BooleanSupplier stop;
		private long nodes;
		private boolean aborted;

		Search(Board b, boolean quiescence, long deadline, BooleanSupplier stop) {
			this.b = b;
			this.quiescence = quiescence;
			this.deadline = deadline;
			this.stop = stop;
		}

		/**
		 * Scores every root move with a full window (so weaker levels can pick from
		 * a meaningful spread). Returns false if the search ran out of time mid-depth.
		 */
		boolean searchRoot(int[] moves, int[] scores, int depth) {
			for (int i = 0; i < moves.length; i++) {
				b.makeMove(moves[i]);
				int score = -negamax(depth - 1, -INF, INF, 1);
				b.unmakeMove(moves[i]);
				if (aborted) return false;
				scores[i] = score;
			}
			return true;
		}

		private int negamax(int depth, int alpha, int beta, int ply) {
			if ((++nodes & 1023) == 0 && (stop.getAsBoolean() || System.nanoTime() > deadline)) {
				aborted = true;
			}
			if (aborted) return 0;
			if (b.halfmoveClock() >= 100) return 0;
			if (depth <= 0) {
				return quiescence ? quiesce(alpha, beta, ply) : evaluateOrMate(ply);
			}
			int[] moves = b.legalMoves();
			if (moves.length == 0) return b.inCheck() ? -(MATE - ply) : 0;
			order(moves);
			int best = -INF;
			for (int move : moves) {
				b.makeMove(move);
				int score = -negamax(depth - 1, -beta, -alpha, ply + 1);
				b.unmakeMove(move);
				if (aborted) return 0;
				best = Math.max(best, score);
				alpha = Math.max(alpha, score);
				if (alpha >= beta) break;
			}
			return best;
		}

		private int quiesce(int alpha, int beta, int ply) {
			if ((++nodes & 1023) == 0 && (stop.getAsBoolean() || System.nanoTime() > deadline)) {
				aborted = true;
			}
			if (aborted) return 0;
			boolean check = b.inCheck();
			int[] moves = b.legalMoves();
			if (moves.length == 0) return check ? -(MATE - ply) : 0;

			int best = -INF;
			if (!check) {
				best = Evaluator.evaluate(b);
				if (ply >= MAX_QUIESCE_PLY || best >= beta) return best;
				alpha = Math.max(alpha, best);
			}
			order(moves);
			for (int move : moves) {
				// Outside check, only captures and promotions keep the search "noisy".
				if (!check && !b.isCapture(move) && Move.promotion(move) == 0) continue;
				b.makeMove(move);
				int score = -quiesce(-beta, -alpha, ply + 1);
				b.unmakeMove(move);
				if (aborted) return 0;
				best = Math.max(best, score);
				alpha = Math.max(alpha, score);
				if (alpha >= beta) break;
			}
			return best;
		}

		private int evaluateOrMate(int ply) {
			if (b.legalMoves().length == 0) return b.inCheck() ? -(MATE - ply) : 0;
			return Evaluator.evaluate(b);
		}

		/** MVV-LVA-ish ordering: big captures and promotions searched first. */
		private void order(int[] moves) {
			int n = moves.length;
			int[] keys = new int[n];
			for (int i = 0; i < n; i++) {
				int move = moves[i];
				int key = 0;
				int victim = b.pieceAt(Move.to64(move));
				if (victim != 0 || Move.kind(move) == Move.KIND_EN_PASSANT) {
					int victimType = victim != 0 ? Math.abs(victim) : Board.PAWN;
					int attackerType = Math.abs(b.pieceAt(Move.from64(move)));
					key = 10_000 + Evaluator.MATERIAL[victimType] - attackerType;
				}
				key += Move.promotion(move) * 100;
				keys[i] = key;
			}
			for (int i = 1; i < n; i++) {
				int key = keys[i], move = moves[i];
				int j = i - 1;
				while (j >= 0 && keys[j] < key) {
					keys[j + 1] = keys[j];
					moves[j + 1] = moves[j];
					j--;
				}
				keys[j + 1] = key;
				moves[j + 1] = move;
			}
		}
	}
}
