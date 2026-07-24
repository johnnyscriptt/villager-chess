package com.foldpaper.villagerchess.chess.engine;

/**
 * Engine strength knobs for one villager trading level.
 *
 * @param maxDepth      iterative-deepening ceiling
 * @param timeBudgetMs  soft time limit for the whole search
 * @param quiescence    search captures at the horizon instead of raw eval
 * @param blunderChance probability of picking semi-randomly among near-best root moves
 * @param evalNoiseCp   random centipawn noise added to root scores before picking
 */
public record DifficultyProfile(int maxDepth, long timeBudgetMs, boolean quiescence,
		double blunderChance, int evalNoiseCp) {

	/** Villager trading levels: 1 Novice, 2 Apprentice, 3 Journeyman, 4 Expert, 5 Master. */
	public static DifficultyProfile forVillagerLevel(int level) {
		return switch (Math.clamp(level, 1, 5)) {
			case 1 -> new DifficultyProfile(2, 200, false, 0.35, 150);
			case 2 -> new DifficultyProfile(3, 400, false, 0.20, 80);
			case 3 -> new DifficultyProfile(4, 800, true, 0.08, 40);
			case 4 -> new DifficultyProfile(6, 1500, true, 0.02, 15);
			default -> new DifficultyProfile(8, 2500, true, 0.0, 0);
		};
	}
}
