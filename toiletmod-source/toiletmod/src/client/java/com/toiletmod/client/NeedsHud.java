package com.toiletmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/** Две полоски в левом верхнем углу: «Пописать» и «Покакать». */
public class NeedsHud {
	private static final int X = 6;
	private static final int Y = 6;
	private static final int W = 80;
	private static final int H = 7;

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden) return;

		drawBar(ctx, mc, Text.translatable("hud.toiletmod.pee"), ClientNeeds.pee, X, Y);
		drawBar(ctx, mc, Text.translatable("hud.toiletmod.poop"), ClientNeeds.poop, X, Y + 24);
	}

	private static void drawBar(DrawContext ctx, MinecraftClient mc, Text label, float value, int x, int y) {
		value = Math.max(0f, Math.min(100f, value));
		ctx.drawText(mc.textRenderer, label, x, y, 0xFFFFFF, true);
		ctx.drawText(mc.textRenderer, Text.literal((int) value + "%"), x + W - 22, y, 0xDDDDDD, true);

		int top = y + 11;
		ctx.fill(x - 1, top - 1, x + W + 1, top + H + 1, 0xAA000000);
		int filled = (int) (W * value / 100f);
		int color = value < 50f ? 0xFF55FF55 : value < 80f ? 0xFFFFFF55 : 0xFFFF5555;
		if (filled > 0) {
			ctx.fill(x, top, x + filled, top + H, color);
		}
	}
}
