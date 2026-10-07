package dev.warax.visuals.hud;

import dev.warax.visuals.gui.Fonts;
import dev.warax.visuals.module.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/** Всплывающие уведомления о включении/выключении модулей (справа снизу). */
public final class Notifications {
    private static final long LIFE = 2500L;

    private static final class Note {
        final String text;
        final boolean on;
        final long time;

        Note(String text, boolean on, long time) {
            this.text = text;
            this.on = on;
            this.time = time;
        }
    }

    private static final List<Note> LIST = new ArrayList<>();

    private Notifications() {
    }

    public static void push(String name, boolean on) {
        LIST.add(new Note(name + (on ? " включён" : " выключен"), on, System.currentTimeMillis()));
        while (LIST.size() > 5) {
            LIST.remove(0);
        }
    }

    public static void render(MatrixStack ms, TextRenderer tr, int sw, int sh) {
        long now = System.currentTimeMillis();
        LIST.removeIf(n -> now - n.time > LIFE);
        int y = sh - 26;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            Note n = LIST.get(i);
            long age = now - n.time;
            float k;
            if (age < 200) {
                k = age / 200f;
            } else if (age > LIFE - 300) {
                k = (LIFE - age) / 300f;
            } else {
                k = 1f;
            }
            k = Math.max(0f, Math.min(1f, k));
            k = 1f - (1f - k) * (1f - k);
            int w = Fonts.width(n.text) + 14;
            int x = sw - (int) ((w + 4) * k);
            DrawableHelper.fill(ms, x, y, x + w, y + 18, 0xC00A0A10);
            DrawableHelper.fill(ms, x, y, x + 2, y + 18, n.on ? 0xFF55FF77 : 0xFFFF5566);
            int bar = (int) (w * (1f - age / (float) LIFE));
            DrawableHelper.fill(ms, x, y + 17, x + bar, y + 18, Theme.hi());
            Fonts.shadow(ms, n.text, x + 8, y + 5, 0xFFFFFFFF);
            y -= 21;
        }
    }
}
