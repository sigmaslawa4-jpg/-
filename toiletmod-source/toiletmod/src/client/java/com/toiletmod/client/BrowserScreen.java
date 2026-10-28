package com.toiletmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.toiletmod.OpenBrowserPayload;
import com.toiletmod.block.ScreenBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Экран с настоящим Chromium-браузером (через MCEF) для телевизора и компьютера. */
public class BrowserScreen extends Screen {
	private static final int BAR_H = 26;

	private final int kind;
	private final BlockPos devicePos;
	private final boolean tv;

	private Object browser; // MCEFBrowser (через рефлексию)
	private TextFieldWidget urlField;
	private String urlText = "";

	public BrowserScreen(int kind, BlockPos pos) {
		super(Text.translatable("screen.toiletmod.browser"));
		this.kind = kind;
		this.devicePos = pos;
		this.tv = kind == ScreenBlock.KIND_TV;
	}

	/** Вызывается при получении пакета от сервера. */
	public static void open(OpenBrowserPayload payload) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!McefBridge.available()) {
			if (mc.player != null) {
				mc.player.sendMessage(Text.translatable("message.toiletmod.mcef_missing"), false);
			}
			return;
		}
		if (!McefBridge.initialized()) {
			if (mc.player != null) {
				mc.player.sendMessage(Text.translatable("message.toiletmod.mcef_not_ready"), false);
			}
			return;
		}
		mc.setScreen(new BrowserScreen(payload.kind(), payload.pos()));
	}

	private Object b(String name, Object... args) {
		return browser == null ? null : McefBridge.call(browser, name, args);
	}

	private String homeUrl() {
		return tv ? "https://www.youtube.com" : "https://www.google.com";
	}

	@Override
	protected void init() {
		if (browser == null) {
			browser = McefBridge.createBrowser(homeUrl(), false);
			urlText = homeUrl();
			if (browser == null) {
				if (client != null && client.player != null) {
					client.player.sendMessage(Text.translatable("message.toiletmod.mcef_not_ready"), false);
				}
				close();
				return;
			}
		}

		int x = 4;
		addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> b("goBack"))
				.dimensions(x, 3, 20, 20).build());
		x += 22;
		addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> b("goForward"))
				.dimensions(x, 3, 20, 20).build());
		x += 22;
		addDrawableChild(ButtonWidget.builder(Text.literal("R"), btn -> b("reload"))
				.dimensions(x, 3, 20, 20).build());
		x += 22;

		if (tv) {
			x = addQuick(x, Text.literal("YouTube"), 56, "https://www.youtube.com");
		} else {
			x = addQuick(x, Text.literal("Google"), 50, "https://www.google.com");
			x = addQuick(x, Text.translatable("screen.toiletmod.ai"), 36, "https://gemini.google.com");
			x = addQuick(x, Text.translatable("screen.toiletmod.games"), 50, "https://www.crazygames.com");
		}

		int goW = 30;
		urlField = new TextFieldWidget(textRenderer, x + 2, 3, Math.max(40, width - x - goW - 10), 20,
				Text.translatable("screen.toiletmod.url"));
		urlField.setMaxLength(2048);
		urlField.setText(urlText);
		addDrawableChild(urlField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Go"), btn -> navigate())
				.dimensions(width - goW - 4, 3, goW, 20).build());

		resizeBrowser();
	}

	private int addQuick(int x, Text label, int w, String url) {
		addDrawableChild(ButtonWidget.builder(label, btn -> b("loadURL", url)).dimensions(x, 3, w, 20).build());
		return x + w + 2;
	}

	private void navigate() {
		String target = normalize(urlField.getText());
		if (target != null) {
			b("loadURL", target);
			urlText = target;
			urlField.setFocused(false);
		}
	}

	private String normalize(String input) {
		String s = input.trim();
		if (s.isEmpty()) return null;
		if (s.startsWith("http://") || s.startsWith("https://")) return s;
		if (!s.contains(" ") && s.contains(".")) return "https://" + s;
		String q = URLEncoder.encode(s, StandardCharsets.UTF_8);
		return tv ? "https://www.youtube.com/results?search_query=" + q
				: "https://www.google.com/search?q=" + q;
	}

	private double scale() {
		return client.getWindow().getScaleFactor();
	}

	private int bx(double x) {
		return (int) (x * scale());
	}

	private int by(double y) {
		return (int) ((y - BAR_H) * scale());
	}

	private void resizeBrowser() {
		if (browser != null && width > 100 && height > BAR_H + 50) {
			b("resize", (int) (width * scale()), (int) ((height - BAR_H) * scale()));
		}
	}

	@Override
	public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
		urlText = urlField != null ? urlField.getText() : urlText;
		super.resize(client, width, height);
		resizeBrowser();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (client == null || client.world == null || client.player == null) return;

		// Закрыть, если устройство выключилось (нет интернета), сломано или игрок ушёл далеко.
		BlockState state = client.world.getBlockState(devicePos);
		boolean alive = state.getBlock() instanceof ScreenBlock && state.get(ScreenBlock.ON);
		boolean near = client.player.squaredDistanceTo(Vec3d.ofCenter(devicePos)) < 100.0;
		if (!alive || !near) {
			close();
			return;
		}

		// Показывать актуальный адрес страницы, пока игрок не печатает.
		if (urlField != null && !urlField.isFocused() && browser != null) {
			Object cur = b("getURL");
			String current = cur instanceof String str ? str : null;
			if (current != null && !current.equals(urlField.getText())) {
				urlField.setText(current);
				urlText = current;
			}
		}
	}

	@Override
	public void removed() {
		if (browser != null) {
			b("close");
			browser = null;
		}
		super.removed();
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xFF101010);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		if (browser == null) return;

		RenderSystem.disableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
		RenderSystem.setShaderTexture(0, McefBridge.textureId(browser));
		BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		buffer.vertex(0, height, 0).texture(0.0f, 1.0f).color(255, 255, 255, 255);
		buffer.vertex(width, height, 0).texture(1.0f, 1.0f).color(255, 255, 255, 255);
		buffer.vertex(width, BAR_H, 0).texture(1.0f, 0.0f).color(255, 255, 255, 255);
		buffer.vertex(0, BAR_H, 0).texture(0.0f, 0.0f).color(255, 255, 255, 255);
		BufferRenderer.drawWithGlobalProgram(buffer.end());
		RenderSystem.setShaderTexture(0, 0);
		RenderSystem.enableDepthTest();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseY < BAR_H) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		urlField.setFocused(false);
		b("sendMousePress", bx(mouseX), by(mouseY), button);
		b("setFocus", true);
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (browser != null) {
			b("sendMouseRelease", bx(mouseX), by(mouseY), button);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (browser != null && mouseY >= BAR_H) {
			b("sendMouseMove", bx(mouseX), by(mouseY));
		}
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (browser != null && mouseY >= BAR_H) {
			b("sendMouseWheel", bx(mouseX), by(mouseY), vertical, 0);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		if (urlField.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				navigate();
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		b("sendKeyPress", keyCode, scanCode, modifiers);
		b("setFocus", true);
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (!urlField.isFocused() && browser != null) {
			b("sendKeyRelease", keyCode, scanCode, modifiers);
			b("setFocus", true);
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (chr == (char) 0) return false;
		if (urlField.isFocused()) {
			return super.charTyped(chr, modifiers);
		}
		b("sendKeyTyped", chr, modifiers);
		b("setFocus", true);
		return true;
	}
}
