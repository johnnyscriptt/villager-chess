package com.foldpaper.villagerchess.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SanTest {
	static String san(String fen, String from, String to, int promotion) {
		Board board = Board.fromFen(fen);
		for (int move : board.legalMoves()) {
			if (Move.from64(move) == Sq.of(from) && Move.to64(move) == Sq.of(to)
					&& Move.promotion(move) == promotion) {
				return San.of(board, move);
			}
		}
		fail("no legal move " + from + to);
		return null;
	}

	static String san(String fen, String from, String to) {
		return san(fen, from, to, 0);
	}

	@Test
	void pawnPush() {
		assertEquals("e4", san(FenTest.START, "e2", "e4"));
	}

	@Test
	void knightMove() {
		assertEquals("Nf3", san(FenTest.START, "g1", "f3"));
	}

	@Test
	void pawnCapture() {
		assertEquals("exd5", san("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "e4", "d5"));
	}

	@Test
	void enPassantCapture() {
		assertEquals("exd6", san("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3", "e5", "d6"));
	}

	@Test
	void pieceCapture() {
		assertEquals("Nxd5", san("k7/8/8/3p4/8/4N3/8/K7 w - - 0 1", "e3", "d5"));
	}

	@Test
	void castleKingside() {
		assertEquals("O-O", san(FenTest.KIWIPETE, "e1", "g1"));
	}

	@Test
	void castleQueenside() {
		assertEquals("O-O-O", san(FenTest.KIWIPETE, "e1", "c1"));
	}

	@Test
	void promotionToQueen() {
		assertEquals("a8=Q", san("8/P6k/8/8/8/8/8/K7 w - - 0 1", "a7", "a8", Board.QUEEN));
	}

	@Test
	void checkSuffix() {
		assertEquals("Rf8+", san("4k3/8/8/8/8/8/8/4KR2 w - - 0 1", "f1", "f8"));
	}

	@Test
	void mateSuffix() {
		assertEquals("Qh4#", san("rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2", "d8", "h4"));
	}

	@Test
	void fileDisambiguation() {
		assertEquals("Nbd3", san("k7/8/8/8/8/8/1N3N2/K7 w - - 0 1", "b2", "d3"));
	}

	@Test
	void rankDisambiguation() {
		assertEquals("R1a3", san("1k6/8/8/R7/8/8/8/R3K3 w - - 0 1", "a1", "a3"));
	}
}
