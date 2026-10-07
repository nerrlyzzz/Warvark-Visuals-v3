package dev.warax.visuals.gui;

import dev.warax.visuals.WaraxVisuals;
import dev.warax.visuals.module.Module;
import dev.warax.visuals.module.Module.Category;
import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.module.Setting;
import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ClickGUI в стиле Pulse Visuals: боковая панель с категориями, карточки модулей с переключателями,
 * поиск и выбор темы. Тема общая с главным меню и HUD.
 * ЛКМ — вкл/выкл, ПКМ — настройки, клик по клавише — бинд (ПКМ — сброс).
 */
public class MenuScreen extends Screen {
    private static final int W = 500, H = 310, SIDE = 128, TOP = 42;
    private static final int ROW_MOD = 30, ROW_SET = 18, GAP = 4;
    private static final int T_MOD = 0, T_BOOL = 1, T_NUM = 2, T_MODE = 3;
    private static final int SEARCH_W = 130;

    private static Category lastCat = Category.VISUAL;
    private static double lastScroll;
    private static String lastSearch = "";
    private static final Set<Module> OPEN = new HashSet<>();
    private static final Map<Module, Float> ANIM = new HashMap<>();
    private static final Map<Setting, Float> SANIM = new HashMap<>();

    private Category cat = (lastCat == Category.SECRET && !dev.warax.visuals.module.Secret.unlocked()) ? Category.VISUAL : lastCat;
    private double scroll = lastScroll, scrollShown = lastScroll;
    private Module listening;
    private Setting.Num dragging;
    private boolean opening;
    private int contentH;
    private String search = lastSearch;
    private boolean searchFocus;
    private final long openTime = System.currentTimeMillis();
    private float catAnim = -1;

    private static class Row {
        int type, y, h;
        Module m;
        Setting s;
    }

    public MenuScreen() {
        super(new LiteralText("Warax Visuals"));
    }

    // ---------- геометрия ----------
    private int px() {
        return (width - W) / 2;
    }

    private int py() {
        return (height - H) / 2;
    }

    private int cx() {
        return px() + SIDE + 10;
    }

    private int cy() {
        return py() + TOP + 6;
    }

    private int cw() {
        return W - SIDE - 20;
    }

    private int ch() {
        return H - TOP - 6 - 22;
    }

    private int catY(int i) {
        return py() + 50 + i * 24;
    }

    private int searchX() {
        return px() + W - 10 - SEARCH_W;
    }

    private int searchY() {
        return py() + 12;
    }

    private int themeX(int i) {
        return px() + 18 + (i % 6) * 18;
    }

    private int themeY(int i) {
        return py() + H - 40 + (i / 6) * 18;
    }

    private int trackX() {
        return cx() + cw() - 150;
    }

    private int switchX() {
        return cx() + cw() - 34;
    }

    private int bindX() {
        return switchX() - 8 - 40;
    }

    private boolean matches(Module m) {
        if (search.isEmpty()) {
            return m.category == cat;
        }
        String q = search.toLowerCase(Locale.ROOT);
        return m.name.toLowerCase(Locale.ROOT).contains(q) || m.desc.toLowerCase(Locale.ROOT).contains(q);
    }

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        int y = 0;
        for (Module m : ModuleManager.MODULES) {
            if (!matches(m)) {
                continue;
            }
            Row r = new Row();
            r.type = T_MOD;
            r.m = m;
            r.y = y;
            r.h = ROW_MOD;
            out.add(r);
            y += ROW_MOD;
            if (OPEN.contains(m)) {
                y += 2;
                for (Setting s : m.settings) {
                    Row sr = new Row();
                    sr.m = m;
                    sr.s = s;
                    sr.type = s instanceof Setting.Bool ? T_BOOL : s instanceof Setting.Num ? T_NUM : T_MODE;
                    sr.y = y;
                    sr.h = ROW_SET;
                    out.add(sr);
                    y += ROW_SET;
                }
                y += 2;
            }
            y += GAP;
        }
        contentH = y;
        return out;
    }

    private static float anim(Module m) {
        Float f = ANIM.get(m);
        float v = f == null ? (m.enabled ? 1f : 0f) : f;
        v = Gfx.approach(v, m.enabled ? 1f : 0f, 0.25f);
        ANIM.put(m, v);
        return v;
    }

    private static float anim(Setting.Bool b) {
        Float f = SANIM.get(b);
        float v = f == null ? (b.value ? 1f : 0f) : f;
        v = Gfx.approach(v, b.value ? 1f : 0f, 0.25f);
        SANIM.put(b, v);
        return v;
    }

    // ---------- жизненный цикл ----------
    @Override
    protected void init() {
        super.init();
        try {
            int code = InputUtil.fromTranslationKey(WaraxVisuals.menuKey.getBoundKeyTranslationKey()).getCode();
            opening = InputUtil.isKeyPressed(client.getWindow().getHandle(), code);
        } catch (Exception e) {
            opening = true;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        lastCat = cat;
        lastScroll = scroll;
        lastSearch = search;
        ModuleManager.save();
    }

    // ---------- отрисовка ----------
    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        float fade = Math.min(1f, (System.currentTimeMillis() - openTime) / 200f);
        Blur.render(delta);
        fill(ms, 0, 0, width, height, Theme.alpha(0xFF000000, (int) ((Blur.enabled() ? 0x40 : 0x88) * fade)));
        fillGradient(ms, 0, 0, width, height, Theme.alpha(Theme.a1(), (int) (0x22 * fade)), Theme.alpha(Theme.hi(), (int) (0x10 * fade)));
        int px = px(), py = py();

        // тень, переливающееся свечение и стекло
        Gfx.card(ms, px, py, px + W, py + H, 8, Theme.panel(), 0.6f + 0.4f * Gfx.wave(0f));
        Gfx.round(ms, px, py, px + SIDE, py + H, 8, Theme.panelDark());
        fill(ms, px + SIDE - 8, py, px + SIDE, py + H, Theme.panelDark());
        fill(ms, px + SIDE, py + 8, px + SIDE + 1, py + H - 8, 0x18FFFFFF);
        boolean secretView = cat == Category.SECRET && search.isEmpty();
        if (secretView) {
            secretBackdrop(ms, px + SIDE + 1, py + 1, px + W - 1, py + H - 1);
        }

        // логотип
        Gfx.glow(ms, px + 12, py + 12, px + 32, py + 32, 5, Theme.grad(Gfx.wave(0f)), 3 + Gfx.glowLevel(), 0x90);
        Gfx.round(ms, px + 12, py + 12, px + 32, py + 32, 5, Theme.grad(Gfx.wave(0f)));
        Fonts.shadow(ms, "W", px + 22 - Fonts.width("W") / 2f, py + 18, 0xFFFFFFFF);
        Gfx.gradText(ms, textRenderer, "Warax", px + 38, py + 13);
        Fonts.draw(ms, "visuals • 1.16.5", px + 38, py + 23, 0xFF6E6E80);

        // категории с плавающим выделением
        Category[] cats = dev.warax.visuals.module.Secret.cats();
        int selIdx = search.isEmpty() ? cat.ordinal() : -1;
        if (selIdx >= 0) {
            catAnim = catAnim < 0 ? selIdx : Gfx.approach(catAnim, selIdx, 0.3f);
            int sy = (int) (py + 50 + catAnim * 24);
            Gfx.glow(ms, px + 8, sy, px + SIDE - 8, sy + 20, 5, Theme.a1(), 2 + Gfx.glowLevel(), 0x60);
            Gfx.hgrad(ms, px + 8, sy + 5, px + SIDE - 8, sy + 15, Theme.alpha(Theme.a1(), 0x30), Theme.alpha(Theme.hi(), 0x10));
            Gfx.glass(ms, px + 8, sy, px + SIDE - 8, sy + 20, 5, Theme.alpha(Theme.a1(), 0x55));
            Gfx.round(ms, px + 8, sy + 4, px + 10, sy + 16, 1, Theme.hi());
        }
        for (int i = 0; i < cats.length; i++) {
            int by = catY(i);
            boolean sel = i == selIdx;
            if (!sel && Gfx.in(mx, my, px + 8, by, SIDE - 16, 20)) {
                Gfx.round(ms, px + 8, by, px + SIDE - 8, by + 20, 5, 0x1CFFFFFF);
            }
            Fonts.shadow(ms, icon(cats[i]), px + 16, by + 6, sel ? Theme.hi() : 0xFF7A7A8C);
            Fonts.shadow(ms, cats[i].title, px + 30, by + 6, sel ? 0xFFFFFFFF : 0xFFA0A0B0);
        }

        // выбор темы
        Fonts.draw(ms, "Тема: " + Theme.NAMES[Theme.index()], px + 12, py + H - 56, 0xFF8A8A9C);
        for (int i = 0; i < Theme.count(); i++) {
            int tx = themeX(i), ty = themeY(i);
            if (i == Theme.index()) {
                Gfx.circle(ms, tx, ty, 8, 0xFFFFFFFF);
            } else if (Gfx.in(mx, my, tx - 7, ty - 7, 14, 14)) {
                Gfx.circle(ms, tx, ty, 8, 0x60FFFFFF);
            }
            Gfx.circle(ms, tx, ty, 6, Theme.a1Of(i));
            Gfx.circle(ms, tx - 1, ty - 1, 3, Theme.hiOf(i));
        }

        // заголовок и поиск
        String header = search.isEmpty() ? cat.title : "Поиск";
        ms.push();
        ms.scale(1.4f, 1.4f, 1f);
        if (secretView) {
            float tt = (System.currentTimeMillis() % 100000L) / 1000f;
            float hx = cx() / 1.4f;
            String sh = "✦ СЕКРЕТ";
            for (int i = 0; i < sh.length(); i++) {
                String c = String.valueOf(sh.charAt(i));
                float k = (float) (0.5 + 0.5 * Math.sin(tt * 2.2 - i * 0.5));
                Fonts.shadow(ms, c, hx, (py + 13) / 1.4f - (float) Math.sin(tt * 3 + i) * 0.8f, Theme.lerp(0xFFB06CFF, 0xFFFFD27A, k));
                hx += Fonts.width(c);
            }
        } else {
            Fonts.shadow(ms, header, cx() / 1.4f, (py + 13) / 1.4f, 0xFFFFFFFF);
        }
        ms.pop();
        int count = 0, on = 0;
        for (Module m : ModuleManager.MODULES) {
            if (matches(m)) {
                count++;
                if (m.enabled) {
                    on++;
                }
            }
        }
        Fonts.draw(ms, on + " / " + count + " включено", cx(), py + 28, 0xFF6E6E80);

        int sx = searchX(), sy = searchY();
        Gfx.round(ms, sx, sy, sx + SEARCH_W, sy + 18, 5, searchFocus ? Theme.alpha(Theme.a1(), 0x50) : 0x30000000);
        if (searchFocus) {
            Gfx.round(ms, sx, sy + 17, sx + SEARCH_W, sy + 18, 0, Theme.hi());
        }
        Fonts.draw(ms, "⌕", sx + 6, sy + 5, 0xFF8A8A9C);
        String st = search.isEmpty() && !searchFocus ? "Поиск модулей..." : search;
        boolean caret = searchFocus && (System.currentTimeMillis() / 500) % 2 == 0;
        Fonts.draw(ms, st + (caret ? "_" : ""), sx + 16, sy + 5, search.isEmpty() && !searchFocus ? 0xFF6E6E80 : 0xFFFFFFFF);

        Gfx.hgrad(ms, cx(), py + TOP, cx() + cw(), py + TOP + 1, Theme.hi(), Theme.alpha(Theme.a1(), 0x00));

        // список с плавной прокруткой
        List<Row> rows = rows();
        int maxScroll = Math.max(0, contentH - ch());
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        scrollShown = scrollShown + (scroll - scrollShown) * 0.35;
        if (Math.abs(scroll - scrollShown) < 0.5) {
            scrollShown = scroll;
        }
        scissor(cx() - 2, cy(), cw() + 4, ch());
        for (Row r : rows) {
            int ry = cy() + r.y - (int) scrollShown;
            if (ry + r.h < cy() || ry > cy() + ch()) {
                continue;
            }
            boolean hov = Gfx.in(mx, my, cx(), Math.max(ry, cy()), cw(), Math.min(ry + r.h, cy() + ch()) - Math.max(ry, cy()));
            drawRow(ms, r, ry, hov);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (rows.isEmpty()) {
            String t = "Ничего не найдено";
            Fonts.draw(ms, t, cx() + (cw() - Fonts.width(t)) / 2f, cy() + 30, 0xFF6E6E80);
        }

        if (contentH > ch()) {
            int barH = Math.max(18, (int) (ch() * (ch() / (double) contentH)));
            int barY = cy() + (int) ((ch() - barH) * (maxScroll == 0 ? 0 : scrollShown / maxScroll));
            Gfx.round(ms, px + W - 6, barY, px + W - 3, barY + barH, 1, Theme.alpha(Theme.hi(), 0xA0));
        }

        String hint = listening != null
                ? "Нажмите клавишу • Backspace — убрать • Esc — отмена"
                : "ЛКМ — вкл/выкл • ПКМ — настройки • Ctrl+F — поиск";
        Fonts.draw(ms, hint, cx(), py + H - 14, 0xFF6E6E80);
        super.render(ms, mx, my, delta);
    }

    private static final float[] SSX = new float[60], SSY = new float[60], SSP = new float[60];
    static {
        java.util.Random r = new java.util.Random(777);
        for (int i = 0; i < 60; i++) {
            SSX[i] = r.nextFloat(); SSY[i] = r.nextFloat(); SSP[i] = r.nextFloat() * 10;
        }
    }

    /** Секретный раздел: туманность фиолетово-золотого цвета, мерцающие звёзды и руническое кольцо. */
    private void secretBackdrop(MatrixStack ms, int x1, int y1, int x2, int y2) {
        float t = (System.currentTimeMillis() % 1000000L) / 1000f;
        Gfx.vgrad(ms, x1, y1, x2, y2, 0x402A0B55, 0x30331A05);
        int w = x2 - x1, h = y2 - y1;
        int bx = x1 + (int) (w * (0.7 + 0.15 * Math.sin(t * 0.4))), by = y1 + (int) (h * (0.35 + 0.15 * Math.cos(t * 0.33)));
        for (int k = 0; k < 5; k++) {
            Gfx.circle(ms, bx, by, 110 - k * 20, 4, 0x0CB06CFF);
        }
        int gx = x1 + (int) (w * (0.25 + 0.1 * Math.cos(t * 0.37))), gy = y1 + (int) (h * (0.75 + 0.1 * Math.sin(t * 0.3)));
        for (int k = 0; k < 4; k++) {
            Gfx.circle(ms, gx, gy, 80 - k * 18, 4, 0x0AFFC14D);
        }
        // вращающееся кольцо из точек
        int rcx = x2 - 46, rcy = y1 + 22;
        for (int i = 0; i < 24; i++) {
            double ang = t * 0.8 + i * Math.PI * 2 / 24;
            int px = rcx + (int) (Math.cos(ang) * 16), py2 = rcy + (int) (Math.sin(ang) * 16);
            fill(ms, px, py2, px + 1, py2 + 1, i % 2 == 0 ? 0xC0FFD27A : 0xC0B06CFF);
        }
        for (int i = 0; i < 60; i++) {
            int sx = x1 + (int) (SSX[i] * w), sy = y1 + (int) (((SSY[i] - t * 0.01f * (1 + i % 3)) % 1f + 1f) % 1f * h);
            int a = (int) (0x30 + 0x90 * (0.5 + 0.5 * Math.sin(t * 2 + SSP[i])));
            int col = i % 2 == 0 ? 0xFFFFD27A : 0xFFC9A2FF;
            fill(ms, sx, sy, sx + 1, sy + 1, Theme.alpha(col, a));
            if (i % 7 == 0) {
                fill(ms, sx - 1, sy, sx + 2, sy + 1, Theme.alpha(col, a / 3));
                fill(ms, sx, sy - 1, sx + 1, sy + 2, Theme.alpha(col, a / 3));
            }
        }
    }

    private static String icon(Category c) {
        switch (c) {
            case VISUAL:
                return "◉";
            case WORLD:
                return "☀";
            case SCREEN:
                return "▣";
            case HUD:
                return "▤";
            case SECRET:
                return "✦";
            default:
                return "⚙";
        }
    }

    private void drawSwitch(MatrixStack ms, int x, int y, int w, int h, float a) {
        if (a > 0.05f) {
            Gfx.glow(ms, x, y, x + w, y + h, h / 2, Theme.a1(), 3, (int) (0x70 * a));
        }
        Gfx.round(ms, x, y, x + w, y + h, h / 2, Theme.lerp(0xFF2A2A36, Theme.a1(), a));
        int r = h / 2 - 2;
        int kx = (int) (x + h / 2 + (w - h) * a);
        Gfx.circle(ms, kx, y + h / 2, r, Theme.lerp(0xFF8C8C9C, 0xFFFFFFFF, a));
    }

    private void drawRow(MatrixStack ms, Row r, int ry, boolean hov) {
        int cx = cx(), cw = cw();
        if (r.type == T_MOD) {
            Module m = r.m;
            float a = anim(m);
            boolean open = OPEN.contains(m);
            int bg = Theme.lerp(hov ? 0x2CFFFFFF : 0x18FFFFFF, Theme.alpha(Theme.a1(), hov ? 0x48 : 0x34), a * 0.8f);
            if (a > 0.05f && Gfx.glowLevel() > 0) {
                Gfx.glow(ms, cx, ry, cx + cw, ry + ROW_MOD, 6, Theme.a1(), 2, (int) (0x40 * a));
            }
            Gfx.glass(ms, cx, ry, cx + cw, ry + ROW_MOD, 6, bg);
            if (a > 0.02f) {
                Gfx.round(ms, cx, ry + 6, cx + 2, ry + ROW_MOD - 6, 1, Theme.alpha(Theme.hi(), (int) (255 * a)));
            }
            Fonts.shadow(ms, m.name, cx + 10, ry + 5, Theme.lerp(0xFFE4E4EC, Theme.hi(), a));
            ms.push();
            ms.scale(0.75f, 0.75f, 1f);
            String d = search.isEmpty() ? m.desc : m.category.title + " • " + m.desc;
            int maxW = (int) ((bindX() - 18 - cx - 10) / 0.75f);
            if (Fonts.width(d) > maxW) {
                d = Fonts.trim(d, maxW - Fonts.width("...")) + "...";
            }
            Fonts.draw(ms, d, (cx + 10) / 0.75f, (ry + 17) / 0.75f, 0xFF8C8C9A);
            ms.pop();

            if (m.alwaysOn) {
                Fonts.draw(ms, "всегда", switchX() - 4, ry + 11, 0xFF6E6E80);
            } else {
                drawSwitch(ms, switchX(), ry + 9, 24, 12, a);
                int bx = bindX();
                Gfx.round(ms, bx, ry + 8, bx + 40, ry + 22, 4, listening == m ? Theme.alpha(Theme.a1(), 0xC0) : 0x50000000);
                String k = listening == m ? "..." : keyName(m.key);
                Fonts.draw(ms, k, bx + (40 - Fonts.width(k)) / 2f, ry + 11, 0xFFD8D8E2);
            }
            if (!m.settings.isEmpty()) {
                String mark = open ? "−" : "+";
                Fonts.shadow(ms, mark, bindX() - 12, ry + 11, open ? Theme.hi() : 0xFF9A9AAA);
            }
            return;
        }

        Gfx.round(ms, cx + 8, ry, cx + cw, ry + ROW_SET, 3, hov ? 0x1EFFFFFF : 0x0EFFFFFF);
        fill(ms, cx + 12, ry + 4, cx + 13, ry + ROW_SET - 4, Theme.alpha(Theme.a1(), 0x90));
        Fonts.draw(ms, r.s.name, cx + 18, ry + 5, 0xFFC8C8D4);
        if (r.type == T_BOOL) {
            drawSwitch(ms, cx + cw - 30, ry + 4, 20, 10, anim((Setting.Bool) r.s));
        } else if (r.type == T_NUM) {
            Setting.Num n = (Setting.Num) r.s;
            int tx = trackX();
            double t = (n.value - n.min) / (n.max - n.min);
            int fillW = (int) (100 * t);
            Gfx.round(ms, tx, ry + 7, tx + 100, ry + 11, 2, 0x50000000);
            if (fillW > 0) {
                Gfx.hgrad(ms, tx, ry + 7, tx + fillW, ry + 11, Theme.a1(), Theme.hi());
            }
            Gfx.circle(ms, tx + fillW, ry + 9, 4, 0xFFFFFFFF);
            Fonts.draw(ms, n.text(), tx + 108, ry + 5, 0xFFFFFFFF);
        } else {
            Setting.Mode md = (Setting.Mode) r.s;
            String v = md.get();
            int vw = Fonts.width(v) + 20;
            int vx = cx + cw - 6 - vw;
            Gfx.round(ms, vx, ry + 2, vx + vw, ry + ROW_SET - 2, 4, Theme.alpha(Theme.a1(), 0x40));
            Fonts.draw(ms, "‹", vx + 4, ry + 5, 0xFF9A9AAA);
            Fonts.draw(ms, "›", vx + vw - 8, ry + 5, 0xFF9A9AAA);
            Fonts.shadow(ms, v, vx + 10, ry + 5, Theme.hi());
        }
    }

    private void scissor(int x, int y, int w, int h) {
        double sf = client.getWindow().getScaleFactor();
        int fbh = client.getWindow().getFramebufferHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * sf), (int) (fbh - (y + h) * sf), (int) (w * sf), (int) (h * sf));
    }

    private static String keyName(int key) {
        if (key < 0) {
            return "—";
        }
        String s = InputUtil.fromKeyCode(key, -1).getLocalizedText().getString();
        return s.length() > 6 ? s.substring(0, 6) : s;
    }

    // ---------- ввод ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) {
            listening = null;
            return true;
        }
        int px = px();
        if (Gfx.in(mx, my, searchX(), searchY(), SEARCH_W, 18)) {
            searchFocus = true;
            if (button == 1) {
                search = "";
            }
            return true;
        }
        searchFocus = false;

        Category[] cats = dev.warax.visuals.module.Secret.cats();
        for (int i = 0; i < cats.length; i++) {
            if (Gfx.in(mx, my, px + 8, catY(i), SIDE - 16, 20)) {
                cat = cats[i];
                search = "";
                scroll = 0;
                return true;
            }
        }
        for (int i = 0; i < Theme.count(); i++) {
            if (Gfx.in(mx, my, themeX(i) - 8, themeY(i) - 8, 16, 16)) {
                Theme.set(i);
                return true;
            }
        }
        if (!Gfx.in(mx, my, cx(), cy(), cw(), ch())) {
            return super.mouseClicked(mx, my, button);
        }
        for (Row r : rows()) {
            int ry = cy() + r.y - (int) scrollShown;
            if (my < ry || my >= ry + r.h) {
                continue;
            }
            if (r.type == T_MOD) {
                int bx = bindX();
                boolean onBind = !r.m.alwaysOn && mx >= bx && mx < bx + 40;
                boolean onPlus = !r.m.settings.isEmpty() && mx >= bx - 16 && mx < bx;
                if (onBind) {
                    if (button == 0) {
                        listening = r.m;
                    } else if (button == 1) {
                        r.m.key = -1;
                    }
                } else if (button == 1 || (button == 0 && (onPlus || r.m.alwaysOn))) {
                    if (!r.m.settings.isEmpty() && !OPEN.remove(r.m)) {
                        OPEN.add(r.m);
                    }
                } else if (button == 0) {
                    r.m.toggle();
                }
            } else if (r.type == T_BOOL && button == 0) {
                Setting.Bool b = (Setting.Bool) r.s;
                b.value = !b.value;
            } else if (r.type == T_MODE) {
                Setting.Mode md = (Setting.Mode) r.s;
                if (button == 0) {
                    md.next();
                } else if (button == 1) {
                    md.prev();
                }
            } else if (r.type == T_NUM && button == 0) {
                dragging = (Setting.Num) r.s;
                applyDrag(mx);
            }
            return true;
        }
        return true;
    }

    private void applyDrag(double mx) {
        if (dragging == null) {
            return;
        }
        double t = Math.max(0, Math.min(1, (mx - trackX()) / 100.0));
        dragging.set(dragging.min + t * (dragging.max - dragging.min));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            applyDrag(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            ModuleManager.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll -= amount * 24;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && listening == null) {
            if (!Character.isISOControl(chr) && search.length() < 24) {
                search += chr;
                scroll = 0;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // отмена
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                listening.key = -1;
            } else {
                listening.key = keyCode;
            }
            listening = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchFocus = true;
            return true;
        }
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocus = false;
                return true;
            }
            return true;
        }
        if (!opening && WaraxVisuals.menuKey.matchesKey(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (WaraxVisuals.menuKey.matchesKey(keyCode, scanCode)) {
            opening = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
