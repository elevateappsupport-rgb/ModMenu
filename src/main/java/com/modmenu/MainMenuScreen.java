package com.modmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MainMenuScreen extends Screen {
    public MainMenuScreen() { super(Text.literal("Mod Menu")); }

    private static Text freecamLabel() {
        return Text.literal("Freecam: " + (Freecam.active ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 2 - 40;
        addDrawableChild(ButtonWidget.builder(Text.literal("Chest ESP"),
                b -> client.setScreen(new EspScreen(this))).dimensions(x, y, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(freecamLabel(), b -> {
            Freecam.toggle(client);
            b.setMessage(freecamLabel());
        }).dimensions(x, y + 26, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("DonutSMP Auction Prices"),
                b -> client.setScreen(new PriceScreen(this))).dimensions(x, y + 52, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"),
                b -> close()).dimensions(x, y + 84, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 70, 0xFFFFFFFF);
    }
}
