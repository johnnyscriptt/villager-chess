package com.foldpaper.villagerchess.chess.engine;

import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.Sq;

/**
 * Material + piece-square evaluation (values from Michniewski's Simplified
 * Evaluation Function), plus a mop-up term that drives won endgames toward mate.
 */
final class Evaluator {
	static final int[] MATERIAL = {0, 100, 320, 330, 500, 900, 0};

	// Visual layout: first row is rank 8, from white's point of view.
	private static final int[] PAWN_PST = {
			0, 0, 0, 0, 0, 0, 0, 0,
			50, 50, 50, 50, 50, 50, 50, 50,
			10, 10, 20, 30, 30, 20, 10, 10,
			5, 5, 10, 25, 25, 10, 5, 5,
			0, 0, 0, 20, 20, 0, 0, 0,
			5, -5, -10, 0, 0, -10, -5, 5,
			5, 10, 10, -20, -20, 10, 10, 5,
			0, 0, 0, 0, 0, 0, 0, 0
	};
	private static final int[] KNIGHT_PST = {
			-50, -40, -30, -30, -30, -30, -40, -50,
			-40, -20, 0, 0, 0, 0, -20, -40,
			-30, 0, 10, 15, 15, 10, 0, -30,
			-30, 5, 15, 20, 20, 15, 5, -30,
			-30, 0, 15, 20, 20, 15, 0, -30,
			-30, 5, 10, 15, 15, 10, 5, -30,
			-40, -20, 0, 5, 5, 0, -20, -40,
			-50, -40, -30, -30, -30, -30, -40, -50
	};
	private static final int[] BISHOP_PST = {
			-20, -10, -10, -10, -10, -10, -10, -20,
			-10, 0, 0, 0, 0, 0, 0, -10,
			-10, 0, 5, 10, 10, 5, 0, -10,
			-10, 5, 5, 10, 10, 5, 5, -10,
			-10, 0, 10, 10, 10, 10, 0, -10,
			-10, 10, 10, 10, 10, 10, 10, -10,
			-10, 5, 0, 0, 0, 0, 5, -10,
			-20, -10, -10, -10, -10, -10, -10, -20
	};
	private static final int[] ROOK_PST = {
			0, 0, 0, 0, 0, 0, 0, 0,
			5, 10, 10, 10, 10, 10, 10, 5,
			-5, 0, 0, 0, 0, 0, 0, -5,
			-5, 0, 0, 0, 0, 0, 0, -5,
			-5, 0, 0, 0, 0, 0, 0, -5,
			-5, 0, 0, 0, 0, 0, 0, -5,
			-5, 0, 0, 0, 0, 0, 0, -5,
			0, 0, 0, 5, 5, 0, 0, 0
	};
	private static final int[] QUEEN_PST = {
			-20, -10, -10, -5, -5, -10, -10, -20,
			-10, 0, 0, 0, 0, 0, 0, -10,
			-10, 0, 5, 5, 5, 5, 0, -10,
			-5, 0, 5, 5, 5, 5, 0, -5,
			0, 0, 5, 5, 5, 5, 0, -5,
			-10, 5, 5, 5, 5, 5, 0, -10,
			-10, 0, 5, 0, 0, 0, 0, -10,
			-20, -10, -10, -5, -5, -10, -10, -20
	};
	private static final int[] KING_PST = {
			-30, -40, -40, -50, -50, -40, -40, -30,
			-30, -40, -40, -50, -50, -40, -40, -30,
			-30, -40, -40, -50, -50, -40, -40, -30,
			-30, -40, -40, -50, -50, -40, -40, -30,
			-20, -30, -30, -40, -40, -30, -30, -20,
			-10, -20, -20, -20, -20, -20, -20, -10,
			20, 20, 0, 0, 0, 0, 20, 20,
			20, 30, 10, 0, 0, 10, 30, 20
	};
	private static final int[][] PST = {null, PAWN_PST, KNIGHT_PST, BISHOP_PST, ROOK_PST, QUEEN_PST, KING_PST};

	private Evaluator() {}

	/** Score in centipawns from the perspective of the side to move. */
	static int evaluate(Board b) {
		int score = 0;
		int whiteMaterial = 0, blackMaterial = 0;
		int whitePieces = 0, blackPieces = 0;
		int whiteKing = 0, blackKing = 0;
		for (int sq = 0; sq < 64; sq++) {
			int piece = b.pieceAt(sq);
			if (piece == 0) continue;
			int type = Math.abs(piece);
			int rank = Sq.rank(sq), file = Sq.file(sq);
			if (piece > 0) {
				score += MATERIAL[type] + PST[type][(7 - rank) * 8 + file];
				whiteMaterial += MATERIAL[type];
				if (type == Board.KING) whiteKing = sq;
				else if (type != Board.PAWN) whitePieces++;
			} else {
				score -= MATERIAL[type] + PST[type][rank * 8 + file];
				blackMaterial += MATERIAL[type];
				if (type == Board.KING) blackKing = sq;
				else if (type != Board.PAWN) blackPieces++;
			}
		}

		if (whiteMaterial - blackMaterial >= 400 && blackPieces <= 1) {
			score += mopUp(whiteKing, blackKing);
		} else if (blackMaterial - whiteMaterial >= 400 && whitePieces <= 1) {
			score -= mopUp(blackKing, whiteKing);
		}
		return b.sideToMove() * score;
	}

	/** Rewards driving the losing king to the edge and bringing the kings together. */
	private static int mopUp(int winnerKing, int loserKing) {
		int file = Sq.file(loserKing), rank = Sq.rank(loserKing);
		int centerDist = Math.max(3 - file, file - 4) + Math.max(3 - rank, rank - 4);
		int kingDist = Math.abs(Sq.file(winnerKing) - file) + Math.abs(Sq.rank(winnerKing) - rank);
		return 10 * centerDist + 4 * (14 - kingDist);
	}
}
