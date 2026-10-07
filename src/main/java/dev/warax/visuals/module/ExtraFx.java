package dev.warax.visuals.module;

import dev.warax.visuals.gui.Fonts;
import dev.warax.visuals.gui.Gfx;
import dev.warax.visuals.module.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * v2.7: +20 функций в «Визуалы», «Мир», «Экран», «HUD» и +40 в «Секрет».
 * Всё косметическое и видно только вам.
 */
public final class ExtraFx {
    private static final Random R = new Random();

    private static final class P {
        Module m;
        int shape, every, trig, color, type;
    }

    private static final List<P> PS = new ArrayList<>();
    private static final Module[] SCR = new Module[20];
    private static final Module[] INFO = new Module[20];
    private static final Module[] THEMES = new Module[10];
    private static final Module[] SFX = new Module[15];
    private static final int[][] THEME_COL = {
            {0xFF4FD8FF, 0xFF1F5BFF}, {0xFFFFB347, 0xFFE0201B}, {0xFFA8FFD8, 0xFF1FBF8F},
            {0xFFFF8FB1, 0xFFC2185B}, {0xFFE6FBFF, 0xFF7FB8FF}, {0xFFFFC36B, 0xFFE0457B},
            {0xFF39FF14, 0xFF00E5FF}, {0xFF7DFFB0, 0xFF0E9F5A}, {0xFF8E9BFF, 0xFF2A1B6B},
            {0xFFFFD8A8, 0xFFB86B2E}
    };
    private static final String[] MOON = {"Полнолуние", "Убывающая луна", "Последняя четверть", "Убывающий серп",
            "Новолуние", "Растущий серп", "Первая четверть", "Растущая луна"};
    private static boolean ready;

    private static int ticks, deaths, jumps, lastSlot = -1, lastHurt;
    private static double dist, lx, lz;
    private static boolean hasPos, wasGround = true, wasDead, jumpNow, landNow, hitNow, killNow, killSeen;
    private static ClientWorld lastWorld;
    private static long joinTime = System.currentTimeMillis(), slotTime;
    private static LivingEntity lastTarget;

    private static int frames, curFps, fpsIdx;
    private static long fpsTime, lastFrame;
    private static final int[] FPSH = new int[60];

    private static final int PN = 50;
    private static final float[][] QX = new float[6][PN], QY = new float[6][PN], QV = new float[6][PN];
    private static final int[][] QC = new int[6][PN];
    private static final boolean[] QS = new boolean[6];

    private ExtraFx() {
    }

    private static Module add(Category c, String n, String d) {
        Module m = new Module(n, d, c);
        ModuleManager.MODULES.add(m);
        return m;
    }

    // trig: 0 всегда, 1 движение, 2 спринт, 3 прыжок, 4 приземление, 5 удар по цели, 6 убийство цели, 7 ночь, 8 в воде
    // color: 0 тема, 1 радуга, иначе ARGB; type: 0 цветная пыль, иначе см. pt()
    private static void p(Category c, String n, String d, int shape, int every, int trig, int color, int type) {
        P q = new P();
        q.m = add(c, n, d);
        q.shape = shape;
        q.every = Math.max(1, every);
        q.trig = trig;
        q.color = color;
        q.type = type;
        PS.add(q);
    }

    public static void init() {
        Category V = Category.VISUAL, W = Category.WORLD, S = Category.SECRET;
        // ===== Визуалы (20) =====
        p(V, "Пыль при ходьбе", "Облачка пыли под ногами при ходьбе.", 2, 2, 1, 0xFFBFB6A8, 0);
        p(V, "Салют при прыжке", "Искры фейерверка, когда прыгаете.", 7, 1, 3, 0, 2);
        p(V, "Удар о землю", "Кольцо частиц при приземлении.", 8, 1, 4, 0, 0);
        p(V, "Огонь при спринте", "Пламя из-под ног при беге.", 2, 1, 2, 0, 3);
        p(V, "Нимб темы", "Светящееся кольцо над головой в цвет темы.", 5, 3, 0, 0, 0);
        p(V, "Крылья темы", "Крылья из частиц за спиной.", 6, 2, 0, 0, 0);
        p(V, "Хвост кометы", "Светящийся хвост при движении.", 2, 1, 1, 0, 4);
        p(V, "Тотем при убийстве", "Взрыв тотема, когда цель погибает.", 7, 1, 6, 0, 5);
        p(V, "Конфетти при ударе", "Разноцветное конфетти на цели.", 7, 1, 5, 1, 0);
        p(V, "Сердца при ударе", "Сердечки над целью при ударе.", 7, 1, 5, 0, 6);
        p(V, "Искры крита", "Звёздочки крита на цели при ударе.", 7, 1, 5, 0, 7);
        p(V, "Кольцо под ногами", "Цветной круг вокруг вас.", 8, 3, 0, 0, 0);
        p(V, "Цветной след", "След в цвет темы при ходьбе.", 2, 1, 1, 0, 0);
        p(V, "Двойная спираль", "Две спирали вокруг вас.", 10, 1, 0, 0, 0);
        p(V, "Защитная сфера", "Сфера из частиц вокруг игрока.", 4, 1, 0, 0, 0);
        p(V, "Пентаграмма", "Красная звезда под ногами.", 16, 4, 0, 0xFFFF3B3B, 0);
        p(V, "Корона темы", "Корона из частиц над головой.", 9, 3, 0, 0xFFFFD54A, 0);
        p(V, "Снежная аура", "Снежки кружат вокруг вас.", 4, 2, 0, 0, 8);
        p(V, "Медовые капли", "Капли мёда над головой.", 1, 4, 0, 0, 9);
        p(V, "Тучка с дождём", "Маленький дождик над головой.", 1, 2, 0, 0, 10);

        // ===== Мир (20) =====
        p(W, "Ночные светлячки", "Ночью вокруг летают светлячки.", 11, 2, 7, 0xFFFFF27A, 0);
        p(W, "Падающие листья", "Листья медленно падают вокруг.", 12, 2, 0, 0xFF7BC043, 0);
        p(W, "Снегопад вокруг", "Снежинки падают вокруг вас.", 12, 1, 0, 0, 8);
        p(W, "Пыль в воздухе", "Мелкие частицы парят в воздухе.", 11, 1, 0, 0, 11);
        p(W, "Падающие звёзды", "Светящиеся звёзды падают с неба.", 12, 3, 0, 0, 4);
        p(W, "Пузыри в воде", "Пузыри вокруг, когда вы в воде.", 11, 1, 8, 0, 12);
        p(W, "Огоньки душ", "Голубые огоньки вокруг.", 11, 4, 0, 0, 13);
        p(W, "Конфетти с неба", "Цветной дождь из конфетти.", 12, 1, 0, 1, 0);
        p(W, "Туман у земли", "Низкий туман вокруг.", 13, 2, 0, 0, 1);
        p(W, "Руны в воздухе", "Знаки зачарования вокруг.", 11, 1, 0, 0, 14);
        p(W, "Подводная взвесь", "Частицы как под водой.", 11, 1, 0, 0, 15);
        p(W, "Вулканические искры", "Искры лавы вылетают из земли.", 13, 6, 0, 0, 16);
        p(W, "Капли с неба", "Капли воды падают сверху.", 12, 2, 0, 0, 10);
        p(W, "Кристаллы", "Голубые искры-кристаллы.", 11, 2, 0, 0xFF7FE8FF, 0);
        p(W, "Летающие ноты", "Музыкальные ноты в воздухе.", 11, 5, 0, 0, 17);
        p(W, "Дождь из сердец", "Сердца падают с неба.", 12, 6, 0, 0, 6);
        p(W, "Портальная дымка", "Фиолетовая дымка у земли.", 13, 1, 0, 0, 18);
        p(W, "Дымка костра", "Дым поднимается от земли.", 13, 12, 0, 0, 19);
        p(W, "Ведьмины огни", "Фиолетовые искры вокруг.", 11, 2, 0, 0, 20);
        p(W, "Дым от земли", "Тёмный дым у ног.", 13, 2, 0, 0, 21);

        // ===== Экран (20) =====
        String[][] scr = {
                {"Холодный фильтр", "Голубой оттенок картинки."},
                {"Тёплый фильтр", "Тёплый оранжевый оттенок."},
                {"Ночной фильтр", "Тёмно-синяя ночная атмосфера."},
                {"Розовый фильтр", "Нежный розовый оттенок."},
                {"Мятный фильтр", "Свежий мятный оттенок."},
                {"Тёмная виньетка", "Затемнение краёв экрана."},
                {"Мягкий свет сверху", "Лёгкое свечение сверху."},
                {"Тень снизу", "Затемнение нижней части экрана."},
                {"Закатный фильтр", "Градиент заката на экране."},
                {"Сетка третей", "Сетка для красивых скриншотов."},
                {"Вспышка урона", "Красная вспышка, когда вас бьют."},
                {"Низкое здоровье", "Пульсирующие красные края при малом HP."},
                {"Индикатор голода", "Оранжевые края, когда вы голодны."},
                {"Свечение спринта", "Края светятся при беге."},
                {"Синий край под водой", "Голубые края под водой."},
                {"Огненный край", "Огненные края, когда вы горите."},
                {"Затемнение при приседании", "Лёгкая виньетка на Shift."},
                {"Плавный вход", "Плавное появление картинки при входе в мир."},
                {"Вспышка смены слота", "Лёгкая вспышка при смене предмета."},
                {"Сканер", "Светящаяся полоса бежит по экрану."},
        };
        for (int i = 0; i < 20; i++) {
            SCR[i] = add(Category.SCREEN, scr[i][0], scr[i][1]);
        }

        // ===== HUD (20) =====
        String[][] info = {
                {"Здоровье числом", "Показывает HP цифрами."},
                {"Сытость числом", "Показывает уровень голода."},
                {"Уровень опыта", "Уровень и прогресс опыта."},
                {"Очки брони", "Сколько у вас очков брони."},
                {"Уровень света", "Освещённость в вашей точке."},
                {"Дата", "Сегодняшняя дата."},
                {"Время в игре", "Игровое время суток."},
                {"День в игре", "Номер игрового дня."},
                {"Фаза луны", "Текущая фаза луны."},
                {"Погода", "Ясно, дождь или гроза."},
                {"Измерение", "В каком вы мире."},
                {"Игроки онлайн", "Сколько игроков на сервере."},
                {"Память", "Использование оперативной памяти."},
                {"Счётчик смертей", "Смерти за сессию."},
                {"Счётчик прыжков", "Прыжки за сессию."},
                {"Пройдено блоков", "Пройденное расстояние за сессию."},
                {"Блок под прицелом", "Название блока, на который смотрите."},
                {"Номер чанка", "Координаты текущего чанка."},
                {"Таймер в мире", "Сколько вы уже в этом мире."},
                {"График FPS", "Мини-график FPS."},
        };
        for (int i = 0; i < 20; i++) {
            INFO[i] = add(Category.HUD, info[i][0], info[i][1]);
        }

        // ===== Секрет (+40) =====
        String[] tn = {"Океан", "Лава", "Мята", "Вишня", "Лёд", "Закат", "Неон", "Изумруд", "Полночь", "Карамель"};
        for (int i = 0; i < 10; i++) {
            THEMES[i] = add(S, "Тема «" + tn[i] + "»", "Секретная тема для меню, ClickGUI и HUD.");
        }
        p(S, "Радужное кольцо", "Радужный круг вокруг вас.", 8, 2, 0, 1, 0);
        p(S, "Радужная спираль", "Спираль всех цветов радуги.", 3, 1, 0, 1, 0);
        p(S, "Золотой нимб", "Золотое кольцо над головой.", 5, 3, 0, 0xFFFFD54A, 0);
        p(S, "Королевская корона", "Золотая корона над головой.", 9, 2, 0, 0xFFFFC21A, 0);
        p(S, "Крылья ангела", "Белые крылья за спиной.", 6, 2, 0, 0xFFFFFFFF, 0);
        p(S, "Крылья демона", "Красные крылья за спиной.", 6, 2, 0, 0xFFB0121B, 0);
        p(S, "ДНК", "Радужная двойная спираль.", 10, 1, 0, 1, 0);
        p(S, "Щит-сфера", "Сфера в цвет темы.", 4, 1, 0, 0, 0);
        p(S, "Звезда под ногами", "Радужная звезда под вами.", 16, 3, 0, 1, 0);
        p(S, "Восьмёрка", "Частицы летают восьмёркой.", 18, 1, 0, 1, 0);
        p(S, "Волна", "Волнистое кольцо вокруг.", 17, 2, 0, 0, 0);
        p(S, "Торнадо", "Радужный вихрь вокруг вас.", 15, 1, 0, 1, 0);
        p(S, "Орбиты", "Три кольца как у атома.", 14, 1, 0, 0, 0);
        p(S, "Магический круг", "Фиолетовый круг под ногами.", 8, 2, 0, 0xFFB04CFF, 0);
        p(S, "Радужный след", "Радужный след при ходьбе.", 2, 1, 1, 1, 0);
        String[][] sfx = {
                {"Глитч", "Цифровые помехи на экране."},
                {"Радужная виньетка", "Края экрана переливаются радугой."},
                {"Капли на стекле", "Капли дождя стекают по экрану."},
                {"Падающие сердечки", "Сердечки падают по экрану."},
                {"Конфетти на экране", "Разноцветное конфетти."},
                {"Мерцающие звёзды", "Звёзды мерцают на экране."},
                {"Северное сияние", "Сияние в верхней части экрана."},
                {"Огненный низ", "Языки пламени внизу экрана."},
                {"Голограмма", "Голографические линии."},
                {"Радужный пульс", "Экран мягко пульсирует радугой."},
                {"Блики", "Солнечные блики объектива."},
                {"Синтвейв-сетка", "Неоновая сетка в стиле 80-х."},
                {"Лучи света", "Мягкие лучи сверху."},
                {"Кометы", "Кометы пролетают по экрану."},
                {"Пиксельный дождь", "Быстрый неоновый дождь."},
        };
        for (int i = 0; i < 15; i++) {
            SFX[i] = add(S, sfx[i][0], sfx[i][1]);
        }
        ready = true;
    }

    public static int themeColor(boolean hi) {
        if (!ready) {
            return 0;
        }
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i] != null && THEMES[i].isActive()) {
                return THEME_COL[i][hi ? 0 : 1];
            }
        }
        return 0;
    }

    private static ParticleEffect pt(int t) {
        switch (t) {
            case 1: return ParticleTypes.CLOUD;
            case 2: return ParticleTypes.FIREWORK;
            case 3: return ParticleTypes.FLAME;
            case 4: return ParticleTypes.END_ROD;
            case 5: return ParticleTypes.TOTEM_OF_UNDYING;
            case 6: return ParticleTypes.HEART;
            case 7: return ParticleTypes.CRIT;
            case 8: return ParticleTypes.ITEM_SNOWBALL;
            case 9: return ParticleTypes.DRIPPING_HONEY;
            case 10: return ParticleTypes.DRIPPING_WATER;
            case 11: return ParticleTypes.MYCELIUM;
            case 12: return ParticleTypes.BUBBLE;
            case 13: return ParticleTypes.SOUL;
            case 14: return ParticleTypes.ENCHANT;
            case 15: return ParticleTypes.UNDERWATER;
            case 16: return ParticleTypes.LAVA;
            case 17: return ParticleTypes.NOTE;
            case 18: return ParticleTypes.PORTAL;
            case 19: return ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case 20: return ParticleTypes.WITCH;
            default: return ParticleTypes.SMOKE;
        }
    }

    private static ParticleEffect eff(P q, float off) {
        if (q.type != 0) {
            return pt(q.type);
        }
        int c;
        if (q.color == 0) {
            c = Theme.grad((float) (Math.sin(ticks * 0.1 + off * 6) * 0.5 + 0.5));
        } else if (q.color == 1) {
            c = ModuleManager.rainbow(off);
        } else {
            c = q.color;
        }
        return new DustParticleEffect(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1.0f);
    }

    private static void put(MinecraftClient mc, P q, double x, double y, double z, double vx, double vy, double vz, float off) {
        mc.world.addParticle(eff(q, off), x, y, z, vx, vy, vz);
    }

    public static void tick(MinecraftClient mc) {
        if (!ready || mc.player == null || mc.world == null) {
            return;
        }
        ClientPlayerEntity p = mc.player;
        ticks++;
        long now = System.currentTimeMillis();
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            joinTime = now;
            hasPos = false;
        }
        boolean g = p.isOnGround();
        Vec3d v = p.getVelocity();
        jumpNow = wasGround && !g && v.y > 0.2;
        landNow = !wasGround && g;
        if (jumpNow) {
            jumps++;
        }
        wasGround = g;
        boolean dead = p.isDead() || p.getHealth() <= 0;
        if (dead && !wasDead) {
            deaths++;
        }
        wasDead = dead;
        if (hasPos) {
            double dx = p.getX() - lx, dz = p.getZ() - lz;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 10) {
                dist += d;
            }
        }
        lx = p.getX();
        lz = p.getZ();
        hasPos = true;
        int slot = p.inventory.selectedSlot;
        if (lastSlot != -1 && slot != lastSlot) {
            slotTime = now;
        }
        lastSlot = slot;

        LivingEntity t = ModuleManager.target;
        hitNow = false;
        killNow = false;
        if (t != lastTarget) {
            lastTarget = t;
            lastHurt = t == null ? 0 : t.hurtTime;
            killSeen = false;
        }
        if (t != null) {
            if (t.hurtTime > lastHurt) {
                hitNow = true;
            }
            lastHurt = t.hurtTime;
            if (!killSeen && (t.isDead() || t.getHealth() <= 0)) {
                killNow = true;
                killSeen = true;
            }
        }
        boolean moving = v.x * v.x + v.z * v.z > 0.001;
        long tod = mc.world.getTimeOfDay() % 24000L;
        boolean night = tod > 13000 && tod < 23000;

        for (P q : PS) {
            if (ticks % q.every != 0 || !q.m.isActive()) {
                continue;
            }
            boolean ok;
            LivingEntity c = p;
            switch (q.trig) {
                case 1: ok = moving; break;
                case 2: ok = p.isSprinting(); break;
                case 3: ok = jumpNow; break;
                case 4: ok = landNow; break;
                case 5: ok = hitNow; c = t; break;
                case 6: ok = killNow; c = t; break;
                case 7: ok = night; break;
                case 8: ok = p.isTouchingWater(); break;
                default: ok = true; break;
            }
            if (!ok || c == null) {
                continue;
            }
            try {
                spawn(mc, c, q);
            } catch (Throwable ignored) {
                // частицы не критичны
            }
        }
    }

    private static void spawn(MinecraftClient mc, LivingEntity e, P q) {
        double x = e.getX(), y = e.getY(), z = e.getZ(), hh = e.getHeight();
        double a = ticks * 0.25;
        float o = ticks * 0.02f;
        switch (q.shape) {
            case 0:
                for (int k = 0; k < 2; k++) {
                    double an = a + k * Math.PI;
                    put(mc, q, x + Math.cos(an) * 0.8, y + hh * 0.55, z + Math.sin(an) * 0.8, 0, 0, 0, o + k * 0.5f);
                }
                break;
            case 1:
                put(mc, q, x + (R.nextDouble() - 0.5) * 0.7, y + hh + 0.5, z + (R.nextDouble() - 0.5) * 0.7, 0, 0, 0, o);
                break;
            case 2:
                put(mc, q, x + (R.nextDouble() - 0.5) * 0.4, y + 0.05, z + (R.nextDouble() - 0.5) * 0.4, 0, 0.01, 0, o);
                break;
            case 3: {
                double h = (ticks % 40) / 40.0 * hh;
                put(mc, q, x + Math.cos(a * 2) * 0.7, y + h, z + Math.sin(a * 2) * 0.7, 0, 0, 0, (float) (h / hh));
                break;
            }
            case 4: {
                double u = R.nextDouble() * Math.PI * 2, w = R.nextDouble() * Math.PI;
                put(mc, q, x + Math.cos(u) * Math.sin(w) * 1.2, y + hh * 0.55 + Math.cos(w) * 1.2,
                        z + Math.sin(u) * Math.sin(w) * 1.2, 0, 0, 0, (float) (u / 6.28));
                break;
            }
            case 5:
                for (int k = 0; k < 12; k++) {
                    double an = k * Math.PI / 6 + a * 0.3;
                    put(mc, q, x + Math.cos(an) * 0.35, y + hh + 0.3, z + Math.sin(an) * 0.35, 0, 0, 0, k / 12f);
                }
                break;
            case 6: {
                double yaw = Math.toRadians(e.bodyYaw);
                double fx = -Math.sin(yaw), fz = Math.cos(yaw), sx = Math.cos(yaw), sz = Math.sin(yaw);
                double k = hh / 1.8;
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 1; i <= 7; i++) {
                        double tt = i / 7.0;
                        double flap = Math.sin(ticks * 0.25) * 0.3 * tt;
                        double ox = sx * side * tt * 1.1 - fx * (0.3 + flap);
                        double oz = sz * side * tt * 1.1 - fz * (0.3 + flap);
                        double oy = (1.35 + Math.sin(tt * Math.PI) * 0.35 + tt * 0.25) * k;
                        put(mc, q, x + ox, y + oy, z + oz, 0, 0, 0, (float) tt);
                    }
                }
                break;
            }
            case 7:
                for (int i = 0; i < 12; i++) {
                    put(mc, q, x, y + hh * 0.6, z, (R.nextDouble() - 0.5) * 0.3, R.nextDouble() * 0.25,
                            (R.nextDouble() - 0.5) * 0.3, i / 12f);
                }
                break;
            case 8:
                for (int i = 0; i < 16; i++) {
                    double an = i * Math.PI / 8 + a * 0.2;
                    put(mc, q, x + Math.cos(an) * 1.4, y + 0.05, z + Math.sin(an) * 1.4, 0, 0, 0, i / 16f);
                }
                break;
            case 9:
                for (int i = 0; i < 8; i++) {
                    double an = i * Math.PI / 4 + a * 0.2;
                    double cx = x + Math.cos(an) * 0.35, cz = z + Math.sin(an) * 0.35;
                    put(mc, q, cx, y + hh + 0.15, cz, 0, 0, 0, i / 8f);
                    if (i % 2 == 0) {
                        put(mc, q, cx, y + hh + 0.38, cz, 0, 0, 0, i / 8f);
                    }
                }
                break;
            case 10: {
                double h = (ticks % 40) / 40.0 * hh;
                for (int k = 0; k < 2; k++) {
                    double an = a * 2 + k * Math.PI;
                    put(mc, q, x + Math.cos(an) * 0.6, y + h, z + Math.sin(an) * 0.6, 0, 0, 0, (float) (h / hh) + k * 0.5f);
                }
                break;
            }
            case 11:
                for (int i = 0; i < 2; i++) {
                    put(mc, q, x + (R.nextDouble() - 0.5) * 16, y - 1 + R.nextDouble() * 5, z + (R.nextDouble() - 0.5) * 16,
                            0, 0, 0, R.nextFloat());
                }
                break;
            case 12:
                for (int i = 0; i < 2; i++) {
                    put(mc, q, x + (R.nextDouble() - 0.5) * 16, y + 6 + R.nextDouble() * 3, z + (R.nextDouble() - 0.5) * 16,
                            0, -0.05, 0, R.nextFloat());
                }
                break;
            case 13:
                for (int i = 0; i < 2; i++) {
                    put(mc, q, x + (R.nextDouble() - 0.5) * 12, y + 0.1, z + (R.nextDouble() - 0.5) * 12,
                            0, 0.01, 0, R.nextFloat());
                }
                break;
            case 14:
                for (int k = 0; k < 3; k++) {
                    double tk = k * Math.PI / 3;
                    double an = a * 1.5 + k;
                    double px = Math.cos(an), py = Math.sin(an) * Math.cos(tk), pz = Math.sin(an) * Math.sin(tk);
                    put(mc, q, x + px, y + hh * 0.55 + py, z + pz, 0, 0, 0, k / 3f);
                }
                break;
            case 15: {
                double h = (ticks % 40) / 40.0 * 2.5;
                double r = 0.3 + h * 0.5;
                for (int k = 0; k < 3; k++) {
                    double an = a * 2 + k * Math.PI * 2 / 3;
                    put(mc, q, x + Math.cos(an) * r, y + h, z + Math.sin(an) * r, 0, 0, 0, (float) (h / 2.5));
                }
                break;
            }
            case 16: {
                double[] vx = new double[11], vz = new double[11];
                for (int k = 0; k <= 10; k++) {
                    double an = -Math.PI / 2 + k * Math.PI / 5 + ticks * 0.02;
                    double r = k % 2 == 0 ? 1.3 : 0.5;
                    vx[k] = Math.cos(an) * r;
                    vz[k] = Math.sin(an) * r;
                }
                for (int k = 0; k < 10; k++) {
                    for (int s = 0; s < 3; s++) {
                        double tt = s / 3.0;
                        put(mc, q, x + vx[k] + (vx[k + 1] - vx[k]) * tt, y + 0.05, z + vz[k] + (vz[k + 1] - vz[k]) * tt,
                                0, 0, 0, k / 10f);
                    }
                }
                break;
            }
            case 17:
                for (int i = 0; i < 16; i++) {
                    double an = i * Math.PI / 8;
                    put(mc, q, x + Math.cos(an), y + hh * 0.45 + Math.sin(an * 3 + ticks * 0.2) * 0.25, z + Math.sin(an),
                            0, 0, 0, i / 16f);
                }
                break;
            default: {
                double tt = ticks * 0.15;
                put(mc, q, x + Math.sin(tt) * 1.1, y + hh * 0.55, z + Math.sin(tt) * Math.cos(tt) * 1.1, 0, 0, 0, o);
                break;
            }
        }
    }

    private static boolean on(Module m) {
        return m != null && m.isActive();
    }

    public static void render2D(MatrixStack ms) {
        if (!ready) {
            return;
        }
        try {
            draw(ms);
        } catch (Throwable ignored) {
            // не критично
        }
    }

    private static void edges(MatrixStack ms, int w, int h, int c, int a, int s) {
        a = Math.max(0, Math.min(255, a));
        int c1 = Theme.alpha(c, a), c0 = Theme.alpha(c, 0);
        Gfx.vgrad(ms, 0, 0, w, s, c1, c0);
        Gfx.vgrad(ms, 0, h - s, w, h, c0, c1);
        Gfx.hgrad(ms, 0, 0, s, h, c1, c0);
        Gfx.hgrad(ms, w - s, 0, w, h, c0, c1);
    }

    private static void draw(MatrixStack ms) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        frames++;
        if (fpsTime == 0) {
            fpsTime = now;
        }
        if (now - fpsTime >= 500) {
            curFps = (int) (frames * 1000L / (now - fpsTime));
            frames = 0;
            fpsTime = now;
            FPSH[fpsIdx] = curFps;
            fpsIdx = (fpsIdx + 1) % FPSH.length;
        }
        float dt = lastFrame == 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            return;
        }
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        float wave = (float) (Math.sin(now / 400.0) * 0.5 + 0.5);

        // ---------- Экран ----------
        if (on(SCR[0])) Gfx.rect(ms, 0, 0, w, h, 0x1A2E6BFF);
        if (on(SCR[1])) Gfx.rect(ms, 0, 0, w, h, 0x1AFF9A2E);
        if (on(SCR[2])) Gfx.rect(ms, 0, 0, w, h, 0x30000A2A);
        if (on(SCR[3])) Gfx.rect(ms, 0, 0, w, h, 0x1AFF5FA8);
        if (on(SCR[4])) Gfx.rect(ms, 0, 0, w, h, 0x1A3DFFB0);
        if (on(SCR[5])) edges(ms, w, h, 0xFF000000, 0x90, 60);
        if (on(SCR[6])) Gfx.vgrad(ms, 0, 0, w, h / 3, 0x30FFFFFF, 0x00FFFFFF);
        if (on(SCR[7])) Gfx.vgrad(ms, 0, h * 2 / 3, w, h, 0x00000000, 0x60000000);
        if (on(SCR[8])) Gfx.vgrad(ms, 0, 0, w, h, 0x22FF8A3D, 0x228A3DFF);
        if (on(SCR[9])) {
            Gfx.rect(ms, w / 3, 0, w / 3 + 1, h, 0x40FFFFFF);
            Gfx.rect(ms, w * 2 / 3, 0, w * 2 / 3 + 1, h, 0x40FFFFFF);
            Gfx.rect(ms, 0, h / 3, w, h / 3 + 1, 0x40FFFFFF);
            Gfx.rect(ms, 0, h * 2 / 3, w, h * 2 / 3 + 1, 0x40FFFFFF);
        }
        if (on(SCR[10]) && p.hurtTime > 0) Gfx.rect(ms, 0, 0, w, h, Theme.alpha(0xFFFF2020, p.hurtTime * 8));
        if (on(SCR[11]) && p.getHealth() <= 6) edges(ms, w, h, 0xFFFF1010, (int) (0x40 + wave * 0x50), 50);
        if (on(SCR[12]) && p.getHungerManager().getFoodLevel() <= 6) edges(ms, w, h, 0xFFFF8A00, 0x60, 40);
        if (on(SCR[13]) && p.isSprinting()) edges(ms, w, h, Theme.hi(), 0x50, 30);
        if (on(SCR[14]) && p.isSubmergedInWater()) edges(ms, w, h, 0xFF2E7BFF, 0x70, 50);
        if (on(SCR[15]) && p.isOnFire()) {
            edges(ms, w, h, 0xFFFF6A00, 0x80, 45);
            Gfx.vgrad(ms, 0, h * 3 / 4, w, h, 0x00FF3A00, Theme.alpha(0xFFFF3A00, (int) (0x40 + wave * 0x30)));
        }
        if (on(SCR[16]) && p.isSneaking()) edges(ms, w, h, 0xFF000000, 0x50, 50);
        if (on(SCR[17]) && now - joinTime < 1500) {
            Gfx.rect(ms, 0, 0, w, h, Theme.alpha(0xFF000000, (int) (255 * (1f - (now - joinTime) / 1500f))));
        }
        if (on(SCR[18]) && now - slotTime < 250) {
            Gfx.rect(ms, 0, 0, w, h, Theme.alpha(0xFFFFFFFF, (int) (0x30 * (1f - (now - slotTime) / 250f))));
        }
        if (on(SCR[19])) {
            int y = (int) ((now / 6) % (h + 20));
            Gfx.vgrad(ms, 0, y - 14, w, y, Theme.alpha(Theme.hi(), 0), Theme.alpha(Theme.hi(), 0x40));
            Gfx.rect(ms, 0, y, w, y + 1, Theme.alpha(Theme.hi(), 0x90));
        }

        // ---------- Секрет ----------
        if (Secret.unlocked()) {
            secret(ms, mc, w, h, now, dt, wave);
        }

        // ---------- HUD ----------
        if (!mc.options.hudHidden && !mc.options.debugEnabled) {
            info(ms, mc, p, w, h, now);
        }
    }

    private static void secret(MatrixStack ms, MinecraftClient mc, int w, int h, long now, float dt, float wave) {
        if (on(SFX[0]) && R.nextFloat() < 0.25f) {
            for (int i = 0; i < 3; i++) {
                int y = R.nextInt(Math.max(1, h)), hh = 1 + R.nextInt(4), off = R.nextInt(30) - 15;
                Gfx.rect(ms, Math.max(0, off), y, Math.min(w, w + off), y + hh, i % 2 == 0 ? 0x4000FFFF : 0x40FF00FF);
            }
        }
        if (on(SFX[1])) edges(ms, w, h, ModuleManager.rainbow(0f), 0x70, 40);
        if (on(SFX[2])) pool(ms, mc, 0, 0, w, h, dt, now);
        if (on(SFX[3])) pool(ms, mc, 1, 1, w, h, dt, now);
        if (on(SFX[4])) pool(ms, mc, 2, 2, w, h, dt, now);
        if (on(SFX[5])) pool(ms, mc, 3, 3, w, h, dt, now);
        if (on(SFX[6])) {
            for (int x = 0; x < w; x += 4) {
                float t = x / (float) w;
                int yb = (int) (h * 0.18 + Math.sin(t * 6 + now / 900.0) * h * 0.05);
                int bh = (int) (30 + Math.sin(t * 10 + now / 600.0) * 10);
                int c = Theme.lerp(0xFF39FF9A, 0xFF8A5CFF, (float) (Math.sin(t * 4 + now / 1500.0) * 0.5 + 0.5));
                Gfx.vgrad(ms, x, yb - bh, x + 4, yb, Theme.alpha(c, 0), Theme.alpha(c, 0x50));
                Gfx.vgrad(ms, x, yb, x + 4, yb + 10, Theme.alpha(c, 0x50), Theme.alpha(c, 0));
            }
        }
        if (on(SFX[7])) {
            for (int x = 0; x < w; x += 4) {
                int hh = (int) (20 + (Math.sin(x * 0.3 + now / 120.0) + Math.sin(x * 0.13 + now / 230.0)) * 8 + R.nextInt(4));
                Gfx.vgrad(ms, x, h - hh, x + 4, h, 0x00FF6A00, 0x90FF3A00);
            }
        }
        if (on(SFX[8])) {
            Gfx.rect(ms, 0, 0, w, h, 0x1000E5FF);
            int off = (int) ((now / 40) % 4);
            for (int y = off; y < h; y += 4) {
                Gfx.rect(ms, 0, y, w, y + 1, 0x1200E5FF);
            }
            int by = (int) ((now / 8) % (h + 40)) - 20;
            Gfx.vgrad(ms, 0, by, w, by + 20, 0x0000E5FF, 0x3000E5FF);
        }
        if (on(SFX[9])) Gfx.rect(ms, 0, 0, w, h, Theme.alpha(ModuleManager.rainbow(0f), (int) (0x20 * wave)));
        if (on(SFX[10])) {
            int sx = (int) (w * 0.82), sy = (int) (h * 0.12);
            Gfx.circle(ms, sx, sy, 22, 0x30FFF2C0);
            Gfx.circle(ms, sx, sy, 10, 0x50FFFFFF);
            int[] cols = {0x22FFE0A0, 0x22A0E0FF, 0x22FFA0E0, 0x22A0FFC0, 0x22FFFFFF, 0x22C0A0FF};
            for (int i = 0; i < 6; i++) {
                float t = 0.35f + i * 0.3f;
                int cx = (int) (sx + (w / 2f - sx) * t), cy = (int) (sy + (h / 2f - sy) * t);
                Gfx.circle(ms, cx, cy, 5 + (i * 7) % 20, cols[i]);
            }
        }
        if (on(SFX[11])) {
            int y0 = (int) (h * 0.65);
            Gfx.vgrad(ms, 0, y0 - 20, w, y0, 0x00FF2BD6, 0x40FF2BD6);
            float sh = (now % 1000) / 1000f;
            for (int j = 0; j < 10; j++) {
                float t = (j + sh) / 10f;
                int y = (int) (y0 + (h - y0) * t * t);
                Gfx.rect(ms, 0, y, w, y + 1, 0x60FF2BD6);
            }
            for (int i = -12; i <= 12; i++) {
                float bx = w / 2f + i * (w / 10f);
                for (int s = 0; s < 20; s++) {
                    float t = s / 20f;
                    int px = (int) (w / 2f + (bx - w / 2f) * t), py = (int) (y0 + (h - y0) * t);
                    Gfx.rect(ms, px, py, px + 1, py + (h - y0) / 20 + 1, 0x5000E5FF);
                }
            }
        }
        if (on(SFX[12])) {
            for (int i = 0; i < 7; i++) {
                int x = (int) (w * (0.1 + 0.13 * i) + Math.sin(now / 2000.0 + i) * 20);
                int wd = 18 + (i % 3) * 8;
                Gfx.vgrad(ms, x - wd / 2, 0, x + wd / 2, (int) (h * 0.7), 0x28FFF8D0, 0x00FFF8D0);
            }
        }
        if (on(SFX[13])) pool(ms, mc, 4, 4, w, h, dt, now);
        if (on(SFX[14])) pool(ms, mc, 5, 5, w, h, dt, now);
    }

    private static void seed(int k, int i, int w, int h, boolean top) {
        QX[k][i] = R.nextFloat() * w;
        QY[k][i] = top ? -R.nextFloat() * 40 : R.nextFloat() * h;
        switch (k) {
            case 0: QV[k][i] = 8 + R.nextFloat() * 14; break;
            case 1: QV[k][i] = 25 + R.nextFloat() * 35; break;
            case 2: QV[k][i] = 30 + R.nextFloat() * 50; break;
            case 3: QV[k][i] = R.nextFloat() * 6.28f; break;
            case 4:
                QV[k][i] = 220 + R.nextFloat() * 200;
                if (top) {
                    QX[k][i] = -R.nextFloat() * w;
                    QY[k][i] = R.nextFloat() * h * 0.6f - 40;
                }
                break;
            default: QV[k][i] = 200 + R.nextFloat() * 150; break;
        }
        QC[k][i] = ModuleManager.rainbow(R.nextFloat());
    }

    private static void pool(MatrixStack ms, MinecraftClient mc, int k, int kind, int w, int h, float dt, long now) {
        if (!QS[k]) {
            for (int i = 0; i < PN; i++) {
                seed(k, i, w, h, false);
            }
            QS[k] = true;
        }
        int n = kind == 4 ? 6 : kind == 3 ? PN : 40;
        for (int i = 0; i < n; i++) {
            float x = QX[k][i], y = QY[k][i];
            int ix = (int) x, iy = (int) y;
            switch (kind) {
                case 0:
                    QY[k][i] += QV[k][i] * dt;
                    Gfx.round(ms, ix, iy, ix + 3, iy + 5, 1, 0x5099CCFF);
                    Gfx.rect(ms, ix + 1, iy + 1, ix + 2, iy + 2, 0x80FFFFFF);
                    break;
                case 1:
                    QY[k][i] += QV[k][i] * dt;
                    QX[k][i] += (float) Math.sin(now / 500.0 + i) * 0.3f;
                    mc.textRenderer.drawWithShadow(ms, "\u2665", x, y, i % 2 == 0 ? 0xFFFF4F8B : 0xFFFF8FB1);
                    break;
                case 2: {
                    QY[k][i] += QV[k][i] * dt;
                    QX[k][i] += (float) Math.sin(y / 20.0 + i) * 0.5f;
                    boolean flip = ((now / 150) + i) % 2 == 0;
                    Gfx.rect(ms, ix, iy, ix + (flip ? 3 : 2), iy + (flip ? 2 : 3), QC[k][i]);
                    break;
                }
                case 3: {
                    int a = (int) ((Math.sin(now / 300.0 + QV[k][i]) * 0.5 + 0.5) * 200);
                    int s = i % 5 == 0 ? 2 : 1;
                    Gfx.rect(ms, ix, iy, ix + s, iy + s, Theme.alpha(0xFFFFFFFF, a));
                    break;
                }
                case 4: {
                    QX[k][i] += QV[k][i] * dt;
                    QY[k][i] += QV[k][i] * 0.5f * dt;
                    for (int s = 0; s < 10; s++) {
                        int sx = ix - s * 4, sy = iy - s * 2;
                        Gfx.rect(ms, sx, sy, sx + 3, sy + 2, Theme.alpha(QC[k][i], 220 - s * 22));
                    }
                    break;
                }
                default:
                    QY[k][i] += QV[k][i] * dt;
                    Gfx.rect(ms, ix, iy, ix + 1, iy + 8, 0x9040A0FF);
                    break;
            }
            if (kind != 3 && (QY[k][i] > h + 10 || QX[k][i] > w + 60)) {
                seed(k, i, w, h, true);
            }
        }
    }

    private static String two(long v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    private static void info(MatrixStack ms, MinecraftClient mc, ClientPlayerEntity p, int w, int h, long now) {
        List<String[]> L = new ArrayList<>();
        if (on(INFO[0])) L.add(new String[]{"HP", String.format(Locale.ROOT, "%.1f/%.0f", p.getHealth(), p.getMaxHealth())});
        if (on(INFO[1])) L.add(new String[]{"Сытость", p.getHungerManager().getFoodLevel() + "/20"});
        if (on(INFO[2])) L.add(new String[]{"Опыт", p.experienceLevel + " ур. (" + (int) (p.experienceProgress * 100) + "%)"});
        if (on(INFO[3])) L.add(new String[]{"Броня", String.valueOf(p.getArmor())});
        if (on(INFO[4])) L.add(new String[]{"Свет", String.valueOf(mc.world.getLightLevel(p.getBlockPos()))});
        if (on(INFO[5])) L.add(new String[]{"Дата", new SimpleDateFormat("dd.MM.yyyy").format(new Date())});
        long tod = mc.world.getTimeOfDay();
        if (on(INFO[6])) {
            long t = tod % 24000L;
            L.add(new String[]{"Время", two((t / 1000 + 6) % 24) + ":" + two((t % 1000) * 60 / 1000)});
        }
        if (on(INFO[7])) L.add(new String[]{"День", String.valueOf(tod / 24000L + 1)});
        if (on(INFO[8])) L.add(new String[]{"Луна", MOON[(int) ((tod / 24000L) % 8)]});
        if (on(INFO[9])) L.add(new String[]{"Погода", mc.world.isThundering() ? "Гроза" : mc.world.isRaining() ? "Дождь" : "Ясно"});
        if (on(INFO[10])) {
            String d = mc.world.getRegistryKey().getValue().getPath();
            if ("overworld".equals(d)) d = "Обычный мир";
            else if ("the_nether".equals(d)) d = "Незер";
            else if ("the_end".equals(d)) d = "Энд";
            L.add(new String[]{"Мир", d});
        }
        if (on(INFO[11])) {
            int n = mc.getNetworkHandler() == null ? 1 : mc.getNetworkHandler().getPlayerList().size();
            L.add(new String[]{"Онлайн", String.valueOf(n)});
        }
        if (on(INFO[12])) {
            Runtime rt = Runtime.getRuntime();
            L.add(new String[]{"ОЗУ", ((rt.totalMemory() - rt.freeMemory()) >> 20) + "/" + (rt.maxMemory() >> 20) + " МБ"});
        }
        if (on(INFO[13])) L.add(new String[]{"Смерти", String.valueOf(deaths)});
        if (on(INFO[14])) L.add(new String[]{"Прыжки", String.valueOf(jumps)});
        if (on(INFO[15])) L.add(new String[]{"Пройдено", (long) dist + " бл."});
        if (on(INFO[16])) {
            String b = "-";
            HitResult hr = mc.crosshairTarget;
            if (hr != null && hr.getType() == HitResult.Type.BLOCK) {
                BlockPos bp = ((BlockHitResult) hr).getBlockPos();
                b = mc.world.getBlockState(bp).getBlock().getName().getString();
            }
            L.add(new String[]{"Блок", b});
        }
        if (on(INFO[17])) {
            L.add(new String[]{"Чанк", ((int) Math.floor(p.getX()) >> 4) + ", " + ((int) Math.floor(p.getZ()) >> 4)});
        }
        if (on(INFO[18])) {
            long s = (now - joinTime) / 1000;
            L.add(new String[]{"В мире", (s >= 3600 ? (s / 3600) + ":" : "") + two((s / 60) % 60) + ":" + two(s % 60)});
        }
        boolean graph = on(INFO[19]);
        if (L.isEmpty() && !graph) {
            return;
        }
        int lw = 0;
        for (String[] s : L) {
            lw = Math.max(lw, Fonts.width(s[0] + ": ") + Fonts.width(s[1]));
        }
        int pw = Math.max(lw + 12, graph ? 74 : 0);
        int lh = 11;
        int ph = L.size() * lh + 6 + (graph ? 36 : 0);
        int x = 4, y = h / 2 - ph / 2;
        Gfx.round(ms, x, y, x + pw, y + ph, 4, 0x90101018);
        Gfx.rect(ms, x, y + 3, x + 2, y + ph - 3, Theme.hi());
        int ty = y + 4;
        for (String[] s : L) {
            String lab = s[0] + ": ";
            Fonts.shadow(ms, lab, x + 7, ty, Theme.hi());
            Fonts.shadow(ms, s[1], x + 7 + Fonts.width(lab), ty, 0xFFFFFFFF);
            ty += lh;
        }
        if (graph) {
            Fonts.shadow(ms, "FPS: ", x + 7, ty, Theme.hi());
            Fonts.shadow(ms, String.valueOf(curFps), x + 7 + Fonts.width("FPS: "), ty, 0xFFFFFFFF);
            int max = 60;
            for (int v : FPSH) {
                max = Math.max(max, v);
            }
            int base = ty + 33;
            for (int k = 0; k < FPSH.length; k++) {
                int v = FPSH[(fpsIdx + k) % FPSH.length];
                int bh = Math.max(1, Math.min(22, v * 22 / max));
                Gfx.rect(ms, x + 7 + k, base - bh, x + 8 + k, base, Theme.grad(k / 60f));
            }
        }
    }
}
