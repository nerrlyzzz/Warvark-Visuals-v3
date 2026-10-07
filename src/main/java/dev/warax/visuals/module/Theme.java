package dev.warax.visuals.module;

/**
 * Градиентные темы. Одна и та же тема используется в главном меню, ClickGUI и HUD —
 * выбираешь в главном меню, и она сразу применяется везде.
 */
public final class Theme {
    public static final String[] NAMES = {
            "Фиолет–розовый", "Зелёный–чёрный", "Красный–чёрный", "Синий–голубой", "Оранж–красный",
            "Бирюза–фиолет", "Золото–бронза", "Розово–персик", "Лайм–циан", "Индиго–пурпур",
            "Мята–небо", "Снежная"
    };
    // основной цвет и яркий цвет; градиент идёт от яркого к основному
    private static final int[] A1 = {0x7c5cff, 0x12b34d, 0xe5303a, 0x2563ff, 0xff8a1f, 0x00c9a7, 0xd97706, 0xffb347, 0x00d4ff, 0x9333ea, 0x10b981, 0x94a3b8};
    private static final int[] HI = {0xff4fd8, 0x2bff7e, 0xff5560, 0x4dd8ff, 0xff9a4d, 0x2cf0d0, 0xfcd34d, 0xff7fb5, 0xc6ff5c, 0xa78bfa, 0x7dd3fc, 0xffffff};

    private Theme() {
    }

    public static int count() {
        return NAMES.length;
    }

    public static int index() {
        int i = ModuleManager.themeMode == null ? 0 : ModuleManager.themeMode.index;
        return i < 0 || i >= A1.length ? 0 : i;
    }

    public static void set(int i) {
        if (ModuleManager.themeMode != null && i >= 0 && i < NAMES.length) {
            ModuleManager.themeMode.index = i;
            ModuleManager.save();
        }
    }

    public static int hiOf(int i) {
        return 0xFF000000 | HI[i];
    }

    public static int a1Of(int i) {
        return 0xFF000000 | A1[i];
    }

    public static int hi() {
        int o = SecretFx.themeColor(true);
        if (o != 0) {
            return o;
        }
        return hiOf(index());
    }

    public static int a1() {
        int o = SecretFx.themeColor(false);
        if (o != 0) {
            return o;
        }
        return a1Of(index());
    }

    /** t = 0 — яркий цвет, t = 1 — основной. */
    public static int grad(float t) {
        return lerp(hi(), a1(), t);
    }

    public static int panel() {
        return alpha(lerp(0xFF0B0B12, a1(), 0.07f), dev.warax.visuals.gui.Gfx.glassOn() ? 0xA8 : 0xF2);
    }

    public static int panelDark() {
        return alpha(lerp(0xFF07070B, a1(), 0.05f), dev.warax.visuals.gui.Gfx.glassOn() ? 0x8C : 0xF8);
    }

    /** Фон главного меню: верх и низ. */
    public static int bgTop() {
        return lerp(0xFF06060B, a1(), 0.22f);
    }

    public static int bgBottom() {
        return lerp(0xFF040407, hi(), 0.06f);
    }

    public static int alpha(int color, int a) {
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
