package com.foldpaper.villagerchess.chess;

import java.util.Arrays;
import java.util.Random;

/**
 * Chess position with make/unmake on a 0x88 board. Pure Java — no Minecraft imports.
 * Pieces are signed: positive white, negative black, types PAWN..KING.
 */
public final class Board {
	public static final int PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;
	public static final int WHITE = 1, BLACK = -1;

	static final int CASTLE_WK = 1, CASTLE_WQ = 2, CASTLE_BK = 4, CASTLE_BQ = 8;

	// Zobrist keys, fixed seed so hashes are stable across runs.
	private static final long[][] ZPIECE = new long[12][128];
	private static final long[] ZCASTLE = new long[16];
	private static final long[] ZEP = new long[8];
	private static final long ZSIDE;
	// Which castling rights survive a move touching each square.
	private static final int[] CASTLE_MASK = new int[128];

	static {
		Random r = new Random(0x5EEDF00DL);
		for (long[] table : ZPIECE) {
			for (int i = 0; i < table.length; i++) table[i] = r.nextLong();
		}
		for (int i = 0; i < ZCASTLE.length; i++) ZCASTLE[i] = r.nextLong();
		for (int i = 0; i < ZEP.length; i++) ZEP[i] = r.nextLong();
		ZSIDE = r.nextLong();
		Arrays.fill(CASTLE_MASK, CASTLE_WK | CASTLE_WQ | CASTLE_BK | CASTLE_BQ);
		CASTLE_MASK[0x04] &= ~(CASTLE_WK | CASTLE_WQ); // e1
		CASTLE_MASK[0x07] &= ~CASTLE_WK;               // h1
		CASTLE_MASK[0x00] &= ~CASTLE_WQ;               // a1
		CASTLE_MASK[0x74] &= ~(CASTLE_BK | CASTLE_BQ); // e8
		CASTLE_MASK[0x77] &= ~CASTLE_BK;               // h8
		CASTLE_MASK[0x70] &= ~CASTLE_BQ;               // a8
	}

	final int[] squares = new int[128];
	int side = WHITE;
	int castling;
	int ep88 = -1;
	int halfmove;
	int fullmove = 1;
	int kingSqWhite = -1, kingSqBlack = -1;
	long zobrist;

	private int ply;
	private int[] undoCaptured = new int[64];
	private int[] undoCastling = new int[64];
	private int[] undoEp = new int[64];
	private int[] undoHalfmove = new int[64];
	private long[] undoZobrist = new long[64];
	private long[] hashHistory = new long[65];

	private Board() {}

	public static Board startpos() {
		return fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}

	public static Board fromFen(String fen) {
		Board board = new Board();
		Fen.parseInto(board, fen);
		board.zobrist = board.computeZobrist();
		board.hashHistory[0] = board.zobrist;
		for (int sq = 0; sq < 128; sq++) {
			if (board.squares[sq] == KING * WHITE) board.kingSqWhite = sq;
			if (board.squares[sq] == KING * BLACK) board.kingSqBlack = sq;
		}
		return board;
	}

	public String toFen() {
		return Fen.write(this);
	}

	public int sideToMove() {
		return side;
	}

	public int fullmoveNumber() {
		return fullmove;
	}

	public int halfmoveClock() {
		return halfmove;
	}

	/** Piece at a 0-63 square (signed, 0 if empty). */
	public int pieceAt(int sq64) {
		return squares[to88(sq64)];
	}

	public int[] legalMoves() {
		return MoveGenerator.legal(this);
	}

	public boolean inCheck() {
		return MoveGenerator.attacked(this, kingSq(side), -side);
	}

	/** True if the move captures something (including en passant). Valid before makeMove. */
	public boolean isCapture(int move) {
		return Move.kind(move) == Move.KIND_EN_PASSANT || squares[to88(Move.to64(move))] != 0;
	}

	public void makeMove(int move) {
		int from = to88(Move.from64(move));
		int to = to88(Move.to64(move));
		int kind = Move.kind(move);
		int promo = Move.promotion(move);
		int piece = squares[from];
		int capSq = kind == Move.KIND_EN_PASSANT ? to - 16 * side : to;
		int captured = squares[capSq];

		if (ply == undoCaptured.length) growStacks();
		undoCaptured[ply] = captured;
		undoCastling[ply] = castling;
		undoEp[ply] = ep88;
		undoHalfmove[ply] = halfmove;
		undoZobrist[ply] = zobrist;

		if (captured != 0) {
			squares[capSq] = 0;
			zobrist ^= zp(captured, capSq);
		}
		squares[from] = 0;
		zobrist ^= zp(piece, from);
		int placed = promo != 0 ? promo * side : piece;
		squares[to] = placed;
		zobrist ^= zp(placed, to);

		if (piece == KING * side) {
			if (side == WHITE) kingSqWhite = to; else kingSqBlack = to;
		}
		if (kind == Move.KIND_CASTLE_KING) {
			moveRook(from + 3, from + 1);
		} else if (kind == Move.KIND_CASTLE_QUEEN) {
			moveRook(from - 4, from - 1);
		}

		int newCastling = castling & CASTLE_MASK[from] & CASTLE_MASK[to];
		zobrist ^= ZCASTLE[castling] ^ ZCASTLE[newCastling];
		castling = newCastling;

		int newEp = kind == Move.KIND_DOUBLE_PUSH ? from + 16 * side : -1;
		if (ep88 != -1) zobrist ^= ZEP[ep88 & 7];
		if (newEp != -1) zobrist ^= ZEP[newEp & 7];
		ep88 = newEp;

		halfmove = (Math.abs(piece) == PAWN || captured != 0) ? 0 : halfmove + 1;
		if (side == BLACK) fullmove++;
		side = -side;
		zobrist ^= ZSIDE;

		ply++;
		hashHistory[ply] = zobrist;
	}

	public void unmakeMove(int move) {
		side = -side;
		if (side == BLACK) fullmove--;
		ply--;
		int from = to88(Move.from64(move));
		int to = to88(Move.to64(move));
		int kind = Move.kind(move);
		int captured = undoCaptured[ply];

		int piece = Move.promotion(move) != 0 ? PAWN * side : squares[to];
		squares[from] = piece;
		squares[to] = 0;
		if (kind == Move.KIND_EN_PASSANT) {
			squares[to - 16 * side] = captured;
		} else if (captured != 0) {
			squares[to] = captured;
		}
		if (piece == KING * side) {
			if (side == WHITE) kingSqWhite = from; else kingSqBlack = from;
		}
		if (kind == Move.KIND_CASTLE_KING) {
			moveRookBack(from + 3, from + 1);
		} else if (kind == Move.KIND_CASTLE_QUEEN) {
			moveRookBack(from - 4, from - 1);
		}

		castling = undoCastling[ply];
		ep88 = undoEp[ply];
		halfmove = undoHalfmove[ply];
		zobrist = undoZobrist[ply];
	}

	public GameStatus status() {
		if (legalMoves().length == 0) {
			return inCheck() ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
		}
		if (halfmove >= 100) return GameStatus.DRAW_FIFTY_MOVE;
		if (repetitions() >= 3) return GameStatus.DRAW_REPETITION;
		if (insufficientMaterial()) return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
		return GameStatus.ONGOING;
	}

	public Board copy() {
		Board board = new Board();
		System.arraycopy(squares, 0, board.squares, 0, 128);
		board.side = side;
		board.castling = castling;
		board.ep88 = ep88;
		board.halfmove = halfmove;
		board.fullmove = fullmove;
		board.kingSqWhite = kingSqWhite;
		board.kingSqBlack = kingSqBlack;
		board.zobrist = zobrist;
		board.hashHistory[0] = zobrist;
		return board;
	}

	int kingSq(int color) {
		return color == WHITE ? kingSqWhite : kingSqBlack;
	}

	private int repetitions() {
		int count = 1;
		int back = Math.min(halfmove, ply);
		for (int i = ply - 2; i >= ply - back; i -= 2) {
			if (hashHistory[i] == zobrist) count++;
		}
		return count;
	}

	private boolean insufficientMaterial() {
		int minors = 0;
		for (int sq = 0; sq < 128; sq++) {
			if ((sq & 0x88) != 0 || squares[sq] == 0) continue;
			int type = Math.abs(squares[sq]);
			if (type == PAWN || type == ROOK || type == QUEEN) return false;
			if (type == KNIGHT || type == BISHOP) minors++;
		}
		return minors <= 1;
	}

	private void moveRook(int rookFrom, int rookTo) {
		int rook = squares[rookFrom];
		squares[rookFrom] = 0;
		squares[rookTo] = rook;
		zobrist ^= zp(rook, rookFrom) ^ zp(rook, rookTo);
	}

	private void moveRookBack(int rookFrom, int rookTo) {
		squares[rookFrom] = squares[rookTo];
		squares[rookTo] = 0;
	}

	private void growStacks() {
		int cap = undoCaptured.length * 2;
		undoCaptured = Arrays.copyOf(undoCaptured, cap);
		undoCastling = Arrays.copyOf(undoCastling, cap);
		undoEp = Arrays.copyOf(undoEp, cap);
		undoHalfmove = Arrays.copyOf(undoHalfmove, cap);
		undoZobrist = Arrays.copyOf(undoZobrist, cap);
		hashHistory = Arrays.copyOf(hashHistory, cap + 1);
	}

	private long computeZobrist() {
		long hash = 0;
		for (int sq = 0; sq < 128; sq++) {
			if ((sq & 0x88) == 0 && squares[sq] != 0) hash ^= zp(squares[sq], sq);
		}
		hash ^= ZCASTLE[castling];
		if (ep88 != -1) hash ^= ZEP[ep88 & 7];
		if (side == BLACK) hash ^= ZSIDE;
		return hash;
	}

	private static long zp(int piece, int sq88) {
		int index = Math.abs(piece) - 1 + (piece < 0 ? 6 : 0);
		return ZPIECE[index][sq88];
	}

	static int to88(int sq64) {
		return sq64 + (sq64 & ~7);
	}

	static int to64(int sq88) {
		return (sq88 + (sq88 & 7)) >> 1;
	}
}
