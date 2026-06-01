package com.nekotech.screens;

import com.nekotech.NekoTechnology;
import com.nekotech.item.custom.NekoTag.NekoTask;
import com.nekotech.network.payload.s2c.OpenTagListPayload;
import com.nekotech.util.NekoTagTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class NekoTagListScreen extends Screen {
    private static final Identifier WINDOW_TEXTURE =
            Identifier.of(NekoTechnology.MOD_ID, "textures/gui/tag/window.png");

    private static final int BG_W = 176;
    private static final int BG_H = 166;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 20;
    private static final int LIST_W = 160;
    private static final int LIST_H = 132;

    private static final int ROW_W = 152;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 2;

    private static final int ICON_SIZE = 16;

    private final BlockPos pos;
    private final List<OpenTagListPayload.TagEntry> tags;

    private double scroll;

    public NekoTagListScreen(
            BlockPos pos,
            List<OpenTagListPayload.TagEntry> tags
    ) {
        super(Text.translatable("gui.neko-technology.tag_list"));
        this.pos = pos;
        this.tags = List.copyOf(tags);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int bgX = (width - BG_W) / 2;
        int bgY = (height - BG_H) / 2;

        context.drawTexture(
                WINDOW_TEXTURE,
                bgX,
                bgY,
                0,
                0,
                BG_W,
                BG_H,
                BG_W,
                BG_H
        );

        renderTitle(context, bgX, bgY);
        renderTagList(context, bgX, bgY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTitle(DrawContext context, int bgX, int bgY) {
        context.drawText(
                textRenderer,
                title,
                bgX + 8,
                bgY + 7,
                0x404040,
                false
        );

        String posText = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

        context.drawText(
                textRenderer,
                posText,
                bgX + BG_W - 8 - textRenderer.getWidth(posText),
                bgY + 7,
                0x606060,
                false
        );
    }

    private void renderTagList(DrawContext context, int bgX, int bgY) {
        int listX = bgX + LIST_X;
        int listY = bgY + LIST_Y;

        if (tags.isEmpty()) {
            context.drawText(
                    textRenderer,
                    Text.translatable("gui.neko-technology.no_tags"),
                    listX + 8,
                    listY + 8,
                    0x808080,
                    false
            );
            return;
        }

        context.enableScissor(
                listX,
                listY,
                listX + LIST_W,
                listY + LIST_H
        );

        int rowX = listX;
        int rowY = listY - (int) scroll;

        for (OpenTagListPayload.TagEntry tag : tags) {
            if (rowY + ROW_H >= listY && rowY <= listY + LIST_H) {
                renderTagRow(context, tag, rowX, rowY);
            }

            rowY += ROW_H + ROW_GAP;
        }

        context.disableScissor();

        renderScrollbar(context, listX + LIST_W - 5, listY);
    }

    private void renderTagRow(
            DrawContext context,
            OpenTagListPayload.TagEntry tag,
            int x,
            int y
    ) {
        Identifier background = NekoTagTextures.background(tag.color());

        context.drawTexture(
                background,
                x,
                y,
                0,
                0,
                ROW_W,
                ROW_H,
                ROW_W,
                ROW_H
        );

        renderPriority(context, tag, x, y);
        renderTaskIcon(context, tag, x, y);
    }

    private void renderPriority(
            DrawContext context,
            OpenTagListPayload.TagEntry tag,
            int x,
            int y
    ) {
        String priorityText = Short.toString(tag.priority());

        context.drawText(
                textRenderer,
                priorityText,
                x + 8,
                y + 7,
                0xFFFFFF,
                true
        );
    }

    private void renderTaskIcon(
            DrawContext context,
            OpenTagListPayload.TagEntry tag,
            int x,
            int y
    ) {
        NekoTask task = NekoTask.byId(tag.task());
        Identifier taskTexture = task.texture();

        context.drawTexture(
                taskTexture,
                x + ROW_W - ICON_SIZE - 6,
                y + 3,
                0,
                0,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE
        );
    }

    private void renderScrollbar(DrawContext context, int x, int y) {
        int maxScroll = getMaxScroll();

        if (maxScroll <= 0) {
            return;
        }

        int contentHeight = getContentHeight();
        int barHeight = Math.max(20, LIST_H * LIST_H / contentHeight);
        int barY = y + (int) ((LIST_H - barHeight) * (scroll / maxScroll));

        context.fill(x, y, x + 4, y + LIST_H, 0x66000000);
        context.fill(x, barY, x + 4, barY + barHeight, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        int bgX = (width - BG_W) / 2;
        int bgY = (height - BG_H) / 2;

        int listX = bgX + LIST_X;
        int listY = bgY + LIST_Y;

        if (!isInside(mouseX, mouseY, listX, listY, LIST_W, LIST_H)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        scroll = MathHelper.clamp(
                scroll - verticalAmount * 12,
                0,
                getMaxScroll()
        );

        return true;
    }

    private boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private int getContentHeight() {
        if (tags.isEmpty()) {
            return 0;
        }

        return tags.size() * (ROW_H + ROW_GAP) - ROW_GAP;
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - LIST_H);
    }
}
