package com.foldpaper.villagerchess.chess;

import java.util.Arrays;

/** Legal move generation and attack detection on a {@link Board}. */
final class MoveGenerator {
	private static final int[] KNIGHT_OFFSETS = {31, 33, 14, 18, -31, -33, -14, -18};
	private static final int[] KING_OFFSETS = {1, -1, 16, -16, 15, 17, -15, -17};
	private static final int[] BISHOP_DIRS = {15, 17, -15, -17};
	private static final int[] ROOK_DIRS = {1, -1, 16, -16};

	private MoveGenerator() {}

	static int[] legal(Board b) {
		int[] buf = new int[256];
		int pseudoCount = pseudo(b, buf);
		int mover = b.side;
		int legalCount = 0;
		for (int i = 0; i < pseudoCount; i++) {
			int move = buf[i];
			b.makeMove(move);
			if (!attacked(b, b.kingSq(mover), -mover)) {
				buf[legalCount++] = move;
			}
			b.unmakeMove(move);
		}
		return Arrays.copyOf(buf, legalCount);
	}

	/** True if {@code sq88} is attacked by any piece of color {@code by}. */
	static boolean attacked(Board b, int sq88, int by) {
		// Pawns: a white pawn on sq-15/sq-17 attacks sq (mirrored for black).
		int p1 = sq88 - 15 * by, p2 = sq88 - 17 * by;
		if (valid(p1) && b.squares[p1] == Board.PAWN * by) return true;
		if (valid(p2) && b.squares[p2] == Board.PAWN * by) return true;
		for (int o : KNIGHT_OFFSETS) {
			int sq = sq88 + o;
			if (valid(sq) && b.squares[sq] == Board.KNIGHT * by) return true;
		}
		for (int o : KING_OFFSETS) {
			int sq = sq88 + o;
			if (valid(sq) && b.squares[sq] == Board.KING * by) return true;
		}
		for (int dir : BISHOP_DIRS) {
			for (int sq = sq88 + dir; valid(sq); sq += dir) {
				int piece = b.squares[sq];
				if (piece == 0) continue;
				if (piece == Board.BISHOP * by || piece == Board.QUEEN * by) return true;
				break;
			}
		}
		for (int dir : ROOK_DIRS) {
			for (int sq = sq88 + dir; valid(sq); sq += dir) {
				int piece = b.squares[sq];
				if (piece == 0) continue;
				if (piece == Board.ROOK * by || piece == Board.QUEEN * by) return true;
				break;
			}
		}
		return false;
	}

	private static int pseudo(Board b, int[] buf) {
		int n = 0;
		int side = b.side;
		for (int from = 0; from < 128; from++) {
			if ((from & 0x88) != 0) continue;
			int piece = b.squares[from];
			if (piece == 0 || Integer.signum(piece) != side) continue;
			switch (Math.abs(piece)) {
				case Board.PAWN -> n = pawnMoves(b, from, buf, n);
				case Board.KNIGHT -> n = leaperMoves(b, from, KNIGHT_OFFSETS, buf, n);
				case Board.KING -> n = leaperMoves(b, from, KING_OFFSETS, buf, n);
				case Board.BISHOP -> n = sliderMoves(b, from, BISHOP_DIRS, buf, n);
				case Board.ROOK -> n = sliderMoves(b, from, ROOK_DIRS, buf, n);
				case Board.QUEEN -> {
					n = sliderMoves(b, from, BISHOP_DIRS, buf, n);
					n = sliderMoves(b, from, ROOK_DIRS, buf, n);
				}
			}
		}
		n = castleMoves(b, buf, n);
		return n;
	}

	private static int pawnMoves(Board b, int from, int[] buf, int n) {
		int side = b.side;
		int push = from + 16 * side;
		int startRank = side == Board.WHITE ? 1 : 6;
		int promoRank = side == Board.WHITE ? 7 : 0;
		if (valid(push) && b.squares[push] == 0) {
			n = addPawnMove(from, push, promoRank, buf, n);
			int dbl = from + 32 * side;
			if ((from >> 4) == startRank && b.squares[dbl] == 0) {
				buf[n++] = Move.pack(Board.to64(from), Board.to64(dbl), 0, Move.KIND_DOUBLE_PUSH);
			}
		}
		for (int capDir : new int[] {15 * side, 17 * side}) {
			int to = from + capDir;
			if (!valid(to)) continue;
			int target = b.squares[to];
			if (target != 0 && Integer.signum(target) == -side) {
				n = addPawnMove(from, to, promoRank, buf, n);
			} else if (to == b.ep88) {
				buf[n++] = Move.pack(Board.to64(from), Board.to64(to), 0, Move.KIND_EN_PASSANT);
			}
		}
		return n;
	}

	private static int addPawnMove(int from, int to, int promoRank, int[] buf, int n) {
		if ((to >> 4) == promoRank) {
			for (int promo = Board.QUEEN; promo >= Board.KNIGHT; promo--) {
				buf[n++] = Move.pack(Board.to64(from), Board.to64(to), promo, Move.KIND_NORMAL);
			}
		} else {
			buf[n++] = Move.pack(Board.to64(from), Board.to64(to), 0, Move.KIND_NORMAL);
		}
		return n;
	}

	private static int leaperMoves(Board b, int from, int[] offsets, int[] buf, int n) {
		for (int o : offsets) {
			int to = from + o;
			if (!valid(to)) continue;
			int target = b.squares[to];
			if (target == 0 || Integer.signum(target) == -b.side) {
				buf[n++] = Move.pack(Board.to64(from), Board.to64(to), 0, Move.KIND_NORMAL);
			}
		}
		return n;
	}

	private static int sliderMoves(Board b, int from, int[] dirs, int[] buf, int n) {
		for (int dir : dirs) {
			for (int to = from + dir; valid(to); to += dir) {
				int target = b.squares[to];
				if (target == 0) {
					buf[n++] = Move.pack(Board.to64(from), Board.to64(to), 0, Move.KIND_NORMAL);
				} else {
					if (Integer.signum(target) == -b.side) {
						buf[n++] = Move.pack(Board.to64(from), Board.to64(to), 0, Move.KIND_NORMAL);
					}
					break;
				}
			}
		}
		return n;
	}

	private static int castleMoves(Board b, int[] buf, int n) {
		int side = b.side;
		int king = side == Board.WHITE ? 0x04 : 0x74;
		int kingRight = side == Board.WHITE ? Board.CASTLE_WK : Board.CASTLE_BK;
		int queenRight = side == Board.WHITE ? Board.CASTLE_WQ : Board.CASTLE_BQ;
		if ((b.castling & (kingRight | queenRight)) == 0) return n;
		if (attacked(b, king, -side)) return n;
		if ((b.castling & kingRight) != 0
				&& b.squares[king + 1] == 0 && b.squares[king + 2] == 0
				&& !attacked(b, king + 1, -side) && !attacked(b, king + 2, -side)) {
			buf[n++] = Move.pack(Board.to64(king), Board.to64(king + 2), 0, Move.KIND_CASTLE_KING);
		}
		if ((b.castling & queenRight) != 0
				&& b.squares[king - 1] == 0 && b.squares[king - 2] == 0 && b.squares[king - 3] == 0
				&& !attacked(b, king - 1, -side) && !attacked(b, king - 2, -side)) {
			buf[n++] = Move.pack(Board.to64(king), Board.to64(king - 2), 0, Move.KIND_CASTLE_QUEEN);
		}
		return n;
	}

	private static boolean valid(int sq88) {
		return (sq88 & 0x88) == 0;
	}
}
