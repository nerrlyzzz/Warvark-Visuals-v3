package dev.warax.visuals.module;

import dev.warax.visuals.gui.Gfx;
import dev.warax.visuals.module.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.Random;

/**
 * Секретный раздел: только косметические эффекты (темы, эффекты экрана, ауры частиц).
 * Всё видно только вам и никак не влияет на игру.
 */
public final class SecretFx {
    private static final Random R = new Random();

    public static Module RAINBOWUI, NEBULA, GOLD;
    public static Module NEONFRAME, RAINBOWFRAME, VIGNETTE, CINEMA, SCANLINES, STARFALL, SNOW, SAKURA, MATRIX, PULSE,
            GRAIN, BUBBLES, FIREFLIES, SEPIA;

    private static final String[][] PART = {
            {"Аура сердец", "Сердечки кружат вокруг вас."},
            {"Музыкальная аура", "Ноты над головой."},
            {"Огненное кольцо", "Кольцо огня вокруг пояса."},
            {"Кольцо душ", "Голубое пламя душ вокруг вас."},
            {"Портальный вихрь", "Фиолетовая спираль."},
            {"Звёздный шлейф", "Светящийся след под ногами."},
            {"Магическая сфера", "Руны зачарования вокруг вас."},
            {"Аура ведьмы", "Фиолетовые искры."},
            {"Салют над головой", "Искры фейерверка над вами."},
            {"Облачко", "Маленькое облако над головой."},
            {"Дыхание дракона", "Розовый дым под ногами."},
            {"Тотемная спираль", "Зелёно-жёлтая спираль."},
            {"Изумрудная пыль", "Зелёные искорки вокруг."},
            {"Пепел", "Тёмный пепел в воздухе."},
            {"Белый пепел", "Светлые хлопья вокруг."},
            {"Багровые споры", "Красные споры Незера."},
            {"Искажённые споры", "Бирюзовые споры."},
            {"Обратный портал", "Спираль фиолетовых частиц."},
            {"Крит-аура", "Звёздочки крита по кругу."},
            {"Пузыри", "Лопающиеся пузыри над головой."},
    };
    // 0 кольцо, 1 над головой, 2 под ногами, 3 спираль, 4 сфера
    private static final int[] PP = {0, 1, 0, 0, 3, 2, 4, 4, 1, 1, 2, 3, 4, 4, 4, 4, 4, 3, 0, 1};
    // раз в N тиков
    private static final int[] PR = {5, 6, 1, 1, 1, 2, 2, 2, 4, 3, 2, 1, 2, 1, 1, 1, 1, 1, 2, 3};
    private static Module[] PM;
    private static ParticleEffect[] PT;
    private static int ticks;

    private static final int N = 70;
    private static final float[][] FX = new float[5][N], FY = new float[5][N], FS = new float[5][N];
    private static final boolean[] SEEDED = new boolean[5];
    private static float[] mcol;
    private static int mcolN;
    private static long lastFrame;
    private static final String GLY = "01ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

    private SecretFx() {
    }

    private static Module s(String n, String d) {
        Module m = new Module(n, d, Category.SECRET);
        ModuleManager.MODULES.add(m);
        return m;
    }

    public static void init() {
        RAINBOWUI = s("Радужный интерфейс", "Вся тема мода переливается радугой.");
        NEBULA = s("Тема «Небула»", "Секретная розово-фиолетовая тема.");
        GOLD = s("Тема «Золото»", "Секретная золотая тема.");
        NEONFRAME = s("Неоновая рамка", "Светящаяся рамка по краям экрана.");
        RAINBOWFRAME = s("Радужная рамка", "Рамка экрана цветами радуги.");
        VIGNETTE = s("Виньетка темы", "Края экрана в цвет темы.");
        CINEMA = s("Кинорежим", "Чёрные полосы сверху и снизу.");
        SCANLINES = s("Сканлайны", "Эффект старого монитора.");
        STARFALL = s("Звездопад", "Падающие звёзды на экране.");
        SNOW = s("Снегопад", "Снежинки поверх экрана.");
        SAKURA = s("Лепестки сакуры", "Розовые лепестки летят по экрану.");
        MATRIX = s("Матрица", "Зелёный дождь символов.");
        PULSE = s("Пульс экрана", "Экран мягко пульсирует цветом темы.");
        GRAIN = s("Зерно плёнки", "Шум как на старой камере.");
        BUBBLES = s("Пузыри на экране", "Прозрачные пузыри поднимаются вверх.");
        FIREFLIES = s("Светлячки", "Тёплые огоньки плавают по экрану.");
        SEPIA = s("Сепия", "Тёплый старинный оттенок.");
        PM = new Module[PART.length];
        for (int i = 0; i < PART.length; i++) {
            PM[i] = s(PART[i][0], PART[i][1]);
        }
    }

    /** Цвет секретной темы или 0, если ни одна не включена. */
    public static int themeColor(boolean hi) {
        if (RAINBOWUI != null && RAINBOWUI.isActive()) {
            return ModuleManager.rainbow(hi ? 0f : 0.15f);
        }
        if (NEBULA != null && NEBULA.isActive()) {
            return hi ? 0xFFFF6AD5 : 0xFF7A3CFF;
        }
        if (GOLD != null && GOLD.isActive()) {
            return hi ? 0xFFFFE27A : 0xFFC98A1B;
        }
        return ExtraFx.themeColor(hi);
    }

    private static ParticleEffect[] types() {
        if (PT == null) {
            PT = new ParticleEffect[]{
                    ParticleTypes.HEART, ParticleTypes.NOTE, ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME,
                    ParticleTypes.PORTAL, ParticleTypes.END_ROD, ParticleTypes.ENCHANT, ParticleTypes.WITCH,
                    ParticleTypes.FIREWORK, ParticleTypes.CLOUD, ParticleTypes.DRAGON_BREATH,
                    ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.ASH,
                    ParticleTypes.WHITE_ASH, ParticleTypes.CRIMSON_SPORE, ParticleTypes.WARPED_SPORE,
                    ParticleTypes.REVERSE_PORTAL, ParticleTypes.CRIT, ParticleTypes.BUBBLE_POP
            };
        }
        return PT;
    }

    public static void tick(MinecraftClient mc) {
        if (PM == null || mc.player == null || mc.world == null || !Secret.unlocked()) {
            return;
        }
        ticks++;
        ParticleEffect[] t = types();
        for (int i = 0; i < PM.length && i < t.length; i++) {
            if (PM[i].isActive() && ticks % PR[i] == 0) {
                try {
                    spawn(mc, mc.player, t[i], PP[i]);
                } catch (Throwable ignored) {
                    // частицы не критичны
                }
            }
        }
    }

    private static void spawn(MinecraftClient mc, ClientPlayerEntity p, ParticleEffect t, int pat) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double a = ticks * 0.25;
        switch (pat) {
            case 0:
                for (int k = 0; k < 2; k++) {
                    double an = a + k * Math.PI;
                    mc.world.addParticle(t, x + Math.cos(an) * 0.8, y + 1.0, z + Math.sin(an) * 0.8, 0, 0, 0);
                }
                break;
            case 1:
                mc.world.addParticle(t, x + (R.nextDouble() - 0.5) * 0.6, y + p.getHeight() + 0.4,
                        z + (R.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
                break;
            case 2:
                mc.world.addParticle(t, x + (R.nextDouble() - 0.5) * 0.4, y + 0.05,
                        z + (R.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
                break;
            case 3: {
                double h = (ticks % 40) / 40.0 * p.getHeight();
                mc.world.addParticle(t, x + Math.cos(a * 2) * 0.7, y + h, z + Math.sin(a * 2) * 0.7, 0, 0, 0);
                break;
            }
            default: {
                double u = R.nextDouble() * Math.PI * 2, v = R.nextDouble() * Math.PI;
                mc.world.addParticle(t, x + Math.cos(u) * Math.sin(v) * 1.2, y + 1.0 + Math.cos(v) * 1.2,
                        z + Math.sin(u) * Math.sin(v) * 1.2, 0, 0, 0);
                break;
            }
        }
    }

    private static boolean on(Module m) {
        return m != null && m.isActive();
    }

    /** Эффекты экрана, рисуются под HUD. */
    public static void render2D(MatrixStack ms) {
        if (PM == null || !Secret.unlocked()) {
            return;
        }
        try {
            draw(ms);
        } catch (Throwable ignored) {
            // не критично
        }
    }

    private static void draw(MatrixStack ms) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float wave = (float) (Math.sin(now / 400.0) * 0.5 + 0.5);

        if (on(SEPIA)) {
            Gfx.rect(ms, 0, 0, w, h, 0x30704214);
        }
        if (on(PULSE)) {
            Gfx.rect(ms, 0, 0, w, h, Theme.alpha(Theme.a1(), (int) (0x28 * wave)));
        }
        if (on(VIGNETTE)) {
            int c = Theme.alpha(Theme.a1(), 0x90), z0 = Theme.alpha(Theme.a1(), 0);
            int e = Math.max(20, h / 5);
            Gfx.vgrad(ms, 0, 0, w, e, c, z0);
            Gfx.vgrad(ms, 0, h - e, w, h, z0, c);
            Gfx.hgrad(ms, 0, 0, e, h, c, z0);
            Gfx.hgrad(ms, w - e, 0, w, h, z0, c);
        }
        if (on(SCANLINES)) {
            for (int y = 0; y < h; y += 3) {
                Gfx.rect(ms, 0, y, w, y + 1, 0x16000000);
            }
        }
        if (on(GRAIN)) {
            for (int i = 0; i < 200; i++) {
                int x = R.nextInt(Math.max(1, w)), y = R.nextInt(Math.max(1, h));
                Gfx.rect(ms, x, y, x + 1, y + 1, R.nextBoolean() ? 0x26FFFFFF : 0x26000000);
            }
        }
        flakes(ms, STARFALL, 0, w, h, dt, now);
        flakes(ms, SNOW, 1, w, h, dt, now);
        flakes(ms, SAKURA, 2, w, h, dt, now);
        flakes(ms, BUBBLES, 3, w, h, dt, now);
        flakes(ms, FIREFLIES, 4, w, h, dt, now);
        if (on(MATRIX)) {
            matrix(ms, mc, w, h, dt);
        }
        if (on(CINEMA)) {
            int b = h / 9;
            Gfx.rect(ms, 0, 0, w, b, 0xFF000000);
            Gfx.rect(ms, 0, h - b, w, h, 0xFF000000);
        }
        if (on(NEONFRAME)) {
            frame(ms, w, h, false, now);
        }
        if (on(RAINBOWFRAME)) {
            frame(ms, w, h, true, now);
        }
    }

    private static void flakes(MatrixStack ms, Module m, int k, int w, int h, float dt, long now) {
        if (!on(m)) {
            return;
        }
        float[] xs = FX[k], ys = FY[k], ss = FS[k];
        if (!SEEDED[k]) {
            for (int i = 0; i < N; i++) {
                xs[i] = R.nextFloat();
                ys[i] = R.nextFloat();
                ss[i] = 0.3f + R.nextFloat() * 0.7f;
            }
            SEEDED[k] = true;
        }
        for (int i = 0; i < N; i++) {
            float sp = ss[i];
            switch (k) {
                case 0:
                    ys[i] += dt * sp * 0.9f;
                    xs[i] += dt * sp * 0.3f;
                    break;
                case 1:
                    ys[i] += dt * sp * 0.15f;
                    xs[i] += (float) Math.sin(now / 900.0 + i) * dt * 0.02f;
                    break;
                case 2:
                    ys[i] += dt * sp * 0.12f;
                    xs[i] += dt * 0.05f + (float) Math.sin(now / 600.0 + i) * dt * 0.03f;
                    break;
                case 3:
                    ys[i] -= dt * sp * 0.12f;
                    break;
                default:
                    xs[i] += (float) Math.sin(now / 1500.0 + i * 1.7) * dt * 0.03f;
                    ys[i] += (float) Math.cos(now / 1300.0 + i * 2.3) * dt * 0.03f;
                    break;
            }
            if (ys[i] > 1.05f) {
                ys[i] = -0.05f;
                xs[i] = R.nextFloat();
            } else if (ys[i] < -0.05f) {
                ys[i] = 1.05f;
                xs[i] = R.nextFloat();
            }
            if (xs[i] > 1.05f) {
                xs[i] = -0.05f;
            } else if (xs[i] < -0.05f) {
                xs[i] = 1.05f;
            }
            int x = (int) (xs[i] * w), y = (int) (ys[i] * h);
            switch (k) {
                case 0: {
                    for (int t = 5; t >= 1; t--) {
                        int tx = x - t * 3, ty = y - t * 9 / 3;
                        Gfx.rect(ms, tx, ty, tx + 2, ty + 1, Theme.alpha(0xFFFFFFFF, (int) (0x90 * sp / t)));
                    }
                    Gfx.rect(ms, x, y, x + 2, y + 2, 0xFFFFFFFF);
                    break;
                }
                case 1: {
                    int s = sp > 0.7f ? 2 : 1;
                    Gfx.rect(ms, x, y, x + s, y + s, Theme.alpha(0xFFFFFFFF, (int) (0xA0 + 0x50 * sp)));
                    break;
                }
                case 2:
                    Gfx.round(ms, x, y, x + 4, y + 3, 1, 0xC8FFB7D5);
                    break;
                case 3: {
                    int r = 3 + (int) (sp * 6);
                    Gfx.round(ms, x, y, x + r, y + r, r / 2, 0x30FFFFFF);
                    Gfx.rect(ms, x + 1, y + 1, x + 2, y + 2, 0x80FFFFFF);
                    break;
                }
                default: {
                    float b = (float) (Math.sin(now / 300.0 + i) * 0.5 + 0.5);
                    Gfx.glow(ms, x, y, x + 2, y + 2, 1, 0xFFFFE680, 3, (int) (0x90 * b));
                    Gfx.rect(ms, x, y, x + 2, y + 2, Theme.alpha(0xFFFFF2B0, 0x80 + (int) (0x7F * b)));
                    break;
                }
            }
        }
    }

    private static void matrix(MatrixStack ms, MinecraftClient mc, int w, int h, float dt) {
        int n = Math.max(1, w / 9);
        if (mcol == null || mcolN != n) {
            mcol = new float[n];
            for (int i = 0; i < n; i++) {
                mcol[i] = R.nextFloat() * h;
            }
            mcolN = n;
        }
        for (int i = 0; i < n; i++) {
            mcol[i] += dt * (60 + (i * 37 % 50));
            if (mcol[i] - 70 > h) {
                mcol[i] = -R.nextInt(Math.max(1, h));
            }
            for (int j = 0; j < 8; j++) {
                float y = mcol[i] - j * 9;
                if (y < -9 || y > h) {
                    continue;
                }
                char ch = GLY.charAt(Math.abs(i * 7 + j * 13 + (int) (mcol[i] / 9)) % GLY.length());
                int a = j == 0 ? 0xFF : Math.max(0x20, 0xC0 - j * 0x18);
                int col = (a << 24) | (j == 0 ? 0xD8FFD8 : 0x22FF55);
                mc.textRenderer.draw(ms, String.valueOf(ch), i * 9, y, col);
            }
        }
    }

    private static int fcol(boolean rb, float t, long now) {
        if (rb) {
            return ModuleManager.rainbow(t);
        }
        return Theme.grad((float) (Math.sin(now / 500.0 + t * 6) * 0.5 + 0.5));
    }

    private static void frame(MatrixStack ms, int w, int h, boolean rb, long now) {
        int th = 2, seg = 16;
        for (int x = 0; x < w; x += seg) {
            int x2 = Math.min(x + seg, w);
            Gfx.rect(ms, x, 0, x2, th, fcol(rb, x / (float) w, now));
            Gfx.rect(ms, x, h - th, x2, h, fcol(rb, 1f - x / (float) w, now));
        }
        for (int y = 0; y < h; y += seg) {
            int y2 = Math.min(y + seg, h);
            Gfx.rect(ms, 0, y, th, y2, fcol(rb, 1f - y / (float) h, now));
            Gfx.rect(ms, w - th, y, w, y2, fcol(rb, y / (float) h, now));
        }
        int c = fcol(rb, 0.5f, now);
        Gfx.vgrad(ms, 0, th, w, th + 10, Theme.alpha(c, 0x50), Theme.alpha(c, 0));
        Gfx.vgrad(ms, 0, h - th - 10, w, h - th, Theme.alpha(c, 0), Theme.alpha(c, 0x50));
        Gfx.hgrad(ms, th, 0, th + 10, h, Theme.alpha(c, 0x50), Theme.alpha(c, 0));
        Gfx.hgrad(ms, w - th - 10, 0, w - th, h, Theme.alpha(c, 0), Theme.alpha(c, 0x50));
    }
}
