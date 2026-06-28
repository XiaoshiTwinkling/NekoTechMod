package com.nekotech.screens.modmenu;

import com.nekotech.NekoTechnology;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ModMainScreen extends Screen {

    private static final Identifier ModTitle = Identifier.of(NekoTechnology.MOD_ID, "textures/gui/mod_title.png");

    public ButtonWidget Settings;
    public ButtonWidget About;

    public ModMainScreen() {
        super(Text.literal("NekoTech"));
    }

    public ModMainScreen(Screen screen) {
        super(Text.literal("NekoTech"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTexture(ModTitle, width / 2 - 150, - 40, 300, 225, 0, 0, 1600, 1200, 1600, 1200);

        Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(NekoTechnology.MOD_ID);
        ModContainer container = containerOpt.get();
        ModMetadata metadata = container.getMetadata();

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(NekoTechnology.MOD_ID + " " + metadata.getVersion().toString()), 5, height - 10, 0xffffff);

    }

    @Override
    protected void init(){
        Settings = ButtonWidget.builder(Text.translatable("gui.neko-technology.settings"), button -> {
                    client.setScreen(new ModSettingScreen());
                })
                .dimensions(width / 2 - 150, height / 2 + 20, 300, 20)
                .build();


        About = ButtonWidget.builder(Text.translatable("gui.neko-technology.about"), button -> {
                    client.setScreen(new ModAboutScreen());
                })
                .dimensions(width / 2 - 150, height / 2 + 50, 300, 20)
                .build();

        addDrawableChild(Settings);
        addDrawableChild(About);

    }
}
