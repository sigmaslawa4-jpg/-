package com.toiletmod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Пакет сервер -> клиент: текущие значения потребностей (0..100). */
public record NeedsPayload(float pee, float poop) implements CustomPayload {
	public static final CustomPayload.Id<NeedsPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ToiletMod.MOD_ID, "needs"));

	public static final PacketCodec<RegistryByteBuf, NeedsPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT, NeedsPayload::pee,
			PacketCodecs.FLOAT, NeedsPayload::poop,
			NeedsPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
