package com.modmenu;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static String apiKey = "";
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("modmenu_lite.properties");

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(FILE)) { p.load(in); }
            apiKey = p.getProperty("apiKey", "");
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("apiKey", apiKey);
            try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Mod Menu Lite"); }
        } catch (Exception ignored) {}
    }
}
