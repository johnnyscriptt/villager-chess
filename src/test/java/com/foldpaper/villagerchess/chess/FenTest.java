package com.foldpaper.villagerchess.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FenTest {
	static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	static final String KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
	static final String EN_PASSANT = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2";

	@Test
	void startposMatchesStartFen() {
		assertEquals(START, Board.startpos().toFen());
	}

	@Test
	void roundTripStart() {
		assertEquals(START, Board.fromFen(START).toFen());
	}

	@Test
	void roundTripKiwipete() {
		assertEquals(KIWIPETE, Board.fromFen(KIWIPETE).toFen());
	}

	@Test
	void roundTripEnPassantSquare() {
		assertEquals(EN_PASSANT, Board.fromFen(EN_PASSANT).toFen());
	}

	@Test
	void sideToMoveParsed() {
		assertEquals(1, Board.fromFen(START).sideToMove());
		assertEquals(-1, Board.fromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1").sideToMove());
	}
}
