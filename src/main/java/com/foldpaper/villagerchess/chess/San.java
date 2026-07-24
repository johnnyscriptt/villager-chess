package com.foldpaper.villagerchess.chess;

/** Standard Algebraic Notation for a move on a given position. */
public final class San {
	private static final String PIECE_LETTERS = " PNBRQK";

	private San() {}

	/** Renders {@code move} (which must be legal on {@code board}) as SAN, e.g. "Nbd3", "exd6", "O-O", "e8=Q#". */
	public static String of(Board board, int move) {
		int kind = Move.kind(move);
		String base;
		if (kind == Move.KIND_CASTLE_KING) {
			base = "O-O";
		} else if (kind == Move.KIND_CASTLE_QUEEN) {
			base = "O-O-O";
		} else {
			base = describe(board, move);
		}
		return base + suffix(board, move);
	}

	private static String describe(Board board, int move) {
		int from = Move.from64(move);
		int to = Move.to64(move);
		int type = Math.abs(board.pieceAt(from));
		boolean capture = board.isCapture(move);
		StringBuilder san = new StringBuilder();
		if (type == Board.PAWN) {
			if (capture) san.append((char) ('a' + Sq.file(from))).append('x');
		} else {
			san.append(PIECE_LETTERS.charAt(type));
			san.append(disambiguation(board, move, type));
			if (capture) san.append('x');
		}
		san.append(Sq.name(to));
		if (Move.promotion(move) != 0) {
			san.append('=').append(PIECE_LETTERS.charAt(Move.promotion(move)));
		}
		return san.toString();
	}

	private static String disambiguation(Board board, int move, int type) {
		int from = Move.from64(move);
		int to = Move.to64(move);
		boolean ambiguous = false, sameFile = false, sameRank = false;
		for (int other : board.legalMoves()) {
			int otherFrom = Move.from64(other);
			if (otherFrom == from || Move.to64(other) != to) continue;
			if (Math.abs(board.pieceAt(otherFrom)) != type) continue;
			ambiguous = true;
			if (Sq.file(otherFrom) == Sq.file(from)) sameFile = true;
			if (Sq.rank(otherFrom) == Sq.rank(from)) sameRank = true;
		}
		if (!ambiguous) return "";
		if (!sameFile) return String.valueOf((char) ('a' + Sq.file(from)));
		if (!sameRank) return String.valueOf((char) ('1' + Sq.rank(from)));
		return Sq.name(from);
	}

	private static String suffix(Board board, int move) {
		board.makeMove(move);
		String suffix = "";
		if (board.inCheck()) {
			suffix = board.legalMoves().length == 0 ? "#" : "+";
		}
		board.unmakeMove(move);
		return suffix;
	}
}
