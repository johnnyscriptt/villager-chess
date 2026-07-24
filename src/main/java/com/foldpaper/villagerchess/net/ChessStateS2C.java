package com.foldpaper.villagerchess.net;

import com.foldpaper.villagerchess.VillagerChess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Authoritative board state after any move or game end. lastFrom/lastTo are -1 when absent. */
public record ChessStateS2C(int gameId, String fen, int lastFrom, int lastTo, int outcomeId,
		boolean playerToMove) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ChessStateS2C> TYPE =
			new CustomPacketPayload.Type<>(VillagerChess.id("state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChessStateS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChessStateS2C::gameId,
			ByteBufCodecs.STRING_UTF8, ChessStateS2C::fen,
			ByteBufCodecs.VAR_INT, ChessStateS2C::lastFrom,
			ByteBufCodecs.VAR_INT, ChessStateS2C::lastTo,
			ByteBufCodecs.VAR_INT, ChessStateS2C::outcomeId,
			ByteBufCodecs.BOOL, ChessStateS2C::playerToMove,
			ChessStateS2C::new);

	public ChessOutcome outcome() {
		return ChessOutcome.byId(outcomeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
