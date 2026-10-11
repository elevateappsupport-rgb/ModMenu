package com.modmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class PriceScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget keyField, itemField;

    public PriceScreen(Screen parent) {
        super(Text.literal("DonutSMP Auction Prices"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 150, y = 40;
        keyField = new TextFieldWidget(textRenderer, x, y, 300, 20, Text.literal("API key"));
        keyField.setMaxLength(200);
        keyField.setText(Config.apiKey);
        keyField.setPlaceholder(Text.literal("API key (get it with /api on DonutSMP)"));
        addDrawableChild(keyField);

        itemField = new TextFieldWidget(textRenderer, x, y + 26, 220, 20, Text.literal("Item"));
        itemField.setMaxLength(100);
        itemField.setPlaceholder(Text.literal("Item, e.g. diamond pickaxe"));
        addDrawableChild(itemField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), b -> {
            Config.apiKey = keyField.getText().trim();
            Config.save();
            PriceChecker.search(Config.apiKey, itemField.getText());
        }).dimensions(x + 226, y + 26, 74, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                b -> client.setScreen(parent)).dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFFFF);
        List<String> lines = PriceChecker.lines;
        int y = 100;
        for (String l : lines) {
            ctx.drawTextWithShadow(textRenderer, l, width / 2 - 150, y, 0xFFFFFFFF);
            y += 12;
        }
    }

    @Override
    public void close() { client.setScreen(parent); }
}
