package dev.warax.visuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.module.Theme;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

/** Отрисовка в мире: круги прыжка, след, China Hat, обводка блока. Всё — с тестом глубины (не видно сквозь стены). */
public final class Render3D {
    private static final class Circle {
        final Vec3d pos;
        final long time;

        Circle(Vec3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }

    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static final ArrayDeque<Vec3d> TRAIL = new ArrayDeque<>();

    private Render3D() {
    }

    public static void addCircle(Vec3d p) {
        CIRCLES.add(new Circle(p, System.currentTimeMillis()));
        if (CIRCLES.size() > 10) {
            CIRCLES.remove(0);
        }
    }

    public static void addTrail(Vec3d p, int max) {
        Vec3d last = TRAIL.peekLast();
        if (last != null && last.squaredDistanceTo(p) > 100) {
            TRAIL.clear(); // телепорт — начинаем след заново
        }
        if (last == null || last.squaredDistanceTo(p) > 0.0004) {
            TRAIL.addLast(p);
        } else if (!TRAIL.isEmpty()) {
            TRAIL.pollFirst(); // стоим — след плавно исчезает
        }
        while (TRAIL.size() > max) {
            TRAIL.pollFirst();
        }
    }

    public static void clearTrail() {
        TRAIL.clear();
    }

    public static void render(MatrixStack ms, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        boolean any = ModuleManager.JUMPCIRCLES.isActive() || ModuleManager.TRAILS.isActive()
                || ModuleManager.CHINAHAT.isActive() || ModuleManager.BLOCKOUTLINE.isActive()
                || Particles3D.hasAny();
        if (!any) {
            return;
        }
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Matrix4f m = ms.peek().getModel();

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableLighting();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        try {
            if (ModuleManager.JUMPCIRCLES.isActive()) {
                circles(m, cam);
            }
            if (ModuleManager.TRAILS.isActive()) {
                trail(m, cam, td, mc.player);
            }
            if (ModuleManager.CHINAHAT.isActive() && !mc.options.getPerspective().isFirstPerson()) {
                hat(m, cam, td, mc.player);
            }
            if (ModuleManager.BLOCKOUTLINE.isActive() && !mc.options.hudHidden) {
                outline(mc, m, cam);
            }
            Particles3D.render(m, cam, mc.gameRenderer.getCamera());
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(1.0f);
            RenderSystem.depthMask(true);
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableTexture();
        }
    }

    // ---------- эффекты ----------

    private static void circles(Matrix4f m, Vec3d cam) {
        long now = System.currentTimeMillis();
        double life = ModuleManager.jcTime.value * 1000.0;
        CIRCLES.removeIf(c -> now - c.time > life);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();
        int seg = 64;
        for (Circle c : CIRCLES) {
            double t = (now - c.time) / life;
            double ease = 1 - Math.pow(1 - t, 3);
            double r = ModuleManager.jcRadius.value * ease;
            int a = (int) (230 * (1 - t));
            double x = c.pos.x - cam.x, y = c.pos.y - cam.y + 0.03, z = c.pos.z - cam.z;

            b.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= seg; i++) {
                double ang = i / (double) seg * Math.PI * 2;
                int col = ModuleManager.color(ModuleManager.jcColor, loop(i, seg));
                double cs = Math.cos(ang), sn = Math.sin(ang);
                v(b, m, x + cs * r, y, z + sn * r, Theme.alpha(col, a));
                v(b, m, x + cs * r * 0.7, y, z + sn * r * 0.7, Theme.alpha(col, 0));
            }
            tess.draw();

            RenderSystem.lineWidth(2.0f);
            b.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= seg; i++) {
                double ang = i / (double) seg * Math.PI * 2;
                int col = ModuleManager.color(ModuleManager.jcColor, loop(i, seg));
                v(b, m, x + Math.cos(ang) * r, y, z + Math.sin(ang) * r, Theme.alpha(col, a));
            }
            tess.draw();
        }
    }

    private static void trail(Matrix4f m, Vec3d cam, float td, ClientPlayerEntity p) {
        if (TRAIL.isEmpty()) {
            return;
        }
        List<Vec3d> pts = new ArrayList<>(TRAIL);
        pts.add(new Vec3d(MathHelper.lerp(td, p.prevX, p.getX()), MathHelper.lerp(td, p.prevY, p.getY()),
                MathHelper.lerp(td, p.prevZ, p.getZ())));
        if (pts.size() < 2) {
            return;
        }
        double h = ModuleManager.trHeight.value;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();
        int n = pts.size();

        b.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            Vec3d q = pts.get(i);
            float k = i / (float) (n - 1); // 0 — хвост, 1 — игрок
            int col = ModuleManager.color(ModuleManager.trColor, 1f - k);
            int a = (int) (170 * k);
            double x = q.x - cam.x, y = q.y - cam.y, z = q.z - cam.z;
            v(b, m, x, y + 0.02, z, Theme.alpha(col, a));
            v(b, m, x, y + h, z, Theme.alpha(col, a / 4));
        }
        tess.draw();

        RenderSystem.lineWidth(2.0f);
        b.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            Vec3d q = pts.get(i);
            float k = i / (float) (n - 1);
            int col = ModuleManager.color(ModuleManager.trColor, 1f - k);
            v(b, m, q.x - cam.x, q.y - cam.y + 0.02, q.z - cam.z, Theme.alpha(col, (int) (255 * k)));
        }
        tess.draw();
    }

    private static void hat(Matrix4f m, Vec3d cam, float td, ClientPlayerEntity p) {
        double x = MathHelper.lerp(td, p.prevX, p.getX()) - cam.x;
        double y = MathHelper.lerp(td, p.prevY, p.getY()) - cam.y + p.getHeight() + 0.03;
        double z = MathHelper.lerp(td, p.prevZ, p.getZ()) - cam.z;
        double r = ModuleManager.hatSize.value;
        double h = r * 0.45;
        int seg = 64;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();

        b.begin(GL11.GL_TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        v(b, m, x, y + h, z, Theme.alpha(ModuleManager.color(ModuleManager.hatColor, 0f), 0x90));
        for (int i = 0; i <= seg; i++) {
            double ang = i / (double) seg * Math.PI * 2;
            int col = ModuleManager.color(ModuleManager.hatColor, loop(i, seg));
            v(b, m, x + Math.cos(ang) * r, y, z + Math.sin(ang) * r, Theme.alpha(col, 0x60));
        }
        tess.draw();

        RenderSystem.lineWidth(2.0f);
        b.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            double ang = i / (double) seg * Math.PI * 2;
            int col = ModuleManager.color(ModuleManager.hatColor, loop(i, seg));
            v(b, m, x + Math.cos(ang) * r, y, z + Math.sin(ang) * r, Theme.alpha(col, 0xFF));
        }
        tess.draw();
    }

    private static void outline(MinecraftClient mc, Matrix4f m, Vec3d cam) {
        HitResult hr = mc.crosshairTarget;
        if (!(hr instanceof BlockHitResult) || hr.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = ((BlockHitResult) hr).getBlockPos();
        BlockState st = mc.world.getBlockState(pos);
        if (st.isAir()) {
            return;
        }
        Entity focus = mc.gameRenderer.getCamera().getFocusedEntity();
        VoxelShape shape = st.getOutlineShape(mc.world, pos,
                focus == null ? ShapeContext.absent() : ShapeContext.of(focus));
        if (shape.isEmpty()) {
            return;
        }
        Box bx = shape.getBoundingBox()
                .offset(pos.getX(), pos.getY(), pos.getZ())
                .expand(0.002)
                .offset(-cam.x, -cam.y, -cam.z);
        int c1 = ModuleManager.color(ModuleManager.boColor, 0f);
        int c2 = ModuleManager.color(ModuleManager.boColor, 1f);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();

        if (ModuleManager.boFill.value) {
            int f1 = Theme.alpha(c1, 0x38), f2 = Theme.alpha(c2, 0x38);
            b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
            double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
            // низ / верх
            v(b, m, x1, y1, z1, f1); v(b, m, x2, y1, z1, f1); v(b, m, x2, y1, z2, f1); v(b, m, x1, y1, z2, f1);
            v(b, m, x1, y2, z1, f2); v(b, m, x1, y2, z2, f2); v(b, m, x2, y2, z2, f2); v(b, m, x2, y2, z1, f2);
            // бока
            v(b, m, x1, y1, z1, f1); v(b, m, x1, y2, z1, f2); v(b, m, x2, y2, z1, f2); v(b, m, x2, y1, z1, f1);
            v(b, m, x1, y1, z2, f1); v(b, m, x2, y1, z2, f1); v(b, m, x2, y2, z2, f2); v(b, m, x1, y2, z2, f2);
            v(b, m, x1, y1, z1, f1); v(b, m, x1, y1, z2, f1); v(b, m, x1, y2, z2, f2); v(b, m, x1, y2, z1, f2);
            v(b, m, x2, y1, z1, f1); v(b, m, x2, y2, z1, f2); v(b, m, x2, y2, z2, f2); v(b, m, x2, y1, z2, f1);
            tess.draw();
        }

        RenderSystem.lineWidth(ModuleManager.boWidth.f());
        b.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        int a = 0xFF;
        int lo = Theme.alpha(c1, a), hi = Theme.alpha(c2, a);
        // нижний квадрат
        line(b, m, x1, y1, z1, x2, y1, z1, lo, lo);
        line(b, m, x2, y1, z1, x2, y1, z2, lo, lo);
        line(b, m, x2, y1, z2, x1, y1, z2, lo, lo);
        line(b, m, x1, y1, z2, x1, y1, z1, lo, lo);
        // верхний квадрат
        line(b, m, x1, y2, z1, x2, y2, z1, hi, hi);
        line(b, m, x2, y2, z1, x2, y2, z2, hi, hi);
        line(b, m, x2, y2, z2, x1, y2, z2, hi, hi);
        line(b, m, x1, y2, z2, x1, y2, z1, hi, hi);
        // вертикали
        line(b, m, x1, y1, z1, x1, y2, z1, lo, hi);
        line(b, m, x2, y1, z1, x2, y2, z1, lo, hi);
        line(b, m, x2, y1, z2, x2, y2, z2, lo, hi);
        line(b, m, x1, y1, z2, x1, y2, z2, lo, hi);
        tess.draw();
    }

    // ---------- утилиты ----------

    /** Бесшовный градиент по кругу: 0 → 1 → 0. */
    private static float loop(int i, int seg) {
        return 1f - Math.abs(2f * i / seg - 1f);
    }

    private static void line(BufferBuilder b, Matrix4f m, double x1, double y1, double z1,
                             double x2, double y2, double z2, int c1, int c2) {
        v(b, m, x1, y1, z1, c1);
        v(b, m, x2, y2, z2, c2);
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, int c) {
        b.vertex(m, (float) x, (float) y, (float) z)
                .color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255)
                .next();
    }
}
