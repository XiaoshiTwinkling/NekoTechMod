package com.nekotech.screens;

import com.nekotech.network.payload.c2s.CreateCatCameraChannelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.UUID;

public class CatCameraNameScreen extends Screen {
    private final UUID catUuid;
    private TextFieldWidget nameField;
    private Text error = Text.empty();

    public CatCameraNameScreen(UUID catUuid) {
        super(Text.translatable("gui.neko-technology.cat_camera.name.title"));
        this.catUuid = catUuid;
    }

    @Override
    protected void init() {
        int center = width / 2;
        nameField = new TextFieldWidget(textRenderer, center - 100, height / 2 - 15, 200, 20,
                Text.translatable("gui.neko-technology.cat_camera.name.field"));
        nameField.setMaxLength(32);
        addDrawableChild(nameField);
        setInitialFocus(nameField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.neko-technology.cat_camera.create"), button -> submit())
                .dimensions(center - 100, height / 2 + 15, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(center + 4, height / 2 + 15, 96, 20).build());
    }

    private void submit() {
        ClientPlayNetworking.send(new CreateCatCameraChannelPayload(catUuid, nameField.getText()));
    }

    public void applyResult(boolean success, boolean close, String messageKey) {
        error = Text.translatable(messageKey);
        if (success && close) close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { submit(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 45, 0xFFFFFF);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height / 2 + 43, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

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
