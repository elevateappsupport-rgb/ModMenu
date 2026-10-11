package com.modmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class EspScreen extends Screen {
    private final Screen parent;

    public EspScreen(Screen parent) {
        super(Text.literal("Chest ESP"));
        this.parent = parent;
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    private void toggle(int x, int y, String name, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(label(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(label(name, get.getAsBoolean()));
        }).dimensions(x, y, 200, 20).build());
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 2 - 60;
        toggle(x, y, "Chest ESP", () -> ChestEsp.enabled, v -> ChestEsp.enabled = v);
        toggle(x, y + 30, "Chests", () -> ChestEsp.chests, v -> ChestEsp.chests = v);
        toggle(x, y + 54, "Ender chests", () -> ChestEsp.enderChests, v -> ChestEsp.enderChests = v);
        toggle(x, y + 78, "Shulker boxes", () -> ChestEsp.shulkers, v -> ChestEsp.shulkers = v);
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                b -> client.setScreen(parent)).dimensions(x, y + 112, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 90, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "Singleplayer only", width / 2, height / 2 - 78, 0xFFAAAAAA);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
