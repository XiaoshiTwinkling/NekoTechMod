package com.nekotech.screens.modmenu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ModAboutScreen extends Screen {
    private final Screen parent;

    public ModAboutScreen() {
        super(Text.literal("About NekoTech"));
        this.parent = new ModMainScreen();
    }

    @Override
    public void init(){

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("gui.neko-technology.about"), width / 2, 10, 0xffffff);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
