package com.foldpaper.villagerchess.chess;

/** FEN parsing and serialization. Also the wire format for board state. */
final class Fen {
	private static final String PIECE_CHARS = "PNBRQK";

	private Fen() {}

	static void parseInto(Board board, String fen) {
		String[] parts = fen.trim().split("\\s+");
		if (parts.length < 2) throw new IllegalArgumentException("bad FEN: " + fen);

		int rank = 7, file = 0;
		for (char c : parts[0].toCharArray()) {
			if (c == '/') {
				rank--;
				file = 0;
			} else if (Character.isDigit(c)) {
				file += c - '0';
			} else {
				int type = PIECE_CHARS.indexOf(Character.toUpperCase(c)) + 1;
				if (type == 0 || rank < 0 || file > 7) throw new IllegalArgumentException("bad FEN: " + fen);
				board.squares[rank * 16 + file] = Character.isUpperCase(c) ? type : -type;
				file++;
			}
		}

		board.side = parts[1].equals("w") ? Board.WHITE : Board.BLACK;

		board.castling = 0;
		if (parts.length > 2 && !parts[2].equals("-")) {
			for (char c : parts[2].toCharArray()) {
				switch (c) {
					case 'K' -> board.castling |= Board.CASTLE_WK;
					case 'Q' -> board.castling |= Board.CASTLE_WQ;
					case 'k' -> board.castling |= Board.CASTLE_BK;
					case 'q' -> board.castling |= Board.CASTLE_BQ;
				}
			}
		}

		board.ep88 = parts.length > 3 && !parts[3].equals("-") ? Board.to88(Sq.of(parts[3])) : -1;
		board.halfmove = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
		board.fullmove = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
	}

	static String write(Board board) {
		StringBuilder fen = new StringBuilder();
		for (int rank = 7; rank >= 0; rank--) {
			int empty = 0;
			for (int file = 0; file < 8; file++) {
				int piece = board.squares[rank * 16 + file];
				if (piece == 0) {
					empty++;
					continue;
				}
				if (empty > 0) {
					fen.append(empty);
					empty = 0;
				}
				char c = PIECE_CHARS.charAt(Math.abs(piece) - 1);
				fen.append(piece > 0 ? c : Character.toLowerCase(c));
			}
			if (empty > 0) fen.append(empty);
			if (rank > 0) fen.append('/');
		}

		fen.append(board.side == Board.WHITE ? " w " : " b ");
		if (board.castling == 0) {
			fen.append('-');
		} else {
			if ((board.castling & Board.CASTLE_WK) != 0) fen.append('K');
			if ((board.castling & Board.CASTLE_WQ) != 0) fen.append('Q');
			if ((board.castling & Board.CASTLE_BK) != 0) fen.append('k');
			if ((board.castling & Board.CASTLE_BQ) != 0) fen.append('q');
		}
		fen.append(' ').append(board.ep88 == -1 ? "-" : Sq.name(Board.to64(board.ep88)));
		fen.append(' ').append(board.halfmove).append(' ').append(board.fullmove);
		return fen.toString();
	}
}
