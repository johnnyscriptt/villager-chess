package com.foldpaper.villagerchess.chess;

/** Square helpers for the 0-63 board index convention: a1 = 0, h1 = 7, a8 = 56, h8 = 63. */
public final class Sq {
	private Sq() {}

	public static int of(String name) {
		int file = name.charAt(0) - 'a';
		int rank = name.charAt(1) - '1';
		if (file < 0 || file > 7 || rank < 0 || rank > 7) throw new IllegalArgumentException(name);
		return rank * 8 + file;
	}

	public static String name(int sq) {
		return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq));
	}

	public static int file(int sq) {
		return sq & 7;
	}

	public static int rank(int sq) {
		return sq >> 3;
	}
}
