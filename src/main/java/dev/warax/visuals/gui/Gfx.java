package dev.warax.visuals.gui;

import dev.warax.visuals.module.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/** Простые примитивы в стиле Pulse: скруглённые панели, круги, градиенты. */
public final class Gfx {
    private Gfx() {
    }

    public static void rect(MatrixStack ms, int x1, int y1, int x2, int y2, int c) {
        DrawableHelper.fill(ms, x1, y1, x2, y2, c);
    }

    /** Скруглённый прямоугольник. */
    public static void round(MatrixStack ms, int x1, int y1, int x2, int y2, int r, int c) {
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            rect(ms, x1, y1, x2, y2, c);
            return;
        }
        rect(ms, x1, y1 + r, x2, y2 - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            rect(ms, x1 + inset, y1 + i, x2 - inset, y1 + i + 1, c);
            rect(ms, x1 + inset, y2 - i - 1, x2 - inset, y2 - i, c);
        }
    }

    /** Круг. step — высота полосы (1 — точно, 2-3 — быстрее для больших кругов). */
    public static void circle(MatrixStack ms, int cx, int cy, int r, int step, int c) {
        if (r <= 0) {
            return;
        }
        step = Math.max(1, step);
        for (int dy = -r; dy < r; dy += step) {
            double yy = dy + step / 2.0;
            double v = r * r - yy * yy;
            if (v <= 0) {
                continue;
            }
            int hw = (int) Math.round(Math.sqrt(v));
            rect(ms, cx - hw, cy + dy, cx + hw, Math.min(cy + dy + step, cy + r), c);
        }
    }

    public static void circle(MatrixStack ms, int cx, int cy, int r, int c) {
        circle(ms, cx, cy, r, 1, c);
    }

    /** Горизонтальный градиент. */
    public static void hgrad(MatrixStack ms, int x1, int y1, int x2, int y2, int c1, int c2) {
        int w = x2 - x1;
        for (int x = x1; x < x2; x += 2) {
            rect(ms, x, y1, Math.min(x + 2, x2), y2, Theme.lerp(c1, c2, (x - x1) / (float) Math.max(1, w)));
        }
    }

    /** Плавная волна 0..1 для переливающихся цветов. */
    public static float wave(float offset) {
        return (float) (Math.sin(System.currentTimeMillis() / 650.0 + offset) * 0.5 + 0.5);
    }

    /** Переливающийся градиентный текст. Возвращает ширину. */
    public static int gradText(MatrixStack ms, TextRenderer tr, String s, float x, float y) {
        float sx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            Fonts.shadow(ms, ch, x, y, Theme.grad(wave(i * 0.45f)));
            x += Fonts.width(ch);
        }
        return (int) (x - sx);
    }


    public static boolean glassOn() {
        return dev.warax.visuals.module.ModuleManager.glassUi == null || dev.warax.visuals.module.ModuleManager.glassUi.value;
    }

    public static int glowLevel() {
        return dev.warax.visuals.module.ModuleManager.glowPower == null ? 3 : dev.warax.visuals.module.ModuleManager.glowPower.i();
    }

    /** Вертикальный градиент. */
    public static void vgrad(MatrixStack ms, int x1, int y1, int x2, int y2, int c1, int c2) {
        int h = y2 - y1;
        for (int y = y1; y < y2; y += 2) {
            rect(ms, x1, y, x2, Math.min(y + 2, y2), Theme.lerp(c1, c2, (y - y1) / (float) Math.max(1, h)));
        }
    }

    /** Мягкое свечение вокруг прямоугольника. size — ширина в пикселях, a — яркость у края. */
    public static void glow(MatrixStack ms, int x1, int y1, int x2, int y2, int r, int color, int size, int a) {
        if (size <= 0 || a <= 0) {
            return;
        }
        int step = Math.max(1, a / size);
        for (int i = size; i >= 1; i--) {
            round(ms, x1 - i, y1 - i, x2 + i, y2 + i, r + i, Theme.alpha(color, step));
        }
    }

    /** Мягкая тень под панелью. */
    public static void shadow(MatrixStack ms, int x1, int y1, int x2, int y2, int r, int size) {
        glow(ms, x1, y1 + 3, x2, y2 + 3, r, 0xFF000000, size, 0x70);
    }

    /** Стеклянная панель: полупрозрачная заливка, блик сверху и светлая кайма. */
    public static void glass(MatrixStack ms, int x1, int y1, int x2, int y2, int r, int fill) {
        if (!glassOn()) {
            round(ms, x1, y1, x2, y2, r, fill);
            return;
        }
        round(ms, x1, y1, x2, y2, r, 0x26FFFFFF);
        round(ms, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), fill);
        int hh = Math.max(2, (y2 - y1) / 2);
        int in = Math.max(1, r / 2);
        vgrad(ms, x1 + in, y1 + 1, x2 - in, y1 + hh, 0x12FFFFFF, 0x00FFFFFF);
        hgradA(ms, x1 + r, y1 + 1, x2 - r, y1 + 2, 0x00FFFFFF, 0x55FFFFFF);
    }

    /** Градиентная линия, яркая по центру и гаснущая к краям. */
    public static void hgradA(MatrixStack ms, int x1, int y1, int x2, int y2, int edge, int mid) {
        int w = x2 - x1;
        if (w <= 0) {
            return;
        }
        for (int x = x1; x < x2; x += 2) {
            float t = (x - x1) / (float) w;
            float k = 1f - Math.abs(t * 2f - 1f);
            rect(ms, x, y1, Math.min(x + 2, x2), y2, Theme.lerp(edge, mid, k));
        }
    }

    /** Панель с тенью, свечением темы и стеклом. */
    public static void card(MatrixStack ms, int x1, int y1, int x2, int y2, int r, int fill, float glowK) {
        int g = glowLevel();
        shadow(ms, x1, y1, x2, y2, r, 2 + g);
        if (g > 0 && glowK > 0.01f) {
            glow(ms, x1, y1, x2, y2, r, Theme.a1(), g * 3, (int) (0x50 * glowK));
        }
        glass(ms, x1, y1, x2, y2, r, fill);
    }

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Плавное приближение значения к цели. */
    public static float approach(float v, float target, float speed) {
        float d = target - v;
        if (Math.abs(d) < 0.01f) {
            return target;
        }
        return v + d * speed;
    }
}
