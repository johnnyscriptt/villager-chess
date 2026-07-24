package com.foldpaper.villagerchess.chess;

/**
 * Moves are packed ints: bits 0-5 from square (0-63), bits 6-11 to square,
 * bits 12-14 promotion piece type (0 = none, or Board.KNIGHT/BISHOP/ROOK/QUEEN),
 * bits 15-17 special-move kind.
 */
public final class Move {
	public static final int NONE = 0;

	public static final int KIND_NORMAL = 0;
	public static final int KIND_DOUBLE_PUSH = 1;
	public static final int KIND_CASTLE_KING = 2;
	public static final int KIND_CASTLE_QUEEN = 3;
	public static final int KIND_EN_PASSANT = 4;

	private Move() {}

	public static int pack(int from64, int to64, int promotion, int kind) {
		return from64 | (to64 << 6) | (promotion << 12) | (kind << 15);
	}

	public static int from64(int move) {
		return move & 63;
	}

	public static int to64(int move) {
		return (move >> 6) & 63;
	}

	public static int promotion(int move) {
		return (move >> 12) & 7;
	}

	public static int kind(int move) {
		return (move >> 15) & 7;
	}
}
