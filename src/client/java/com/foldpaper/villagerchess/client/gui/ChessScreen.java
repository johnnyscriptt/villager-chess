package com.foldpaper.villagerchess.client.gui;

import com.foldpaper.villagerchess.VillagerChess;
import com.foldpaper.villagerchess.chess.Board;
import com.foldpaper.villagerchess.chess.Move;
import com.foldpaper.villagerchess.chess.Sq;
import com.foldpaper.villagerchess.net.ChessMoveC2S;
import com.foldpaper.villagerchess.net.ChessOutcome;
import com.foldpaper.villagerchess.net.ChessResignC2S;
import com.foldpaper.villagerchess.net.ChessStateS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * The chess board. Player is always white (bottom). Moves by click-click or drag-and-drop;
 * the villager model sits to the left watching the cursor, captures and SAN history to the right.
 */
public class ChessScreen extends Screen {
	private static final Identifier PIECES = VillagerChess.id("textures/gui/pieces.png");
	private static final int LIGHT_SQUARE = 0xFFEBD6B0, DARK_SQUARE = 0xFFA97A50;
	private static final int FRAME_OUTER = 0xFF3A2E20, FRAME_INNER = 0xFF6B573F;
	private static final int SELECTED_TINT = 0x80FFE94D, LAST_MOVE_TINT = 0x5FFFC83C;
	private static final int TARGET_DOT = 0xB33FA34D, CHECK_TINT = 0x70FF3030, HOVER_TINT = 0x28FFFFFF;
	private static final int PANEL = 0xE0242018, COORD_COLOR = 0xFFC8B490, MUTED_TEXT = 0xFFB0A890;
	private static final int[] PROMO_CHOICES = {Board.QUEEN, Board.ROOK, Board.BISHOP, Board.KNIGHT};
	private static final int VILLAGER_PANEL_W = 70, INFO_PANEL_W = 96, MINI_PIECE = 10;

	private final ClientChessGame game;
	private int square, boardX, boardY, villagerX, infoX;
	private int selected = -1;
	private List<Integer> targets = List.of();
	private int promoFrom = -1, promoTo = -1;
	private int dragFrom = -1;
	private boolean dragging;
	private double dragX, dragY;
	private Button actionButton;
	private boolean playedEndSound;

	public ChessScreen(ClientChessGame game) {
		super(Component.translatable("villagerchess.title", game.villagerName));
		this.game = game;
	}

	public int gameId() {
		return game.gameId;
	}

	public void onServerState(ChessStateS2C payload) {
		boolean wasOver = game.outcome.isOver();
		playSound(game.applyServerState(payload));
		selected = -1;
		targets = List.of();
		clearDrag();
		if (!wasOver && game.outcome == ChessOutcome.PLAYER_WON && !playedEndSound) {
			playedEndSound = true;
			ui(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.9f);
		}
		if (actionButton != null && game.outcome.isOver()) {
			actionButton.setMessage(Component.translatable("villagerchess.close"));
		}
	}

	@Override
	protected void init() {
		int availH = (height - 70) / 8;
		int availW = (width - VILLAGER_PANEL_W - INFO_PANEL_W - 44) / 8;
		square = Math.clamp(Math.min(availH, availW), 14, 32);
		int clusterW = VILLAGER_PANEL_W + 8 + 10 + 8 * square + 8 + INFO_PANEL_W;
		int clusterX = Math.max(4, (width - clusterW) / 2);
		villagerX = clusterX;
		boardX = clusterX + VILLAGER_PANEL_W + 8 + 10;
		infoX = boardX + 8 * square + 8;
		boardY = Math.max(24, (height - (8 * square + 48)) / 2 + 14);

		actionButton = addRenderableWidget(Button.builder(
				Component.translatable(game.outcome.isOver() ? "villagerchess.close" : "villagerchess.resign"),
				b -> onClose())
				.bounds(boardX + 8 * square - 60, boardY + 8 * square + 26, 60, 16)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
		gg.nextStratum();

		gg.centeredText(font, title, width / 2, boardY - 16, 0xFFFFFFFF);
		drawBoard(gg, mouseX, mouseY);
		drawCoordinates(gg);
		drawVillagerPanel(gg, mouseX, mouseY);
		drawInfoPanel(gg);
		gg.centeredText(font, statusLine(), boardX + 4 * square, boardY + 8 * square + 15, MUTED_TEXT);

		if (dragging && dragFrom != -1) {
			int piece = game.board.pieceAt(dragFrom);
			if (piece != 0) drawPiece(gg, piece, (int) dragX - square / 2, (int) dragY - square / 2, square);
		}
		if (promoFrom != -1) drawPromotionPicker(gg);
		if (game.outcome.isOver()) drawEndBanner(gg);
	}

	private void drawBoard(GuiGraphicsExtractor gg, int mouseX, int mouseY) {
		gg.fill(boardX - 6, boardY - 6, boardX + 8 * square + 6, boardY + 8 * square + 6, FRAME_OUTER);
		gg.fill(boardX - 2, boardY - 2, boardX + 8 * square + 2, boardY + 8 * square + 2, FRAME_INNER);
		int hovered = game.playerToMove && !game.outcome.isOver() && promoFrom == -1
				? squareAt(mouseX, mouseY) : -1;
		for (int sq = 0; sq < 64; sq++) {
			int x = cellX(sq), y = cellY(sq);
			gg.fill(x, y, x + square, y + square, (Sq.rank(sq) + Sq.file(sq)) % 2 == 0 ? DARK_SQUARE : LIGHT_SQUARE);
			if (sq == game.lastFrom || sq == game.lastTo) gg.fill(x, y, x + square, y + square, LAST_MOVE_TINT);
			if (sq == hovered) gg.fill(x, y, x + square, y + square, HOVER_TINT);
		}
		if (selected != -1) {
			gg.fill(cellX(selected), cellY(selected), cellX(selected) + square, cellY(selected) + square, SELECTED_TINT);
		}
		if (game.playerToMove && game.board.inCheck()) {
			int kingSq = findKing();
			if (kingSq != -1) {
				gg.fill(cellX(kingSq), cellY(kingSq), cellX(kingSq) + square, cellY(kingSq) + square, CHECK_TINT);
			}
		}
		for (int sq = 0; sq < 64; sq++) {
			if (dragging && sq == dragFrom) continue;
			int piece = game.board.pieceAt(sq);
			if (piece != 0) drawPiece(gg, piece, cellX(sq), cellY(sq), square);
		}
		for (int target : targets) {
			int dot = Math.max(3, square / 4);
			int cx = cellX(target) + square / 2, cy = cellY(target) + square / 2;
			gg.fill(cx - dot / 2, cy - dot / 2, cx + dot - dot / 2, cy + dot - dot / 2, TARGET_DOT);
		}
	}

	private void drawCoordinates(GuiGraphicsExtractor gg) {
		for (int file = 0; file < 8; file++) {
			gg.centeredText(font, String.valueOf((char) ('a' + file)),
					boardX + file * square + square / 2, boardY + 8 * square + 4, COORD_COLOR);
		}
		for (int rank = 0; rank < 8; rank++) {
			gg.text(font, String.valueOf((char) ('1' + rank)),
					boardX - 10, boardY + (7 - rank) * square + square / 2 - 4, COORD_COLOR);
		}
	}

	private void drawVillagerPanel(GuiGraphicsExtractor gg, int mouseX, int mouseY) {
		int x2 = villagerX + VILLAGER_PANEL_W, y2 = boardY + 8 * square;
		gg.fill(villagerX - 2, boardY - 2, x2 + 2, y2 + 2, FRAME_OUTER);
		gg.fill(villagerX, boardY, x2, y2, PANEL);
		if (game.villagerEntity != null && !game.villagerEntity.isRemoved()) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(gg, villagerX, boardY, x2, y2,
					(int) (8 * square * 0.34f), 0.0625f, mouseX, mouseY, game.villagerEntity);
		}
		if (!game.playerToMove && !game.outcome.isOver()) {
			int dots = (int) (System.currentTimeMillis() / 300 % 4);
			gg.centeredText(font, Component.translatable("villagerchess.thinking")
					.append(".".repeat(dots)), villagerX + VILLAGER_PANEL_W / 2, y2 - 12, 0xFFFFF0A0);
		}
		gg.centeredText(font, Component.literal(game.villagerName),
				villagerX + VILLAGER_PANEL_W / 2, y2 + 4, 0xFFFFFFFF);
		gg.centeredText(font, Component.translatable("merchant.level." + game.villagerLevel),
				villagerX + VILLAGER_PANEL_W / 2, y2 + 14, MUTED_TEXT);
	}

	private void drawInfoPanel(GuiGraphicsExtractor gg) {
		int diff = game.materialDiff();
		drawCaptureRow(gg, game.capturedPieces(Board.BLACK), true, infoX, boardY, diff > 0 ? "+" + diff : null);
		drawCaptureRow(gg, game.capturedPieces(Board.WHITE), false, infoX, boardY + 13, diff < 0 ? "+" + -diff : null);

		int listY = boardY + 30;
		int maxLines = Math.max(1, (8 * square - 32) / 10);
		List<String> lines = moveLines();
		int start = Math.max(0, lines.size() - maxLines);
		for (int i = start; i < lines.size(); i++) {
			gg.text(font, lines.get(i), infoX + 2, listY + (i - start) * 10, MUTED_TEXT);
		}
	}

	private void drawCaptureRow(GuiGraphicsExtractor gg, List<Integer> types, boolean blackPieces,
			int x, int y, String diffLabel) {
		int step = types.size() <= 1 ? MINI_PIECE : Math.min(MINI_PIECE, (INFO_PANEL_W - 24) / types.size());
		for (int i = 0; i < types.size(); i++) {
			float u = (types.get(i) - 1) * 64;
			gg.blit(RenderPipelines.GUI_TEXTURED, PIECES, x + i * step, y,
					u, blackPieces ? 64 : 0, MINI_PIECE, MINI_PIECE, 64, 64, 384, 128);
		}
		if (diffLabel != null) {
			gg.text(font, diffLabel, x + INFO_PANEL_W - 18, y + 1, 0xFF7FD87F);
		}
	}

	private List<String> moveLines() {
		List<String> lines = new java.util.ArrayList<>();
		for (int i = 0; i < game.sanHistory.size(); i += 2) {
			String line = (i / 2 + 1) + ". " + game.sanHistory.get(i);
			if (i + 1 < game.sanHistory.size()) line += " " + game.sanHistory.get(i + 1);
			lines.add(line);
		}
		return lines;
	}

	private void drawPromotionPicker(GuiGraphicsExtractor gg) {
		gg.nextStratum();
		gg.fill(boardX - 6, boardY - 6, boardX + 8 * square + 6, boardY + 8 * square + 6, 0x90000000);
		int cell = square + 8;
		int x0 = boardX + 4 * square - 2 * cell, y0 = boardY + 4 * square - cell / 2;
		gg.fill(x0 - 4, y0 - 4, x0 + 4 * cell + 4, y0 + cell + 4, 0xF0141210);
		for (int i = 0; i < 4; i++) {
			drawPiece(gg, PROMO_CHOICES[i], x0 + i * cell + 4, y0 + 4, square);
		}
	}

	private void drawEndBanner(GuiGraphicsExtractor gg) {
		gg.nextStratum();
		gg.fill(0, 0, width, height, 0x70101010);
		int cx = boardX + 4 * square, cy = boardY + 4 * square;
		gg.fill(cx - 90, cy - 26, cx + 90, cy + 26, 0xF01E1A14);
		gg.fill(cx - 90, cy - 26, cx + 90, cy - 24, FRAME_INNER);
		gg.fill(cx - 90, cy + 24, cx + 90, cy + 26, FRAME_INNER);
		gg.pose().pushMatrix();
		gg.pose().scale(2, 2);
		gg.centeredText(font, bannerTitle(), cx / 2, (cy - 14) / 2, 0xFFFFE080);
		gg.pose().popMatrix();
		gg.centeredText(font, Component.translatable("villagerchess.banner.close_hint"), cx, cy + 8, MUTED_TEXT);
	}

	private Component bannerTitle() {
		return switch (game.outcome) {
			case PLAYER_WON -> Component.translatable("villagerchess.banner.win");
			case VILLAGER_WON -> Component.translatable("villagerchess.banner.loss");
			case PLAYER_RESIGNED -> Component.translatable("villagerchess.status.resigned");
			case ABANDONED -> Component.translatable("villagerchess.status.abandoned");
			default -> Component.translatable("villagerchess.banner.draw");
		};
	}

	private void drawPiece(GuiGraphicsExtractor gg, int piece, int x, int y, int size) {
		float u = (Math.abs(piece) - 1) * 64;
		float v = piece > 0 ? 0 : 64;
		gg.blit(RenderPipelines.GUI_TEXTURED, PIECES, x, y, u, v, size, size, 64, 64, 384, 128);
	}

	private Component statusLine() {
		if (game.outcome != ChessOutcome.ONGOING) return Component.empty();
		if (!game.playerToMove) return Component.translatable("villagerchess.status.thinking", game.villagerName);
		return game.board.inCheck()
				? Component.translatable("villagerchess.status.your_move_check")
				: Component.translatable("villagerchess.status.your_move");
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (event.button() != 0) return false;
		int x = (int) event.x(), y = (int) event.y();

		if (game.outcome.isOver()) {
			onClose();
			return true;
		}
		if (promoFrom != -1) {
			handlePromotionClick(x, y);
			return true;
		}
		if (!game.playerToMove) return false;

		int sq = squareAt(x, y);
		if (sq == -1) {
			clearSelection();
			return false;
		}
		if (selected != -1 && targets.contains(sq)) {
			attemptMove(selected, sq);
			clearSelection();
		} else if (game.board.pieceAt(sq) > 0) { // own (white) piece: select and arm drag
			selected = sq;
			targets = game.legalTargets(sq);
			dragFrom = sq;
			dragging = false;
		} else {
			clearSelection();
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragXAmount, double dragYAmount) {
		if (dragFrom != -1 && game.playerToMove && promoFrom == -1) {
			dragging = true;
			dragX = event.x();
			dragY = event.y();
			return true;
		}
		return super.mouseDragged(event, dragXAmount, dragYAmount);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging && dragFrom != -1) {
			int sq = squareAt((int) event.x(), (int) event.y());
			if (sq != -1 && sq != dragFrom && targets.contains(sq)) {
				attemptMove(dragFrom, sq);
				clearSelection();
			}
			// released on the home square (or off-target): keep the selection, back to click-click
			clearDrag();
			return true;
		}
		return super.mouseReleased(event);
	}

	private void attemptMove(int from, int to) {
		if (game.isPromotion(from, to)) {
			promoFrom = from;
			promoTo = to;
		} else {
			sendMove(from, to, 0);
		}
	}

	private void handlePromotionClick(int x, int y) {
		int cell = square + 8;
		int x0 = boardX + 4 * square - 2 * cell, y0 = boardY + 4 * square - cell / 2;
		if (y >= y0 && y < y0 + cell && x >= x0 && x < x0 + 4 * cell) {
			sendMove(promoFrom, promoTo, PROMO_CHOICES[(x - x0) / cell]);
		}
		promoFrom = -1;
		promoTo = -1;
	}

	private void sendMove(int from, int to, int promotion) {
		int move = game.findMove(from, to, promotion);
		if (move == Move.NONE) return;
		playSound(game.applyLocalMove(move));
		ClientPlayNetworking.send(new ChessMoveC2S(game.gameId, from, to, promotion));
	}

	private void playSound(ClientChessGame.MoveSound sound) {
		switch (sound) {
			case MOVE -> ui(SoundEvents.WOOD_PLACE, 1.4f, 0.5f);
			case CAPTURE -> ui(SoundEvents.WOOD_BREAK, 1.1f, 0.6f);
			case NONE -> {}
		}
	}

	private void ui(SoundEvent event, float pitch, float volume) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}

	private void clearSelection() {
		selected = -1;
		targets = List.of();
		clearDrag();
	}

	private void clearDrag() {
		dragFrom = -1;
		dragging = false;
	}

	private int findKing() {
		for (int sq = 0; sq < 64; sq++) {
			if (game.board.pieceAt(sq) == Board.KING) return sq;
		}
		return -1;
	}

	private int squareAt(int x, int y) {
		int file = Math.floorDiv(x - boardX, square);
		int row = Math.floorDiv(y - boardY, square);
		if (file < 0 || file > 7 || row < 0 || row > 7) return -1;
		return (7 - row) * 8 + file;
	}

	private int cellX(int sq) {
		return boardX + Sq.file(sq) * square;
	}

	private int cellY(int sq) {
		return boardY + (7 - Sq.rank(sq)) * square;
	}

	@Override
	public void onClose() {
		if (!game.outcome.isOver()) {
			ClientPlayNetworking.send(new ChessResignC2S(game.gameId));
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false; // the villager must keep thinking while the board is open
	}
}
