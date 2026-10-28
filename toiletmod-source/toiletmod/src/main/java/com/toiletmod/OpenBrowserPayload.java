package com.toiletmod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Сервер -> клиент: открыть экран браузера (kind: 0 = телевизор, 1 = компьютер). */
public record OpenBrowserPayload(int kind, BlockPos pos) implements CustomPayload {
	public static final CustomPayload.Id<OpenBrowserPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ToiletMod.MOD_ID, "open_browser"));

	public static final PacketCodec<RegistryByteBuf, OpenBrowserPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, OpenBrowserPayload::kind,
			BlockPos.PACKET_CODEC, OpenBrowserPayload::pos,
			OpenBrowserPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
