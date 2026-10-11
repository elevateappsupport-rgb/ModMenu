package com.modmenu;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chest ESP drawn as a 2D overlay (projected onto the screen), so it is visible through all blocks
 * at any depth. Works in singleplayer only.
 */
public class ChestEsp {
    public static boolean enabled = false;
    public static boolean chests = true;
    public static boolean enderChests = false;
    public static boolean shulkers = false;

    private static final int ORANGE = 0xFFFFC040; // light yellow-orange
    private static final int RED = 0xFFFF2020;

    private static volatile List<Vec3d> targets = List.of();

    public static void tick(MinecraftClient mc) {
        if (!enabled) { targets = List.of(); return; }
        if (mc.world == null || mc.player == null) { targets = List.of(); return; }
        if (!mc.isInSingleplayer()) { enabled = false; targets = List.of(); return; }
        if (mc.world.getTime() % 10 != 0) return; // rescan twice per second

        int vd = 32;
        ChunkPos pc = mc.player.getChunkPos();
        List<Vec3d> list = new ArrayList<>();
        for (int dx = -vd; dx <= vd; dx++) {
            for (int dz = -vd; dz <= vd; dz++) {
                Chunk c = mc.world.getChunk(pc.x + dx, pc.z + dz, ChunkStatus.FULL, false);
                if (c == null) continue;
                for (BlockPos bp : c.getBlockEntityPositions()) {
                    BlockEntity be = c.getBlockEntity(bp);
                    if (be == null) continue;
                    boolean hit = (chests && be instanceof ChestBlockEntity)
                            || (enderChests && be instanceof EnderChestBlockEntity)
                            || (shulkers && be instanceof ShulkerBoxBlockEntity);
                    if (hit) list.add(Vec3d.ofCenter(be.getPos()));
                }
            }
        }
        targets = list;
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.world == null || mc.player == null || mc.options.hudHidden) return;

        Entity ce = mc.getCameraEntity();
        if (ce == null) ce = mc.player;
        float td = tc.getTickProgress(false);
        Vec3d cp = ce.getLerpedPos(td).add(0, ce.getEyeHeight(ce.getPose()), 0);
        double yaw = Math.toRadians(ce.getYaw(td));
        double pitch = Math.toRadians(ce.getPitch(td));

        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        double cx = W / 2.0, cy = H / 2.0;
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) W / H;

        List<Vec3d> sorted = new ArrayList<>(targets);
        sorted.sort(Comparator.comparingDouble(v -> v.squaredDistanceTo(cp)));
        int shown = 0;
        for (Vec3d t : sorted) {
            if (shown++ >= 150) break;
            double dx = t.x - cp.x, dy = t.y - cp.y, dz = t.z - cp.z;
            double xc = dx * rx + dy * ry + dz * rz;
            double yc = dx * ux + dy * uy + dz * uz;
            double zc = dx * fx + dy * fy + dz * fz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (zc <= 0.05) {
                // Behind us: line towards the screen edge so you know which way to turn.
                double len = Math.max(1e-6, Math.hypot(xc, yc));
                int ex = (int) (cx + xc / len * cx * 0.9), ey = (int) (cy - yc / len * cy * 0.9);
                line(ctx, (int) cx, (int) cy, ex, ey, RED, W, H);
                ctx.drawTextWithShadow(mc.textRenderer, (int) dist + "m (behind)", clamp(ex, 2, W - 60), clamp(ey, 2, H - 10), ORANGE);
                continue;
            }

            double sx = cx + (xc / (zc * tanHalf * aspect)) * cx;
            double sy = cy - (yc / (zc * tanHalf)) * cy;
            sx = Math.max(-5000, Math.min(5000, sx));
            sy = Math.max(-5000, Math.min(5000, sy));
            int ix = (int) sx, iy = (int) sy;

            line(ctx, (int) cx, (int) cy, ix, iy, RED, W, H);

            int size = (int) Math.max(6, Math.min(120, (H / 2.0) / (tanHalf * zc)));
            int x1 = ix - size / 2, y1 = iy - size / 2;
            ctx.fill(x1, y1, x1 + size, y1 + size, 0x40FFC040);
            border(ctx, x1, y1, size, size, ORANGE);
            if (ix >= 0 && ix < W && iy >= 0 && iy < H) {
                ctx.drawTextWithShadow(mc.textRenderer, (int) dist + "m", x1, y1 - 10, ORANGE);
            }
        }
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static void line(DrawContext ctx, int x0, int y0, int x1, int y1, int color, int W, int H) {
        int dx = x1 - x0, dy = y1 - y0;
        int n = Math.min(1500, Math.max(Math.abs(dx), Math.abs(dy)) / 2);
        int d = Math.max(n, 1);
        for (int i = 0; i <= n; i++) {
            int x = x0 + (int) ((long) dx * i / d);
            int y = y0 + (int) ((long) dy * i / d);
            if (x < 0 || y < 0 || x >= W || y >= H) continue;
            ctx.fill(x, y, x + 2, y + 2, color);
        }
    }
}
