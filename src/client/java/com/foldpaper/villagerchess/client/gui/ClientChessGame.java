package com.foldpaper.villagerchess.client.gui;

import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.Move;
import com.foldpaper.villagerchess.chess.San;
import com.foldpaper.villagerchess.chess.Sq;
import com.foldpaper.villagerchess.net.ChessOpenS2C;
import com.foldpaper.villagerchess.net.ChessOutcome;
import com.foldpaper.villagerchess.net.ChessStateS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side mirror of a game. Reuses the chess core for legal-move highlighting,
 * optimistic move application, SAN history, and captured-piece math; the server's
 * ChessStateS2C remains authoritative.
 */
public final class ClientChessGame {
	/** Pawn-unit piece values for the material-diff display (index = Board piece type). */
	private static final int[] PAWN_UNITS = {0, 1, 3, 3, 5, 9, 0};
	private static final int[] START_COUNTS = {0, 8, 2, 2, 2, 1, 1};

	public enum MoveSound { NONE, MOVE, CAPTURE }

	public final int gameId;
	public final String villagerName;
	public final int villagerLevel;
	/** The actual villager entity for the side panel; null if it isn't loaded client-side. */
	public final LivingEntity villagerEntity;

	public Board board;
	public ChessOutcome outcome = ChessOutcome.ONGOING;
	public boolean playerToMove = true;
	public int lastFrom = -1, lastTo = -1;
	public final List<String> sanHistory = new ArrayList<>();

	public ClientChessGame(ChessOpenS2C payload) {
		this.gameId = payload.gameId();
		this.villagerName = payload.villagerName();
		this.villagerLevel = payload.villagerLevel();
		this.board = Board.fromFen(payload.fen());
		this.villagerEntity = Minecraft.getInstance().level != null
				&& Minecraft.getInstance().level.getEntity(payload.villagerEntityId()) instanceof LivingEntity living
				? living : null;
	}

	/** Applies the server state; returns the sound for a newly learned (villager) move. */
	public MoveSound applyServerState(ChessStateS2C payload) {
		MoveSound sound = MoveSound.NONE;
		if (payload.lastFrom() >= 0 && plyOf(payload.fen()) == sanHistory.size() + 1) {
			int move = deriveMove(payload);
			if (move != Move.NONE) {
				sound = board.isCapture(move) ? MoveSound.CAPTURE : MoveSound.MOVE;
				sanHistory.add(San.of(board, move));
			}
		}
		board = Board.fromFen(payload.fen());
		outcome = payload.outcome();
		playerToMove = payload.playerToMove() && !outcome.isOver();
		lastFrom = payload.lastFrom();
		lastTo = payload.lastTo();
		return sound;
	}

	/** Applies the player's own move immediately so the UI doesn't wait for the round trip. */
	public MoveSound applyLocalMove(int move) {
		MoveSound sound = board.isCapture(move) ? MoveSound.CAPTURE : MoveSound.MOVE;
		sanHistory.add(San.of(board, move));
		board.makeMove(move);
		playerToMove = false;
		lastFrom = Move.from64(move);
		lastTo = Move.to64(move);
		return sound;
	}

	/** Legal destination squares for the piece on {@code from}, empty unless it's the player's turn. */
	public List<Integer> legalTargets(int from) {
		List<Integer> targets = new ArrayList<>();
		if (!playerToMove || outcome.isOver()) return targets;
		for (int move : board.legalMoves()) {
			if (Move.from64(move) == from && !targets.contains(Move.to64(move))) {
				targets.add(Move.to64(move));
			}
		}
		return targets;
	}

	/** The packed legal move for from→to with the given promotion type, or NONE. */
	public int findMove(int from, int to, int promotion) {
		for (int move : board.legalMoves()) {
			if (Move.from64(move) == from && Move.to64(move) == to && Move.promotion(move) == promotion) {
				return move;
			}
		}
		return Move.NONE;
	}

	/** True if moving from→to is a promotion (so the UI must ask which piece). */
	public boolean isPromotion(int from, int to) {
		for (int move : board.legalMoves()) {
			if (Move.from64(move) == from && Move.to64(move) == to && Move.promotion(move) != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Piece types (repeated per count) that {@code color} has lost, biggest first -
	 * i.e. pass BLACK to get the villager's pieces the player captured.
	 * Promotions can make counts go negative; those are clamped to zero.
	 */
	public List<Integer> capturedPieces(int color) {
		int[] counts = new int[7];
		for (int sq = 0; sq < 64; sq++) {
			int piece = board.pieceAt(sq);
			if (piece != 0 && Integer.signum(piece) == color) counts[Math.abs(piece)]++;
		}
		List<Integer> lost = new ArrayList<>();
		for (int type = Board.QUEEN; type >= Board.PAWN; type--) {
			for (int i = 0; i < START_COUNTS[type] - counts[type]; i++) lost.add(type);
		}
		return lost;
	}

	/** Material balance in pawn units from the player's (white's) perspective. */
	public int materialDiff() {
		int diff = 0;
		for (int sq = 0; sq < 64; sq++) {
			int piece = board.pieceAt(sq);
			if (piece != 0) diff += Integer.signum(piece) * PAWN_UNITS[Math.abs(piece)];
		}
		return diff;
	}

	private int deriveMove(ChessStateS2C payload) {
		int promotion = 0;
		int moving = board.pieceAt(payload.lastFrom());
		int toRank = Sq.rank(payload.lastTo());
		if (Math.abs(moving) == Board.PAWN && (toRank == 0 || toRank == 7)) {
			promotion = Math.abs(Board.fromFen(payload.fen()).pieceAt(payload.lastTo()));
		}
		return findMove(payload.lastFrom(), payload.lastTo(), promotion);
	}

	/** Plies played in a FEN: (fullmove-1)*2, plus one if black is to move. */
	private static int plyOf(String fen) {
		String[] parts = fen.trim().split("\\s+");
		int fullmove = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
		return (fullmove - 1) * 2 + (parts[1].equals("b") ? 1 : 0);
	}
}
