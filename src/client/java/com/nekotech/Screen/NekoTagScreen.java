package com.nekotech.Screen;

import com.nekotech.item.ModItems;
import com.nekotech.item.custom.NekoTag.NekoTagData;
import com.nekotech.network.NetworkPayloads;
import com.nekotech.screen.NekoTag.NekoTagScreenHandler;
import com.nekotech.util.NekoTask;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;

public class NekoTagScreen extends HandledScreen<NekoTagScreenHandler> {

    private static final int BG_WIDTH = 230;
    private static final int BG_HEIGHT = 224;

    private static final int COLOR_X = 10;
    private static final int COLOR_Y = 20;

    private static final int FAKE_SLOT_X = 97;
    private static final int FAKE_SLOT_Y = 45;

    private static final int PRIORITY_X = 88;
    private static final int PRIORITY_Y = 84;

    private static final int TASK_X = 160;
    private static final int TASK_Y = 20;

    private DyeColor selectedColor = DyeColor.WHITE;
    private ItemStack displayStack = ItemStack.EMPTY;
    private short priority = 0;
    private NekoTask selectedTask = NekoTask.IDLE;

    private TextFieldWidget priorityBox;

    private int scrollOffset = 0;
    private final int taskRowHeight = 22;
    private final int visibleTaskRows = 5;


    public NekoTagScreen(
            NekoTagScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title);
        this.backgroundWidth = BG_WIDTH;
        this.backgroundHeight = BG_HEIGHT;
        this.playerInventoryTitleY = -1000;
        this.titleY = -1000;
    }

    @Override
    protected void init() {
        super.init();

        loadFromHeldTag();

        int x = this.x;
        int y = this.y;

        priorityBox = new TextFieldWidget(
                this.textRenderer,
                x + PRIORITY_X,
                y + PRIORITY_Y,
                55,
                18,
                Text.translatable("screen.neko-technology.neko_tag.priority")
        );

        priorityBox.setText(Short.toString(priority));
        priorityBox.setChangedListener(this::onPriorityChanged);

        this.addDrawableChild(priorityBox);
    }

    private void loadFromHeldTag() {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        ItemStack tagStack = MinecraftClient.getInstance().player.getStackInHand(handler.getHand());

        if (!tagStack.isOf(ModItems.neko_tag)) {
            return;
        }

        selectedColor = NekoTagData.readColor(tagStack);
        priority = NekoTagData.readPriority(tagStack);
        selectedTask = NekoTagData.readTask(tagStack);
        displayStack = NekoTagData.readDisplayStack(tagStack);
    }

    private void onPriorityChanged(String value) {
        if (value.isEmpty()) {
            priority = 0;
            return;
        }

        try {
            int parsed = Integer.parseInt(value);

            if (parsed < 0) {
                priority = 0;
                priorityBox.setText("0");
            } else if (parsed > Short.MAX_VALUE) {
                priority = Short.MAX_VALUE;
                priorityBox.setText(Short.toString(Short.MAX_VALUE));
            } else {
                priority = (short) parsed;
            }
        } catch (NumberFormatException ignored) {
            priorityBox.setText(Short.toString(priority));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        context.fill(x, y, x + BG_WIDTH, y + BG_HEIGHT, 0xFF202020);
        context.drawBorder(x, y, BG_WIDTH, BG_HEIGHT, 0xFFFFFFFF);

        drawSectionBackground(context, x + 6, y + 16, 80, 82);
        drawSectionBackground(context, x + 84, y + 16, 66, 94);
        drawSectionBackground(context, x + 156, y + 16, 66, 118);
        drawSectionBackground(context, x + 31, y + 137, 169, 81);

        drawColorArea(context, x + COLOR_X, y + COLOR_Y, mouseX, mouseY);
        drawFakeSlot(context, x + FAKE_SLOT_X, y + FAKE_SLOT_Y);
        drawTaskList(context, x + TASK_X, y + TASK_Y, mouseX, mouseY);

        drawPlayerSlotBackgrounds(context);

        context.drawText(textRenderer, Text.translatable("screen.neko-technology.neko_tag.color"), x + 10, y + 8, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("screen.neko-technology.neko_tag.item"), x + 92, y + 32, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("screen.neko-technology.neko_tag.priority"), x + 88, y + 72, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("screen.neko-technology.neko_tag.task"), x + 160, y + 8, 0xFFFFFF, false);
    }

    private void drawSectionBackground(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xFF2B2B2B);
        context.drawBorder(x, y, width, height, 0xFF555555);
    }

    private void drawColorArea(DrawContext context, int startX, int startY, int mouseX, int mouseY) {
        DyeColor[] colors = DyeColor.values();

        for (int i = 0; i < colors.length; i++) {
            DyeColor color = colors[i];

            int col = i % 4;
            int row = i / 4;

            int cx = startX + col * 18;
            int cy = startY + row * 18;

            int rgb = color.getEntityColor();

            context.fill(cx, cy, cx + 14, cy + 14, 0xFF000000 | rgb);

            if (color == selectedColor) {
                context.drawBorder(cx - 2, cy - 2, 18, 18, 0xFFFFFFFF);
            } else if (mouseX >= cx && mouseX < cx + 14 && mouseY >= cy && mouseY < cy + 14) {
                context.drawBorder(cx - 1, cy - 1, 16, 16, 0xFFAAAAAA);
            }
        }
    }

    private void drawFakeSlot(DrawContext context, int slotX, int slotY) {
        drawSlotBackground(context, slotX, slotY);

        if (!displayStack.isEmpty()) {
            context.drawItem(displayStack, slotX, slotY);
            context.drawItemInSlot(textRenderer, displayStack, slotX, slotY);
        }
    }

    private void drawTaskList(DrawContext context, int startX, int startY, int mouseX, int mouseY) {
        NekoTask[] tasks = NekoTask.values();

        context.fill(
                startX - 2,
                startY - 2,
                startX + 58,
                startY + visibleTaskRows * taskRowHeight + 2,
                0xFF303030
        );

        for (int row = 0; row < visibleTaskRows; row++) {
            int taskIndex = scrollOffset + row;

            if (taskIndex >= tasks.length) {
                break;
            }

            NekoTask task = tasks[taskIndex];

            int rowY = startY + row * taskRowHeight;

            boolean hovered = mouseX >= startX
                    && mouseX < startX + 54
                    && mouseY >= rowY
                    && mouseY < rowY + taskRowHeight;

            if (task == selectedTask) {
                context.fill(startX, rowY, startX + 54, rowY + taskRowHeight - 2, 0xFF557755);
            } else if (hovered) {
                context.fill(startX, rowY, startX + 54, rowY + taskRowHeight - 2, 0xFF555555);
            }

            context.drawTexture(
                    task.texture(),
                    startX + 3,
                    rowY + 3,
                    0,
                    0,
                    16,
                    16,
                    16,
                    16
            );

            context.drawText(
                    textRenderer,
                    Text.translatable(task.translationKey()),
                    startX + 23,
                    rowY + 7,
                    0xFFFFFF,
                    false
            );
        }

        drawScrollBar(context, startX + 57, startY);
    }

    private void drawScrollBar(DrawContext context, int x, int y) {
        int total = NekoTask.values().length;
        int barHeight = visibleTaskRows * taskRowHeight;

        context.fill(x, y, x + 3, y + barHeight, 0xFF1A1A1A);

        if (total <= visibleTaskRows) {
            context.fill(x, y, x + 3, y + barHeight, 0xFFAAAAAA);
            return;
        }

        int maxOffset = total - visibleTaskRows;
        int thumbHeight = Math.max(12, barHeight * visibleTaskRows / total);
        int thumbY = y + (barHeight - thumbHeight) * scrollOffset / maxOffset;

        context.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    private void drawInventorySlotBackgrounds(DrawContext context, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBackground(
                        context,
                        startX + column * 18,
                        startY + row * 18
                );
            }
        }
    }

    private void drawHotbarSlotBackgrounds(DrawContext context, int startX, int startY) {
        for (int column = 0; column < 9; column++) {
            drawSlotBackground(context, startX + column * 18, startY);
        }
    }

    private void drawSlotBackground(DrawContext context, int slotX, int slotY) {
        context.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF8B8B8B);

        // 内部：16x16，和物品/hover 区域一致
        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF373737);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.x;
        int y = this.y;

        if (clickColor(mouseX, mouseY, x + COLOR_X, y + COLOR_Y)) {
            return true;
        }

        if (clickFakeSlot(mouseX, mouseY, x + FAKE_SLOT_X, y + FAKE_SLOT_Y)) {
            return true;
        }

        if (clickTask(mouseX, mouseY, x + TASK_X, y + TASK_Y)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickColor(double mouseX, double mouseY, int startX, int startY) {
        DyeColor[] colors = DyeColor.values();

        for (int i = 0; i < colors.length; i++) {
            int col = i % 4;
            int row = i / 4;

            int cx = startX + col * 18;
            int cy = startY + row * 18;

            if (mouseX >= cx && mouseX < cx + 14 && mouseY >= cy && mouseY < cy + 14) {
                selectedColor = colors[i];
                sendSavePacket();
                return true;
            }
        }

        return false;
    }

    private boolean clickFakeSlot(double mouseX, double mouseY, int slotX, int slotY) {
        if (mouseX < slotX || mouseX >= slotX + 18 || mouseY < slotY || mouseY >= slotY + 18) {
            return false;
        }

        ItemStack cursor = this.handler.getCursorStack();

        if (!cursor.isEmpty()) {
            displayStack = cursor.copy();
            displayStack.setCount(1);
        } else {
            displayStack = ItemStack.EMPTY;
        }
        sendSavePacket();

        return true;
    }

    private boolean clickTask(double mouseX, double mouseY, int startX, int startY) {
        if (mouseX < startX || mouseX >= startX + 58) {
            return false;
        }

        if (mouseY < startY || mouseY >= startY + visibleTaskRows * taskRowHeight) {
            return false;
        }

        int row = ((int) mouseY - startY) / taskRowHeight;
        int index = scrollOffset + row;

        NekoTask[] tasks = NekoTask.values();

        if (index >= 0 && index < tasks.length) {
            selectedTask = tasks[index];
            sendSavePacket();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        int x = this.x + TASK_X;
        int y = this.y + TASK_Y;

        boolean insideTaskList = mouseX >= x
                && mouseX < x + 62
                && mouseY >= y
                && mouseY < y + visibleTaskRows * taskRowHeight;

        if (!insideTaskList) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int maxOffset = Math.max(0, NekoTask.values().length - visibleTaskRows);

        if (verticalAmount < 0) {
            scrollOffset = Math.min(maxOffset, scrollOffset + 1);
            return true;
        }

        if (verticalAmount > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        sendSavePacket();
        super.close();
    }

    private void sendSavePacket() {
        String displayItemId = "";

        if (!displayStack.isEmpty()) {
            displayItemId = Registries.ITEM.getId(displayStack.getItem()).toString();
        }

        ClientPlayNetworking.send(new NetworkPayloads.NekoTagUpdatePayload(
                selectedColor.getName(),
                priority,
                selectedTask.id(),
                displayItemId,
                handler.getHand().name()
        ));
    }
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 不绘制默认 title 和 player inventory title
    }

    private void drawPlayerSlotBackgrounds(DrawContext context) {
        int screenX = this.x;
        int screenY = this.y;

        int totalSlots = this.handler.slots.size();
        int playerSlotStart = Math.max(0, totalSlots - 36);

        for (int i = playerSlotStart; i < totalSlots; i++) {
            var slot = this.handler.slots.get(i);
            drawSlotBackground(context, screenX + slot.x, screenY + slot.y);
        }
    }
}