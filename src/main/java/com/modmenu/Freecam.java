package com.modmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.lwjgl.glfw.GLFW;

/**
 * Freecam: a invisible client-side camera entity flies around while the real player stays where it is.
 * The mouse still turns the player's view, WASD / Space / Shift move the camera. Singleplayer only.
 */
public class Freecam {
    public static boolean active = false;
    private static ArmorStandEntity cam;
    private static double x, y, z;

    public static void toggle(MinecraftClient mc) {
        if (active) stop(mc); else start(mc);
    }

    private static void start(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || !mc.isInSingleplayer()) return;
        cam = new ArmorStandEntity(mc.world, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        cam.setInvisible(true);
        cam.setNoGravity(true);
        x = mc.player.getX(); y = mc.player.getY(); z = mc.player.getZ();
        cam.refreshPositionAndAngles(x, y, z, mc.player.getYaw(), mc.player.getPitch());
        mc.setCameraEntity(cam);
        active = true;
    }

    public static void stop(MinecraftClient mc) {
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
        active = false;
    }

    private static boolean down(long handle, int key) {
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }

    public static void startTick(MinecraftClient mc) {
        if (!active) return;
        if (mc.player == null || mc.world == null || !mc.isInSingleplayer() || cam == null) {
            active = false; cam = null; return;
        }
        double f = 0, s = 0, v = 0;
        if (mc.currentScreen == null) {
            long h = mc.getWindow().getHandle();
            if (down(h, GLFW.GLFW_KEY_W)) f += 1;
            if (down(h, GLFW.GLFW_KEY_S)) f -= 1;
            if (down(h, GLFW.GLFW_KEY_A)) s += 1; // left
            if (down(h, GLFW.GLFW_KEY_D)) s -= 1; // right
            if (down(h, GLFW.GLFW_KEY_SPACE)) v += 1;
            if (down(h, GLFW.GLFW_KEY_LEFT_SHIFT)) v -= 1;
        }
        float yawDeg = mc.player.getYaw(), pitchDeg = mc.player.getPitch();
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double lx = Math.cos(yaw), lz = Math.sin(yaw); // left vector
        double speed = 0.8;
        x += (fx * f + lx * s) * speed;
        y += (fy * f + v) * speed;
        z += (fz * f + lz * s) * speed;
        cam.refreshPositionAndAngles(x, y, z, yawDeg, pitchDeg);

        // Stop the real player from reacting to the keys.
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
    }
}
