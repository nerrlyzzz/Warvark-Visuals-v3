package dev.warax.visuals.gui;

import dev.warax.visuals.module.Account;
import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Общая основа всех меню Warax в стиле Pulse: живой фон (аврора + звёзды),
 * стеклянная боковая панель навигации, шапка с часами и плавное появление контента.
 */
public abstract class WaraxBaseScreen extends Screen {
    protected static final String[] NAV = {"Главная", "Одиночная игра", "Сетевая игра", "Настройки", "Выход"};
    protected static final String[] NAV_I = {"◆", "▶", "✦", "⚙", "✕"};
    protected static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    // общий фон: частицы живут между экранами, переходы выглядят бесшовно
    private static final int STARS = 90;
    private static float[] sx, sy, svx, svy, ssz, sph;
    private static final long T0 = System.currentTimeMillis();

    protected final long openTime = System.currentTimeMillis();
    private final float[] navHover = new float[NAV.length];
    private static float navSel = -1;

    protected WaraxBaseScreen(String title) {
        super(new LiteralText(title));
    }

    /** Пункт навигации, подсвеченный на этом экране. */
    protected abstract int navIndex();

    protected abstract String header();

    protected abstract String subHeader();

    /** Отрисовка содержимого в прямоугольнике (x, y, w, h). */
    protected abstract void content(MatrixStack ms, int mx, int my, float delta, int x, int y, int w, int h);

    protected boolean contentClick(double mx, double my, int button) {
        return false;
    }

    // ---------- геометрия ----------
    protected int sideW() {
        return width < 460 ? 108 : 132;
    }

    protected int cX() {
        return sideW() + 14;
    }

    protected int cY() {
        return 40;
    }

    protected int cW() {
        return width - sideW() - 26;
    }

    protected int cH() {
        return height - cY() - 18;
    }

    protected float intro() {
        float t = Math.min(1f, (System.currentTimeMillis() - openTime) / 380f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    protected static float time() {
        return (System.currentTimeMillis() - T0) / 1000f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        client.openScreen(new WaraxTitleScreen());
    }

    // ---------- фон ----------
    public static void background(MatrixStack ms, int w, int h) {
        float t = time();
        fillGradient(ms, 0, 0, w, h, Theme.bgTop(), Theme.bgBottom(), 0);
        // аврора: три плавающих пятна света
        int[][] blobs = {
                {(int) (w * (0.22 + 0.10 * Math.sin(t * 0.31))), (int) (h * (0.28 + 0.10 * Math.cos(t * 0.27))), Theme.a1()},
                {(int) (w * (0.78 + 0.09 * Math.cos(t * 0.23))), (int) (h * (0.72 + 0.10 * Math.sin(t * 0.29))), Theme.hi()},
                {(int) (w * (0.55 + 0.18 * Math.sin(t * 0.17))), (int) (h * (0.15 + 0.06 * Math.sin(t * 0.41))), Theme.lerp(Theme.a1(), Theme.hi(), 0.5f)}
        };
        for (int[] b : blobs) {
            for (int k = 0; k < 6; k++) {
                Gfx.circle(ms, b[0], b[1], 150 - k * 22, 4, Theme.alpha(b[2], 0x09));
            }
        }
        // тонкая сетка
        int grid = 32;
        int off = (int) ((t * 6) % grid);
        for (int x = -grid + off; x < w; x += grid) {
            fillRaw(ms, x, 0, x + 1, h, 0x06FFFFFF);
        }
        for (int y = -grid + off; y < h; y += grid) {
            fillRaw(ms, 0, y, w, y + 1, 0x06FFFFFF);
        }
        // звёзды
        if (sx == null) {
            Random r = new Random();
            sx = new float[STARS]; sy = new float[STARS]; svx = new float[STARS];
            svy = new float[STARS]; ssz = new float[STARS]; sph = new float[STARS];
            for (int i = 0; i < STARS; i++) {
                sx[i] = r.nextFloat() * 2000; sy[i] = r.nextFloat() * 1200;
                svx[i] = (r.nextFloat() - 0.5f) * 0.25f; svy[i] = -0.08f - r.nextFloat() * 0.3f;
                ssz[i] = 1 + r.nextInt(3); sph[i] = r.nextFloat() * 10;
            }
        }
        for (int i = 0; i < STARS; i++) {
            sx[i] += svx[i];
            sy[i] += svy[i];
            float px = ((sx[i] % w) + w) % w, py = ((sy[i] % h) + h) % h;
            int a = (int) (0x25 + 0x70 * (0.5 + 0.5 * Math.sin(t * 1.6 + sph[i])));
            int s = (int) ssz[i];
            int col = i % 3 == 0 ? Theme.hi() : (i % 3 == 1 ? Theme.a1() : 0xFFFFFFFF);
            if (s >= 3) {
                Gfx.circle(ms, (int) px, (int) py, 3, Theme.alpha(col, a / 5));
            }
            fillRaw(ms, (int) px, (int) py, (int) px + Math.min(2, s), (int) py + Math.min(2, s), Theme.alpha(col, a));
        }
        // виньетка
        fillGradient(ms, 0, 0, w, h / 4, 0x50000000, 0x00000000, 0);
        fillGradient(ms, 0, h - h / 3, w, h, 0x00000000, 0x80000000, 0);
    }

    private static void fillRaw(MatrixStack ms, int x1, int y1, int x2, int y2, int c) {
        fill(ms, x1, y1, x2, y2, c);
    }

    protected static void fillGradient(MatrixStack ms, int x1, int y1, int x2, int y2, int c1, int c2, int z) {
        Gfx.vgrad(ms, x1, y1, x2, y2, c1, c2);
    }

    // ---------- рендер ----------
    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        background(ms, width, height);
        Blur.render(delta);
        fill(ms, 0, 0, width, height, 0x18000000);
        float in = intro();

        sidebar(ms, mx, my, in);

        // шапка
        int hx = cX() + (int) ((1 - in) * 16);
        ms.push();
        ms.scale(1.6f, 1.6f, 1f);
        Fonts.shadow(ms, header(), hx / 1.6f, 11 / 1.6f, Theme.alpha(0xFFFFFFFF, Math.max(8, (int) (255 * in))));
        ms.pop();
        Fonts.draw(ms, subHeader(), hx, 27, Theme.alpha(0xFF8A8A9C, Math.max(8, (int) (255 * in))));
        String clock = LocalTime.now().format(CLOCK);
        int cw = Fonts.width(clock) + 18;
        int clx = width - 12 - cw;
        Gfx.glass(ms, clx, 10, clx + cw, 26, 8, 0x50000000);
        Gfx.circle(ms, clx + 8, 18, 2, Theme.grad(Gfx.wave(0)));
        Fonts.shadow(ms, clock, clx + 13, 14, 0xFFE4E4EC);
        Gfx.hgradA(ms, cX(), 36, cX() + cW(), 37, 0x00FFFFFF, Theme.alpha(Theme.hi(), 0x70));

        content(ms, mx, my, delta, cX(), cY() + (int) ((1 - in) * 14), cW(), cH());

        Fonts.draw(ms, "Warax Visuals • 1.16.5", cX(), height - 12, 0xFF5E5E70);
        super.render(ms, mx, my, delta);
    }

    private void sidebar(MatrixStack ms, int mx, int my, float in) {
        int sw = sideW();
        int x0 = 6 - (int) ((1 - in) * 40), y0 = 6, x1 = x0 + sw, y1 = height - 6;
        Gfx.card(ms, x0, y0, x1, y1, 10, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0xB0 : 0xF0), 0.4f + 0.3f * Gfx.wave(0));
        // логотип
        int lg = Theme.grad(Gfx.wave(0f));
        Gfx.glow(ms, x0 + 10, y0 + 10, x0 + 30, y0 + 30, 6, lg, 3 + Gfx.glowLevel(), 0xA0);
        Gfx.round(ms, x0 + 10, y0 + 10, x0 + 30, y0 + 30, 6, lg);
        Gfx.vgrad(ms, x0 + 12, y0 + 11, x0 + 28, y0 + 19, 0x30FFFFFF, 0x00FFFFFF);
        Fonts.shadow(ms, "W", x0 + 20 - Fonts.width("W") / 2f, y0 + 16, 0xFFFFFFFF);
        Gfx.gradText(ms, textRenderer, "Warax", x0 + 36, y0 + 11);
        Fonts.draw(ms, "visuals client", x0 + 36, y0 + 21, 0xFF6E6E80);
        Gfx.hgradA(ms, x0 + 10, y0 + 38, x1 - 10, y0 + 39, 0x00FFFFFF, 0x30FFFFFF);

        // навигация с плавающим индикатором
        int sel = navIndex();
        navSel = navSel < 0 ? sel : Gfx.approach(navSel, sel, 0.25f);
        int iy = (int) (navY(0) + navSel * 24);
        Gfx.glow(ms, x0 + 8, iy, x1 - 8, iy + 20, 6, Theme.a1(), 2 + Gfx.glowLevel(), 0x70);
        Gfx.hgrad(ms, x0 + 8, iy, x1 - 8, iy + 20, Theme.alpha(Theme.a1(), 0x70), Theme.alpha(Theme.hi(), 0x18));
        Gfx.round(ms, x0 + 8, iy + 5, x0 + 10, iy + 15, 1, Theme.hi());
        for (int i = 0; i < NAV.length; i++) {
            int y = navY(i);
            boolean hov = Gfx.in(mx, my, x0 + 8, y, sw - 16, 20);
            navHover[i] = Gfx.approach(navHover[i], hov ? 1f : 0f, 0.3f);
            float h = navHover[i];
            boolean s = i == sel;
            if (h > 0.02f && !s) {
                Gfx.round(ms, x0 + 8, y, x1 - 8, y + 20, 6, Theme.alpha(i == 4 ? 0xFFE5303A : 0xFFFFFFFF, (int) (0x1C * h + (i == 4 ? 0x20 * h : 0))));
            }
            int off = (int) (h * 3);
            int ic = s ? Theme.hi() : Theme.lerp(0xFF6E6E80, i == 4 ? 0xFFFF6B6B : Theme.hi(), h);
            Fonts.shadow(ms, NAV_I[i], x0 + 16 + off, y + 6, ic);
            Fonts.shadow(ms, NAV[i], x0 + 30 + off, y + 6, s ? 0xFFFFFFFF : Theme.lerp(0xFFA0A0B0, 0xFFFFFFFF, h));
        }

        // карточка аккаунта внизу
        int ay = y1 - 34;
        Gfx.glass(ms, x0 + 6, ay, x1 - 6, y1 - 6, 7, 0x40000000);
        String nick = client.getSession().getUsername();
        int av = Theme.grad(Gfx.wave(1f));
        Gfx.circle(ms, x0 + 20, ay + 14, 8, av);
        Gfx.circle(ms, x0 + 20, ay + 14, 6, Theme.alpha(0xFF000000, 0x50));
        String ini = nick.isEmpty() ? "?" : nick.substring(0, 1).toUpperCase();
        Fonts.shadow(ms, ini, x0 + 20 - Fonts.width(ini) / 2f, ay + 10, 0xFFFFFFFF);
        Fonts.shadow(ms, Fonts.trim(nick, sw - 50), x0 + 32, ay + 6, 0xFFFFFFFF);
        Gfx.circle(ms, x0 + 34, ay + 20, 2, 0xFF3BE37A);
        Fonts.draw(ms, Fonts.trim(Account.login(), sw - 56), x0 + 39, ay + 16, 0xFF7E7E90);
    }

    protected int navY(int i) {
        return 54 + i * 24;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < NAV.length; i++) {
                if (Gfx.in(mx, my, 14, navY(i), sideW() - 16, 20)) {
                    go(i);
                    return true;
                }
            }
        }
        if (contentClick(mx, my, button)) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    protected void go(int i) {
        if (i == navIndex()) {
            return;
        }
        switch (i) {
            case 0: client.openScreen(new WaraxTitleScreen()); break;
            case 1: client.openScreen(new WaraxWorldsScreen()); break;
            case 2: client.openScreen(new WaraxServersScreen()); break;
            case 3: client.openScreen(new WaraxOptionsScreen()); break;
            default: client.scheduleStop(); break;
        }
    }

    // ---------- общие элементы ----------

    /** Кнопка в стиле Pulse. primary — залита градиентом темы. */
    protected static void button(MatrixStack ms, int x, int y, int w, int h, String icon, String text, boolean hov, boolean primary) {
        if (primary) {
            Gfx.glow(ms, x, y, x + w, y + h, 7, Theme.a1(), 2 + Gfx.glowLevel() * (hov ? 2 : 1), hov ? 0xB0 : 0x60);
            Gfx.round(ms, x, y, x + w, y + h, 7, Theme.grad(Gfx.wave(0)));
            Gfx.hgrad(ms, x + 3, y + 1, x + w - 3, y + h - 1, Theme.alpha(Theme.hi(), hov ? 0xFF : 0xE0), Theme.alpha(Theme.a1(), hov ? 0xFF : 0xE0));
            Gfx.vgrad(ms, x + 3, y + 1, x + w - 3, y + h / 2, 0x30FFFFFF, 0x00FFFFFF);
        } else {
            if (hov) {
                Gfx.glow(ms, x, y, x + w, y + h, 7, Theme.a1(), 2 + Gfx.glowLevel(), 0x60);
            }
            Gfx.glass(ms, x, y, x + w, y + h, 7, hov ? Theme.alpha(Theme.a1(), 0x55) : 0x40000000);
        }
        int tw = Fonts.width(text) + (icon == null ? 0 : Fonts.width(icon) + 5);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - 8) / 2;
        if (icon != null) {
            Fonts.shadow(ms, icon, tx, ty, primary ? 0xFFFFFFFF : (hov ? Theme.hi() : 0xFF9A9AAA));
            tx += Fonts.width(icon) + 5;
        }
        Fonts.shadow(ms, text, tx, ty, 0xFFFFFFFF);
    }

    /** Текстовое поле. */
    protected static void field(MatrixStack ms, int x, int y, int w, int h, String icon, String value, String hint, boolean focus) {
        if (focus) {
            Gfx.glow(ms, x, y, x + w, y + h, 6, Theme.a1(), 2 + Gfx.glowLevel(), 0x70);
        }
        Gfx.glass(ms, x, y, x + w, y + h, 6, focus ? Theme.alpha(Theme.a1(), 0x40) : 0x40000000);
        if (focus) {
            Gfx.hgrad(ms, x + 6, y + h - 1, x + w - 6, y + h, Theme.hi(), Theme.a1());
        }
        int tx = x + 7;
        if (icon != null) {
            Fonts.draw(ms, icon, tx, y + (h - 8) / 2f, 0xFF8A8A9C);
            tx += Fonts.width(icon) + 5;
        }
        boolean caret = focus && (System.currentTimeMillis() / 500) % 2 == 0;
        String s = value.isEmpty() && !focus ? hint : value;
        s = Fonts.trim(s, x + w - tx - 8);
        Fonts.draw(ms, s + (caret ? "_" : ""), tx, y + (h - 8) / 2f, value.isEmpty() && !focus ? 0xFF6E6E80 : 0xFFFFFFFF);
    }

    protected static void scissor(net.minecraft.client.MinecraftClient mc, int x, int y, int w, int h) {
        double s = mc.getWindow().getScaleFactor();
        int fh = mc.getWindow().getFramebufferHeight();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor((int) (x * s), (int) (fh - (y + h) * s), Math.max(0, (int) (w * s)), Math.max(0, (int) (h * s)));
    }

    protected static void noScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    protected static boolean typable(char c) {
        return c >= 32 && c != 127;
    }
}
