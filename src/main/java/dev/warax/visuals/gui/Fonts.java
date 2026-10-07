package dev.warax.visuals.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Единый шрифт клиента (assets/waraxvisuals/font/main.ttf).
 * Зашит в мод: в настройках его поменять нельзя, весь интерфейс мода рисуется только им.
 */
public final class Fonts {
    public static final Identifier ID = new Identifier("waraxvisuals", "main");
    private static final Style STYLE = Style.EMPTY.withFont(ID);

    private Fonts() {
    }

    private static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static MutableText of(String s) {
        return new LiteralText(s == null ? "" : s).setStyle(STYLE);
    }

    public static MutableText of(Text t) {
        return new LiteralText("").setStyle(STYLE).append(t);
    }

    public static int draw(MatrixStack ms, String s, float x, float y, int c) {
        return tr().draw(ms, of(s), x, y, c);
    }

    public static int draw(MatrixStack ms, Text t, float x, float y, int c) {
        return tr().draw(ms, of(t), x, y, c);
    }

    public static int draw(MatrixStack ms, OrderedText t, float x, float y, int c) {
        return tr().draw(ms, t, x, y, c);
    }

    public static int shadow(MatrixStack ms, String s, float x, float y, int c) {
        return tr().drawWithShadow(ms, of(s), x, y, c);
    }

    public static int shadow(MatrixStack ms, Text t, float x, float y, int c) {
        return tr().drawWithShadow(ms, of(t), x, y, c);
    }

    public static int shadow(MatrixStack ms, OrderedText t, float x, float y, int c) {
        return tr().drawWithShadow(ms, t, x, y, c);
    }

    public static int width(String s) {
        return tr().getWidth(of(s));
    }

    public static int width(Text t) {
        return tr().getWidth(of(t));
    }

    public static int width(OrderedText t) {
        return tr().getWidth(t);
    }

    /** Обрезает строку под ширину в пикселях (с учётом нашего шрифта). */
    public static String trim(String s, int maxW) {
        if (s == null || width(s) <= maxW) {
            return s;
        }
        int n = s.length();
        while (n > 0 && width(s.substring(0, n)) > maxW) {
            n--;
        }
        return s.substring(0, n);
    }
}
