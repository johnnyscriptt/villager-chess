package com.foldpaper.villagerchess.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class GameEndTest {
	/** Plays the move matching the given coordinates (e.g. "g1", "f3") or fails. */
	static void play(Board board, String from, String to) {
		for (int move : board.legalMoves()) {
			if (Move.from64(move) == Sq.of(from) && Move.to64(move) == Sq.of(to) && Move.promotion(move) == 0) {
				board.makeMove(move);
				return;
			}
		}
		fail("no legal move " + from + to);
	}

	@Test
	void foolsMateIsCheckmate() {
		Board board = Board.fromFen("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
		assertEquals(GameStatus.CHECKMATE, board.status());
		assertTrue(board.inCheck());
	}

	@Test
	void backRankMateIsCheckmate() {
		Board board = Board.fromFen("R5k1/5ppp/8/8/8/8/8/6K1 b - - 0 1");
		assertEquals(GameStatus.CHECKMATE, board.status());
	}

	@Test
	void stalemateDetected() {
		Board board = Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
		assertEquals(GameStatus.STALEMATE, board.status());
	}

	@Test
	void fiftyMoveRuleDraw() {
		Board board = Board.fromFen("8/8/8/4k3/8/4K3/4R3/8 w - - 100 80");
		assertEquals(GameStatus.DRAW_FIFTY_MOVE, board.status());
	}

	@Test
	void checkmateBeatsFiftyMoveRule() {
		Board board = Board.fromFen("R5k1/5ppp/8/8/8/8/8/6K1 b - - 100 80");
		assertEquals(GameStatus.CHECKMATE, board.status());
	}

	@Test
	void threefoldRepetitionDraw() {
		Board board = Board.startpos();
		play(board, "g1", "f3"); play(board, "g8", "f6");
		play(board, "f3", "g1"); play(board, "f6", "g8");
		assertEquals(GameStatus.ONGOING, board.status());
		play(board, "g1", "f3"); play(board, "g8", "f6");
		play(board, "f3", "g1"); play(board, "f6", "g8");
		assertEquals(GameStatus.DRAW_REPETITION, board.status());
	}

	@Test
	void bareKingsInsufficientMaterial() {
		Board board = Board.fromFen("8/8/8/4k3/8/4K3/8/8 w - - 0 1");
		assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, board.status());
	}

	@Test
	void kingBishopVsKingInsufficientMaterial() {
		Board board = Board.fromFen("8/8/8/4k3/8/4KB2/8/8 w - - 0 1");
		assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, board.status());
	}

	@Test
	void promotionMovesExist() {
		Board board = Board.fromFen("8/P6k/8/8/8/8/8/K7 w - - 0 1");
		int promotions = 0;
		for (int move : board.legalMoves()) {
			if (Move.promotion(move) != 0) promotions++;
		}
		assertEquals(4, promotions, "a7-a8 promotes to N, B, R, Q");
	}
}
