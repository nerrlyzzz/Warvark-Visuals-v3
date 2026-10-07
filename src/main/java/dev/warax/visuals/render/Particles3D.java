package dev.warax.visuals.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.module.Setting;
import dev.warax.visuals.module.Theme;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Собственный движок частиц Warax: светящиеся billboard-частицы с физикой,
 * отскоком от блоков и аддитивным свечением. Никаких ванильных текстур.
 */
public final class Particles3D {
    public static final int GLOW = 0, STAR = 1, HEART = 2, SPARK = 3, DIAMOND = 4, RING = 5;
    public static final String[] SHAPES = {"Свечение", "Звёзды", "Сердечки", "Искры", "Ромбы", "Кольца"};

    private static final class P {
        double x, y, z, vx, vy, vz;
        float size, rot, spin, life, gravity, phase;
        int color, shape;
        long born;
        boolean collide, firefly, grow;
    }

    private static final List<P> LIST = new ArrayList<>();
    private static final Random R = new Random();
    private static final int MAX = 1500;
    private static long lastNs;
    private static int walkTick;
    private static boolean wasGround = true;

    private Particles3D() {
    }

    public static boolean hasAny() {
        return !LIST.isEmpty();
    }

    public static void clear() {
        LIST.clear();
    }

    // ------------------------------------------------------------ спавн

    public static void spawn(double x, double y, double z, double vx, double vy, double vz,
                             int shape, int color, float size, float life, float gravity) {
        if (LIST.size() >= MAX) {
            LIST.remove(0);
        }
        P p = new P();
        p.x = x; p.y = y; p.z = z;
        p.vx = vx; p.vy = vy; p.vz = vz;
        p.shape = shape;
        p.color = color;
        p.size = size;
        p.life = Math.max(0.1f, life);
        p.gravity = gravity;
        p.rot = R.nextFloat() * 6.2832f;
        p.spin = (R.nextFloat() - 0.5f) * 6f;
        p.phase = R.nextFloat() * 100f;
        p.born = System.currentTimeMillis();
        p.collide = ModuleManager.ptCollide == null || ModuleManager.ptCollide.value;
        p.grow = shape == RING;
        LIST.add(p);
    }

    /** Взрыв частиц во все стороны. */
    public static void burst(double x, double y, double z, int n, int shape, Setting.Mode col,
                             float size, float speed, float life, float gravity) {
        for (int i = 0; i < n; i++) {
            double th = R.nextDouble() * Math.PI * 2, ph = Math.acos(2 * R.nextDouble() - 1);
            double sp = speed * (0.4 + R.nextDouble() * 0.8);
            double vx = Math.sin(ph) * Math.cos(th) * sp;
            double vy = Math.abs(Math.cos(ph)) * sp * 0.9 + speed * 0.25;
            double vz = Math.sin(ph) * Math.sin(th) * sp;
            spawn(x, y, z, vx, vy, vz, shape, ModuleManager.color(col, R.nextFloat()),
                    size * (0.6f + R.nextFloat() * 0.8f), life * (0.7f + R.nextFloat() * 0.6f), gravity);
        }
    }

    /** Частицы по объёму сущности (удар). */
    public static void onEntity(Entity e, int n, int shape, Setting.Mode col) {
        double w = e.getWidth(), h = e.getHeight();
        float size = sz(), life = lf();
        for (int i = 0; i < n; i++) {
            double ox = (R.nextDouble() - 0.5) * w, oz = (R.nextDouble() - 0.5) * w;
            double sp = 2.2 + R.nextDouble() * 2.5;
            double len = Math.max(0.001, Math.sqrt(ox * ox + oz * oz));
            spawn(e.getX() + ox, e.getY() + R.nextDouble() * h, e.getZ() + oz,
                    ox / len * sp, 0.8 + R.nextDouble() * 2.2, oz / len * sp,
                    shape, ModuleManager.color(col, R.nextFloat()), size * (0.6f + R.nextFloat() * 0.8f),
                    life * (0.7f + R.nextFloat() * 0.6f), gr());
        }
        // волна-кольцо по центру цели
        spawn(e.getX(), e.getY() + h * 0.5, e.getZ(), 0, 0, 0, RING, ModuleManager.color(col, 0.5f),
                (float) Math.max(0.6, w * 1.4), 0.45f, 0);
    }

    private static float sz() {
        return ModuleManager.ptSize == null ? 0.12f : (float) ModuleManager.ptSize.value;
    }

    private static float lf() {
        return ModuleManager.ptLife == null ? 1.2f : (float) ModuleManager.ptLife.value;
    }

    private static float gr() {
        return ModuleManager.ptGravity == null ? 6f : (float) ModuleManager.ptGravity.value;
    }

    private static int shape() {
        return ModuleManager.ptShape == null ? GLOW : ModuleManager.ptShape.index;
    }

    /**
     * Замена ванильных частиц боя. true — ванильную частицу надо отменить.
     */
    public static boolean replaceVanilla(ParticleEffect fx, double x, double y, double z,
                                         double vx, double vy, double vz) {
        if (ModuleManager.PARTICLES == null || !ModuleManager.PARTICLES.isActive() || !ModuleManager.ptReplace.value) {
            return false;
        }
        Object t = fx.getType();
        Setting.Mode col = ModuleManager.ptColor;
        if (t == ParticleTypes.CRIT || t == ParticleTypes.ENCHANTED_HIT) {
            spawn(x, y, z, vx * 6, vy * 6 + 1, vz * 6, t == ParticleTypes.CRIT ? SPARK : STAR,
                    ModuleManager.color(col, R.nextFloat()), sz() * 0.9f, lf() * 0.8f, gr());
            return true;
        }
        if (t == ParticleTypes.DAMAGE_INDICATOR) {
            spawn(x, y, z, (R.nextDouble() - 0.5) * 2, 1.5 + R.nextDouble(), (R.nextDouble() - 0.5) * 2, HEART,
                    ModuleManager.color(col, R.nextFloat()), sz(), lf(), gr() * 0.6f);
            return true;
        }
        if (t == ParticleTypes.SWEEP_ATTACK) {
            spawn(x, y, z, 0, 0, 0, RING, ModuleManager.color(col, 0.5f), 1.4f, 0.35f, 0);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ эмиттеры (20 тиков/с)

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            clear();
            return;
        }
        if (ModuleManager.PARTICLES == null || !ModuleManager.PARTICLES.isActive()) {
            return;
        }
        Setting.Mode col = ModuleManager.ptColor;
        int n = ModuleManager.ptCount.i();
        boolean ground = p.isOnGround();
        double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
        boolean moving = dx * dx + dz * dz > 0.0009;

        // шаги
        if (ModuleManager.ptWalk.value && ground && moving && ++walkTick % 2 == 0) {
            for (int i = 0; i < Math.max(1, n / 4); i++) {
                spawn(p.getX() + (R.nextDouble() - 0.5) * 0.5, p.getY() + 0.05, p.getZ() + (R.nextDouble() - 0.5) * 0.5,
                        (R.nextDouble() - 0.5) * 0.6, 0.6 + R.nextDouble() * 0.8, (R.nextDouble() - 0.5) * 0.6,
                        shape(), ModuleManager.color(col, R.nextFloat()), sz() * 0.7f, lf() * 0.7f, gr() * 0.5f);
            }
        }
        // прыжок
        if (ModuleManager.ptJump.value && wasGround && !ground && p.getVelocity().y > 0.2) {
            burst(p.getX(), p.getY() + 0.1, p.getZ(), n, shape(), col, sz(), 2.5f, lf(), gr());
            spawn(p.getX(), p.getY() + 0.05, p.getZ(), 0, 0, 0, RING, ModuleManager.color(col, 0.3f), 1.2f, 0.5f, 0);
        }
        wasGround = ground;
        // светлячки вокруг игрока
        if (ModuleManager.ptAmbient.value && R.nextInt(3) == 0) {
            double a = R.nextDouble() * Math.PI * 2, r = 2 + R.nextDouble() * 6;
            spawn(p.getX() + Math.cos(a) * r, p.getY() + 0.3 + R.nextDouble() * 2.5, p.getZ() + Math.sin(a) * r,
                    0, 0.15, 0, GLOW, ModuleManager.color(col, R.nextFloat()), sz() * 0.8f, 4f + R.nextFloat() * 3f, 0);
            LIST.get(LIST.size() - 1).firefly = true;
        }
    }

    // ------------------------------------------------------------ рендер

    public static void render(Matrix4f m, Vec3d cam, Camera camera) {
        long nowNs = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (nowNs - lastNs) / 1.0e9f);
        lastNs = nowNs;
        if (LIST.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();

        // базис billboard'а по направлению камеры
        double yaw = Math.toRadians(camera.getYaw()), pitch = Math.toRadians(camera.getPitch());
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = fy * rz - fz * ry, uy = fz * rx - fx * rz, uz = fx * ry - fy * rx;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1e-6) {
            ul = 1;
        }
        ux /= ul; uy /= ul; uz /= ul;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // аддитивное свечение
        b.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        Iterator<P> it = LIST.iterator();
        while (it.hasNext()) {
            P p = it.next();
            float age = (now - p.born) / 1000f;
            float t = age / p.life;
            if (t >= 1f) {
                it.remove();
                continue;
            }
            step(mc, p, dt, age);
            float fade = t < 0.12f ? t / 0.12f : 1f - (float) Math.pow((t - 0.12f) / 0.88f, 2);
            if (p.firefly) {
                fade *= 0.55f + 0.45f * (float) Math.sin(age * 4 + p.phase);
            }
            int a = (int) (255 * Math.max(0, Math.min(1, fade)));
            float s = p.grow ? p.size * (0.15f + (float) (1 - Math.pow(1 - t, 3))) : p.size * (1f - t * 0.45f);
            double cx = p.x - cam.x, cy = p.y - cam.y, cz = p.z - cam.z;
            Basis bs = new Basis(rx, ry, rz, ux, uy, uz, p.rot);
            switch (p.shape) {
                case STAR: star(b, m, bs, cx, cy, cz, s, p.color, a); break;
                case HEART: heart(b, m, bs, cx, cy, cz, s, p.color, a); break;
                case SPARK: spark(b, m, p, cx, cy, cz, s, fx, fy, fz, a); break;
                case DIAMOND: diamond(b, m, bs, cx, cy, cz, s, p.color, a); break;
                case RING: ring(b, m, bs, cx, cy, cz, s, p.color, a); break;
                default: glow(b, m, bs, cx, cy, cz, s, p.color, a); break;
            }
        }
        tess.draw();
        RenderSystem.defaultBlendFunc();
    }

    private static void step(MinecraftClient mc, P p, float dt, float age) {
        if (p.grow) {
            return;
        }
        if (p.firefly) {
            p.vx += Math.sin(age * 1.7 + p.phase) * 0.6 * dt;
            p.vz += Math.cos(age * 1.3 + p.phase) * 0.6 * dt;
            p.vy += Math.sin(age * 2.1 + p.phase) * 0.4 * dt;
        }
        p.vy -= p.gravity * dt;
        double drag = Math.pow(0.9, dt * 20);
        p.vx *= drag; p.vy *= drag; p.vz *= drag;
        double ny = p.y + p.vy * dt;
        if (p.collide && p.vy < 0 && mc.world != null) {
            BlockPos bp = new BlockPos(p.x, ny, p.z);
            BlockState st = mc.world.getBlockState(bp);
            if (!st.isAir() && !st.getCollisionShape(mc.world, bp).isEmpty()) {
                double top = bp.getY() + st.getCollisionShape(mc.world, bp).getMax(net.minecraft.util.math.Direction.Axis.Y);
                if (ny < top) {
                    ny = top + 0.01;
                    p.vy = -p.vy * 0.45;
                    p.vx *= 0.7;
                    p.vz *= 0.7;
                    p.spin *= 0.6f;
                }
            }
        }
        p.x += p.vx * dt;
        p.y = ny;
        p.z += p.vz * dt;
        p.rot += p.spin * dt;
    }

    // ------------------------------------------------------------ формы

    private static final class Basis {
        final double ax, ay, az, bx, by, bz;

        Basis(double rx, double ry, double rz, double ux, double uy, double uz, float rot) {
            double c = Math.cos(rot), s = Math.sin(rot);
            ax = rx * c + ux * s; ay = ry * c + uy * s; az = rz * c + uz * s;
            bx = -rx * s + ux * c; by = -ry * s + uy * c; bz = -rz * s + uz * c;
        }
    }

    private static void pv(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz,
                           double u, double v, int col, int a) {
        b.vertex(m, (float) (cx + bs.ax * u + bs.bx * v), (float) (cy + bs.ay * u + bs.by * v),
                (float) (cz + bs.az * u + bs.bz * v))
                .color((col >> 16) & 255, (col >> 8) & 255, col & 255, Math.max(0, Math.min(255, a))).next();
    }

    /** Мягкий круглый ореол: центр яркий, край прозрачный. */
    private static void halo(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz,
                             double r, int cIn, int aIn, int cOut, int aOut) {
        int seg = 14;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            pv(b, m, bs, cx, cy, cz, 0, 0, cIn, aIn);
            pv(b, m, bs, cx, cy, cz, Math.cos(a0) * r, Math.sin(a0) * r, cOut, aOut);
            pv(b, m, bs, cx, cy, cz, Math.cos(a1) * r, Math.sin(a1) * r, cOut, aOut);
        }
    }

    private static int white(int c, float k) {
        return Theme.lerp(c, 0xFFFFFFFF, k);
    }

    private static void glow(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz, float s, int c, int a) {
        halo(b, m, bs, cx, cy, cz, s * 2.2, c, a / 3, c, 0);
        halo(b, m, bs, cx, cy, cz, s, white(c, 0.6f), a, c, a / 4);
    }

    private static void star(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz, float s, int c, int a) {
        halo(b, m, bs, cx, cy, cz, s * 2.0, c, a / 4, c, 0);
        int pts = 10;
        for (int i = 0; i < pts; i++) {
            double a0 = i * Math.PI * 2 / pts - Math.PI / 2, a1 = (i + 1) * Math.PI * 2 / pts - Math.PI / 2;
            double r0 = (i % 2 == 0) ? s * 1.3 : s * 0.5, r1 = (i % 2 == 0) ? s * 0.5 : s * 1.3;
            pv(b, m, bs, cx, cy, cz, 0, 0, white(c, 0.7f), a);
            pv(b, m, bs, cx, cy, cz, Math.cos(a0) * r0, Math.sin(a0) * r0, c, a);
            pv(b, m, bs, cx, cy, cz, Math.cos(a1) * r1, Math.sin(a1) * r1, c, a);
        }
    }

    private static void heart(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz, float s, int c, int a) {
        halo(b, m, bs, cx, cy, cz, s * 2.0, c, a / 4, c, 0);
        int seg = 28;
        double k = s / 14.0;
        double px = 0, py = 0;
        for (int i = 0; i <= seg; i++) {
            double t = i * Math.PI * 2 / seg;
            double x = 16 * Math.pow(Math.sin(t), 3) * k;
            double y = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) * k;
            if (i > 0) {
                pv(b, m, bs, cx, cy, cz, 0, s * 0.2, white(c, 0.55f), a);
                pv(b, m, bs, cx, cy, cz, px, py, c, a);
                pv(b, m, bs, cx, cy, cz, x, y, c, a);
            }
            px = x;
            py = y;
        }
    }

    private static void diamond(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz, float s, int c, int a) {
        halo(b, m, bs, cx, cy, cz, s * 1.9, c, a / 4, c, 0);
        double[][] q = {{0, s * 1.4}, {s * 0.8, 0}, {0, -s * 1.4}, {-s * 0.8, 0}};
        for (int i = 0; i < 4; i++) {
            double[] p0 = q[i], p1 = q[(i + 1) % 4];
            pv(b, m, bs, cx, cy, cz, 0, 0, white(c, 0.75f), a);
            pv(b, m, bs, cx, cy, cz, p0[0], p0[1], c, a);
            pv(b, m, bs, cx, cy, cz, p1[0], p1[1], c, a);
        }
    }

    private static void ring(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz, float s, int c, int a) {
        int seg = 40;
        double ro = s, rm = s * 0.86, ri = s * 0.62;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            // внешняя кромка (яркая) → внутренний мягкий край
            quad(b, m, bs, cx, cy, cz, c0 * ro, s0 * ro, c1 * ro, s1 * ro, c1 * rm, s1 * rm, c0 * rm, s0 * rm, c, a / 3, white(c, 0.4f), a);
            quad(b, m, bs, cx, cy, cz, c0 * rm, s0 * rm, c1 * rm, s1 * rm, c1 * ri, s1 * ri, c0 * ri, s0 * ri, white(c, 0.4f), a, c, 0);
        }
    }

    private static void quad(BufferBuilder b, Matrix4f m, Basis bs, double cx, double cy, double cz,
                             double u0, double v0, double u1, double v1, double u2, double v2, double u3, double v3,
                             int cA, int aA, int cB, int aB) {
        pv(b, m, bs, cx, cy, cz, u0, v0, cA, aA);
        pv(b, m, bs, cx, cy, cz, u1, v1, cA, aA);
        pv(b, m, bs, cx, cy, cz, u2, v2, cB, aB);
        pv(b, m, bs, cx, cy, cz, u0, v0, cA, aA);
        pv(b, m, bs, cx, cy, cz, u2, v2, cB, aB);
        pv(b, m, bs, cx, cy, cz, u3, v3, cB, aB);
    }

    /** Искра: вытянутый по скорости штрих + яркая головка. */
    private static void spark(BufferBuilder b, Matrix4f m, P p, double cx, double cy, double cz, float s,
                              double fx, double fy, double fz, int a) {
        double vx = p.vx, vy = p.vy, vz = p.vz;
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vl < 0.05) {
            vx = 0; vy = 1; vz = 0; vl = 1;
        }
        double len = Math.min(1.2, 0.08 + vl * 0.06) * (s / 0.12);
        double dx = vx / vl, dy = vy / vl, dz = vz / vl;
        // перпендикуляр к штриху в плоскости экрана
        double px = dy * fz - dz * fy, py = dz * fx - dx * fz, pz = dx * fy - dy * fx;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 1e-6) {
            pl = 1;
        }
        double w = s * 0.35;
        px = px / pl * w; py = py / pl * w; pz = pz / pl * w;
        double tx = cx - dx * len, ty = cy - dy * len, tz = cz - dz * len;
        int c = p.color, hc = white(c, 0.7f);
        v(b, m, cx + px, cy + py, cz + pz, hc, a);
        v(b, m, cx - px, cy - py, cz - pz, hc, a);
        v(b, m, tx, ty, tz, c, 0);
        v(b, m, cx + px, cy + py, cz + pz, hc, a);
        v(b, m, cx + dx * w, cy + dy * w, cz + dz * w, hc, a);
        v(b, m, cx - px, cy - py, cz - pz, hc, a);
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, int col, int a) {
        b.vertex(m, (float) x, (float) y, (float) z)
                .color((col >> 16) & 255, (col >> 8) & 255, col & 255, Math.max(0, Math.min(255, a))).next();
    }
}
