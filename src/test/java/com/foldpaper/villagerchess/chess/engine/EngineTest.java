package com.foldpaper.villagerchess.chess.engine;

import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.GameStatus;
import com.foldpaper.villagerchess.chess.Move;
import com.foldpaper.villagerchess.chess.Sq;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineTest {
	// Fixed test profiles (small budgets keep the suite fast); villager presets are tested separately.
	static final DifficultyProfile STRONG = new DifficultyProfile(4, 2000, true, 0.0, 0);
	static final DifficultyProfile WEAK = new DifficultyProfile(1, 100, false, 0.35, 150);

	static int findMove(Board board, DifficultyProfile profile, long seed) {
		return Engine.findMove(board, profile, new Random(seed), () -> false);
	}

	@Test
	void findsMateInOne() {
		Board board = Board.fromFen("6k1/5ppp/8/8/8/8/8/R6K w - - 0 1");
		int move = findMove(board, STRONG, 1);
		board.makeMove(move);
		assertEquals(GameStatus.CHECKMATE, board.status(), "Ra8 is mate");
	}

	@Test
	void capturesHangingQueen() {
		Board board = Board.fromFen("3q3k/8/8/3R4/8/8/8/3K4 w - - 0 1");
		int move = findMove(board, STRONG, 1);
		assertEquals(Sq.of("d8"), Move.to64(move), "Rxd8 wins the queen");
	}

	@Test
	void doesNotHangQueen() {
		// White queen on d4 is attacked by the e5 pawn; the engine must not leave it en prise.
		Board board = Board.fromFen("k7/8/8/4p3/3Q4/8/8/K7 w - - 0 1");
		int move = findMove(board, STRONG, 1);
		board.makeMove(move);
		for (int reply : board.legalMoves()) {
			assertNotEquals(Board.QUEEN, board.pieceAt(Move.to64(reply)),
					"black can capture the queen after " + Sq.name(Move.from64(move)) + Sq.name(Move.to64(move)));
		}
	}

	@Test
	void strongBeatsWeakInSelfPlay() {
		Board board = Board.startpos();
		Random strongRandom = new Random(42);
		Random weakRandom = new Random(43);
		int plies = 0;
		while (!board.status().isOver() && plies < 250) {
			boolean whiteToMove = board.sideToMove() == Board.WHITE;
			DifficultyProfile profile = whiteToMove ? STRONG : WEAK;
			Random random = whiteToMove ? strongRandom : weakRandom;
			int move = Engine.findMove(board, profile, random, () -> false);
			board.makeMove(move);
			plies++;
		}
		assertEquals(GameStatus.CHECKMATE, board.status(), "game should end in mate within 250 plies, was " + board.status() + " after " + plies);
		assertEquals(Board.BLACK, board.sideToMove(), "the weak side (black) should be the one mated");
	}

	@Test
	void villagerLevelsProduceProfiles() {
		for (int level = 1; level <= 5; level++) {
			DifficultyProfile profile = DifficultyProfile.forVillagerLevel(level);
			assertTrue(profile.maxDepth() >= 1);
		}
		assertTrue(DifficultyProfile.forVillagerLevel(5).maxDepth() > DifficultyProfile.forVillagerLevel(1).maxDepth());
		assertTrue(DifficultyProfile.forVillagerLevel(1).blunderChance() > DifficultyProfile.forVillagerLevel(5).blunderChance());
	}

	@Test
	void returnsNoneWithNoLegalMoves() {
		Board board = Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"); // stalemate
		assertEquals(Move.NONE, findMove(board, STRONG, 1));
	}
}
