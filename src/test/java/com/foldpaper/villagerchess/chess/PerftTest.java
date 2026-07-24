package com.foldpaper.villagerchess.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Move generation correctness via perft node counts (standard reference positions
 * from the Chess Programming Wiki). If these pass, castling, en passant,
 * promotion, pins, and check evasion are all correct.
 */
class PerftTest {
	static long perft(Board board, int depth) {
		if (depth == 0) return 1;
		long nodes = 0;
		for (int move : board.legalMoves()) {
			board.makeMove(move);
			nodes += perft(board, depth - 1);
			board.unmakeMove(move);
		}
		return nodes;
	}

	static void assertPerft(String fen, long... expectedPerDepth) {
		Board board = Board.fromFen(fen);
		for (int d = 1; d <= expectedPerDepth.length; d++) {
			assertEquals(expectedPerDepth[d - 1], perft(board, d), fen + " depth " + d);
		}
	}

	@Test
	void startPosition() {
		assertPerft(FenTest.START, 20, 400, 8902, 197281);
	}

	@Test
	void kiwipete() {
		assertPerft(FenTest.KIWIPETE, 48, 2039, 97862);
	}

	@Test
	void position3EnPassantPins() {
		assertPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 14, 191, 2812, 43238);
	}

	@Test
	void position4Promotions() {
		assertPerft("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 6, 264, 9467);
	}

	@Test
	void position5CastleChecks() {
		assertPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 44, 1486, 62379);
	}

	@Test
	void makeUnmakeRestoresState() {
		Board board = Board.fromFen(FenTest.KIWIPETE);
		for (int move : board.legalMoves()) {
			board.makeMove(move);
			board.unmakeMove(move);
			assertEquals(FenTest.KIWIPETE, board.toFen(), "after make/unmake of a move");
		}
	}
}
