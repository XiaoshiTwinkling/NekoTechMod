package com.nekotech.screens;

import com.nekotech.item.custom.NekoTag.NekoTask;
import com.nekotech.network.payload.s2c.OpenTagListPayload;
import com.nekotech.util.NekoTagTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class NekoTagListScreen extends Screen {

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
        int bgX = (width - BG_W) / 2;
        int bgY = (height - BG_H) / 2;

        context.fill(0, 0, width, height, 0x80000000);

        renderVanillaPanel(context, bgX, bgY);

        renderTitle(context, bgX, bgY);
        renderTagList(context, bgX, bgY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderVanillaPanel(DrawContext context, int x, int y) {
        context.fill(x, y, x + BG_W, y + BG_H, 0xFFC6C6C6);
        context.fill(x + 1, y + 1, x + BG_W - 1, y + BG_H - 1, 0xFF8B8B8B);
        context.fill(x + 2, y + 2, x + BG_W - 2, y + BG_H - 2, 0xFFC6C6C6);
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

        int rowY = listY - (int) scroll;

        for (OpenTagListPayload.TagEntry tag : tags) {
            if (rowY + ROW_H >= listY && rowY <= listY + LIST_H) {
                renderTagRow(context, tag, listX, rowY);
            }

            rowY += ROW_H + ROW_GAP;
        }

        context.disableScissor();

        renderScrollbar(context, listX + LIST_W - 5, listY);
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
                x + 25,
                y + 7,
                0xFFFFFF,
                true
        );
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

        renderDisplayStackIcon(context, tag, x, y);
        renderPriority(context, tag, x, y);
        renderTaskIcon(context, tag, x, y);
    }

    private void renderDisplayStackIcon(
            DrawContext context,
            OpenTagListPayload.TagEntry tag,
            int x,
            int y
    ) {
        ItemStack stack = getDisplayStack(tag.displayStackId());

        if (stack.isEmpty()) {
            return;
        }

        context.drawItem(
                stack,
                x + 5,
                y + 3
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

        if (!isInside(mouseX, mouseY, listX, listY)) {
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
            int y
    ) {
        return mouseX >= x
                && mouseX < x + NekoTagListScreen.LIST_W
                && mouseY >= y
                && mouseY < y + NekoTagListScreen.LIST_H;
    }

    private ItemStack getDisplayStack(String displayStackId) {
        if (displayStackId == null || displayStackId.isBlank()) {
            return ItemStack.EMPTY;
        }

        Identifier id = Identifier.tryParse(displayStackId);

        if (id == null) {
            return ItemStack.EMPTY;
        }

        return Registries.ITEM.getOrEmpty(id)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
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

    @Override
    public void blur() {
        // Disable vanilla menu background blur for this screen.
    }

    @Override
    protected void applyBlur(float delta) {
        // Do nothing.
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do nothing.
        // 不调用 super.renderBackground，避免原版背景、暗化、blur。
    }
}
