package com.nekotech.screens;

import com.nekotech.network.payload.c2s.EnterCatCameraChannelPayload;
import com.nekotech.network.payload.s2c.OpenCatCameraListPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class CatCameraChannelListScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 8;
    private final List<OpenCatCameraListPayload.Entry> channels;
    private int firstRow;
    private Text error = Text.empty();

    public CatCameraChannelListScreen(List<OpenCatCameraListPayload.Entry> channels) {
        super(Text.translatable("gui.neko-technology.cat_camera.list.title"));
        this.channels = List.copyOf(channels);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int left = (width - WIDTH) / 2;
        int top = (height - VISIBLE_ROWS * ROW_HEIGHT) / 2;
        context.fill(left - 6, top - 28, left + WIDTH + 6, top + VISIBLE_ROWS * ROW_HEIGHT + 18, 0xDD202020);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 20, 0xFFFFFF);
        if (channels.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.neko-technology.cat_camera.list.empty"),
                    width / 2, top + 12, 0xAAAAAA);
        }
        int end = Math.min(channels.size(), firstRow + VISIBLE_ROWS);
        for (int i = firstRow; i < end; i++) {
            int y = top + (i - firstRow) * ROW_HEIGHT;
            boolean hover = mouseX >= left && mouseX < left + WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            context.fill(left, y, left + WIDTH, y + ROW_HEIGHT - 2, hover ? 0xFF5A5A5A : 0xFF383838);
            OpenCatCameraListPayload.Entry entry = channels.get(i);
            context.drawTextWithShadow(textRenderer, entry.name(), left + 6, y + 4, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, entry.dimension(), left + 6, y + 14, 0x999999);
        }
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2,
                    top + VISIBLE_ROWS * ROW_HEIGHT + 6, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - WIDTH) / 2;
        int top = (height - VISIBLE_ROWS * ROW_HEIGHT) / 2;
        if (button == 0 && mouseX >= left && mouseX < left + WIDTH && mouseY >= top
                && mouseY < top + VISIBLE_ROWS * ROW_HEIGHT) {
            int index = firstRow + ((int) mouseY - top) / ROW_HEIGHT;
            if (index >= 0 && index < channels.size()) {
                ClientPlayNetworking.send(new EnterCatCameraChannelPayload(channels.get(index).catUuid()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        firstRow = MathHelper.clamp(firstRow - (int) Math.signum(verticalAmount), 0,
                Math.max(0, channels.size() - VISIBLE_ROWS));
        return true;
    }

    public void setError(String messageKey) { error = Text.translatable(messageKey); }
    @Override public boolean shouldPause() { return false; }

    @Override
    public void blur() {
    }

    @Override
    protected void applyBlur(float delta) {
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x80000000);
    }
}
