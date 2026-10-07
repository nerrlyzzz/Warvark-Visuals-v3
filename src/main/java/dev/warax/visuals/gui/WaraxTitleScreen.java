package dev.warax.visuals.gui;

import dev.warax.visuals.module.Account;
import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.module.Secret;
import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Главное меню Warax в стиле Pulse: боковая навигация, герой-карточка с логотипом,
 * быстрые карточки «Одиночная / Сетевая», выбор темы (она же в ClickGUI и HUD),
 * смена ника и новости. Пароль секретного раздела печатается прямо здесь.
 */
public class WaraxTitleScreen extends WaraxBaseScreen {
    private static final String[] NEWS = {
            "Новое главное меню и свои экраны игры",
            "Свои настройки графики, звука и управления",
            "Полностью новый HUD и секретное меню",
            "Свои 3D-частицы вместо ванильных"
    };

    private final float[] hov = new float[4];
    private boolean editNick;
    private String nickBuf = "";
    private String msg;
    private boolean msgErr;
    private long msgTime;
    private String secretBuf = "";

    // геометрия, запоминаемая при отрисовке для кликов
    private int playX, playY, playW, playH;
    private int cardY, cardH, cardW;
    private int themeX, themeY, themeW, nickX, nickW, rowH;
    private int vanX, vanY, vanW;

    public WaraxTitleScreen() {
        super("Warax Visuals");
    }

    @Override
    protected int navIndex() {
        return 0;
    }

    @Override
    protected String header() {
        int hr = java.time.LocalTime.now().getHour();
        String g = hr < 6 ? "Доброй ночи" : hr < 12 ? "Доброе утро" : hr < 18 ? "Добрый день" : "Добрый вечер";
        return g + ", " + client.getSession().getUsername();
    }

    @Override
    protected String subHeader() {
        return "Рады видеть тебя в Warax Visuals";
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
    }

    private void msg(String s, boolean err) {
        msg = s;
        msgErr = err;
        msgTime = System.currentTimeMillis();
    }

    @Override
    protected void content(MatrixStack ms, int mx, int my, float delta, int x, int y, int w, int h) {
        float t = time();
        // ---------- герой ----------
        int heroH = 76;
        Gfx.card(ms, x, y, x + w, y + heroH, 10, Theme.alpha(Theme.panel(), Gfx.glassOn() ? 0xA0 : 0xF0), 0.7f + 0.3f * Gfx.wave(0));
        Gfx.hgrad(ms, x + 2, y + 2, x + w - 2, y + heroH - 2, Theme.alpha(Theme.a1(), 0x30), Theme.alpha(Theme.hi(), 0x08));
        // декоративные кольца справа
        for (int k = 0; k < 4; k++) {
            int r = 18 + k * 12 + (int) (3 * Math.sin(t * 1.2 + k));
            Gfx.circle(ms, x + w - 46, y + heroH / 2, r, 4, Theme.alpha(Theme.grad(Gfx.wave(k * 0.2f)), 0x10));
        }
        float ls = w < 300 ? 2.4f : 3.0f;
        ms.push();
        ms.translate(x + 14, y + 12 + 2 * Math.sin(t * 1.5), 0);
        ms.scale(ls, ls, 1f);
        Gfx.gradText(ms, textRenderer, "WARAX", 0, 0);
        ms.pop();
        String vis = "V I S U A L S";
        Fonts.draw(ms, vis, x + 15, y + 14 + 9 * ls, Theme.hi());
        Gfx.round(ms, x + 20 + Fonts.width(vis), y + 12 + 9 * (int) ls + 2, x + 50 + Fonts.width(vis), y + 22 + 9 * (int) ls + 2, 4, Theme.alpha(Theme.a1(), 0x70));
        Fonts.draw(ms, "v2.9", x + 25 + Fonts.width(vis), y + 14 + 9 * ls + 1, 0xFFFFFFFF);
        Fonts.draw(ms, "Красивая игра начинается здесь", x + 15, y + heroH - 14, 0xFF8A8A9C);
        playW = 84;
        playH = 26;
        playX = x + w - playW - 14;
        playY = y + (heroH - playH) / 2;
        boolean ph = Gfx.in(mx, my, playX, playY, playW, playH);
        hov[0] = Gfx.approach(hov[0], ph ? 1 : 0, 0.3f);
        int grow = (int) (hov[0] * 2);
        button(ms, playX - grow, playY - grow, playW + grow * 2, playH + grow * 2, "▶", "ИГРАТЬ", ph, true);

        // ---------- две быстрые карточки ----------
        cardY = y + heroH + 8;
        cardH = 44;
        cardW = (w - 8) / 2;
        String[] ct = {"Одиночная игра", "Сетевая игра"};
        String[] cd = {"Твои миры и новые приключения", "Серверы и друзья онлайн"};
        String[] ci = {"▶", "✦"};
        for (int i = 0; i < 2; i++) {
            int cx = x + i * (cardW + 8);
            boolean hv = Gfx.in(mx, my, cx, cardY, cardW, cardH);
            hov[1 + i] = Gfx.approach(hov[1 + i], hv ? 1 : 0, 0.3f);
            float a = hov[1 + i];
            if (a > 0.02f) {
                Gfx.glow(ms, cx, cardY, cx + cardW, cardY + cardH, 8, Theme.a1(), 2 + Gfx.glowLevel(), (int) (0x80 * a));
            }
            Gfx.glass(ms, cx, cardY, cx + cardW, cardY + cardH, 8, Theme.lerp(0x50000000, Theme.alpha(Theme.a1(), 0x50), a));
            int ix = cx + 10, iy = cardY + 10;
            Gfx.round(ms, ix, iy, ix + 24, iy + 24, 7, Theme.grad(Gfx.wave(i * 0.5f)));
            Gfx.vgrad(ms, ix + 2, iy + 1, ix + 22, iy + 12, 0x30FFFFFF, 0x00FFFFFF);
            Fonts.shadow(ms, ci[i], ix + 12 - Fonts.width(ci[i]) / 2f, iy + 8, 0xFFFFFFFF);
            int tx = ix + 32 + (int) (a * 3);
            Fonts.shadow(ms, Fonts.trim(ct[i], cx + cardW - tx - 14), tx, cardY + 12, 0xFFFFFFFF);
            Fonts.draw(ms, Fonts.trim(cd[i], cx + cardW - tx - 14), tx, cardY + 24, 0xFF8A8A9C);
            Fonts.shadow(ms, "›", cx + cardW - 12 + a * 3, cardY + 18, Theme.lerp(0xFF6E6E80, Theme.hi(), a));
        }

        // ---------- тема + ник ----------
        themeY = cardY + cardH + 8;
        rowH = 40;
        themeX = x;
        themeW = (int) ((w - 8) * 0.6f);
        nickX = x + themeW + 8;
        nickW = w - themeW - 8;
        Gfx.glass(ms, themeX, themeY, themeX + themeW, themeY + rowH, 8, 0x50000000);
        Fonts.draw(ms, "◐ Тема:", themeX + 8, themeY + 6, 0xFF8A8A9C);
        Fonts.shadow(ms, Theme.NAMES[Theme.index()], themeX + 12 + Fonts.width("◐ Тема:"), themeY + 6, Theme.hi());
        int n = Theme.count();
        float step = Math.min(18, (themeW - 16) / (float) n);
        for (int i = 0; i < n; i++) {
            int sx = (int) (themeX + 8 + step / 2 + i * step), sy = themeY + 27;
            boolean hv = Gfx.in(mx, my, sx - 7, sy - 7, 14, 14);
            if (i == Theme.index()) {
                Gfx.glow(ms, sx - 7, sy - 7, sx + 7, sy + 7, 7, Theme.a1Of(i), 3, 0x90);
                Gfx.circle(ms, sx, sy, 8, 0xFFFFFFFF);
            } else if (hv) {
                Gfx.circle(ms, sx, sy, 8, 0x70FFFFFF);
            }
            Gfx.circle(ms, sx, sy, 6, Theme.a1Of(i));
            Gfx.circle(ms, sx - 1, sy - 1, 3, Theme.hiOf(i));
            if (hv) {
                String nm = Theme.NAMES[i];
                int nw = Fonts.width(nm) + 8;
                Gfx.round(ms, sx - nw / 2, sy - 24, sx + nw / 2, sy - 12, 4, 0xE0101018);
                Fonts.draw(ms, nm, sx - nw / 2f + 4, sy - 22, 0xFFFFFFFF);
            }
        }

        boolean nh = Gfx.in(mx, my, nickX, themeY, nickW, rowH);
        hov[3] = Gfx.approach(hov[3], nh || editNick ? 1 : 0, 0.3f);
        Gfx.glass(ms, nickX, themeY, nickX + nickW, themeY + rowH, 8, Theme.lerp(0x50000000, Theme.alpha(Theme.a1(), 0x40), hov[3]));
        Fonts.draw(ms, "✎ Ник в игре", nickX + 8, themeY + 6, 0xFF8A8A9C);
        if (editNick) {
            field(ms, nickX + 6, themeY + 18, nickW - 12, 16, null, nickBuf, "", true);
        } else {
            Fonts.shadow(ms, Fonts.trim(client.getSession().getUsername(), nickW - 20), nickX + 8, themeY + 22, 0xFFFFFFFF);
            ms.push();
            ms.scale(0.75f, 0.75f, 1f);
            Fonts.draw(ms, "ЛКМ — сменить • ПКМ — сброс", (nickX + 8) / 0.75f, (themeY + 32) / 0.75f, 0xFF6E6E80);
            ms.pop();
        }

        // ---------- новости (если есть место) ----------
        int ny = themeY + rowH + 8;
        int avail = y + h - ny - 4;
        if (avail >= 30) {
            int lines = Math.min(NEWS.length, (avail - 18) / 11);
            int nhgt = 18 + lines * 11;
            Gfx.glass(ms, x, ny, x + w, ny + nhgt, 8, 0x40000000);
            Fonts.shadow(ms, "★ Что нового в 2.9", x + 8, ny + 6, Theme.hi());
            for (int i = 0; i < lines; i++) {
                Gfx.circle(ms, x + 11, ny + 21 + i * 11, 1, Theme.a1());
                Fonts.draw(ms, Fonts.trim(NEWS[i], w - 26), x + 16, ny + 18 + i * 11, 0xFFC8C8D4);
            }
        }

        // ---------- ссылка на ванильное меню ----------
        String van = "Ванильное меню";
        vanW = Fonts.width(van);
        vanX = width - 12 - vanW;
        vanY = height - 12;
        boolean vh = Gfx.in(mx, my, vanX, vanY - 2, vanW, 12);
        Fonts.draw(ms, van, vanX, vanY, vh ? Theme.hi() : 0xFF5E5E70);
        if (vh) {
            fill(ms, vanX, vanY + 9, vanX + vanW, vanY + 10, Theme.hi());
        }

        // ---------- тост ----------
        if (msg != null) {
            long age = System.currentTimeMillis() - msgTime;
            if (age > 3500) {
                msg = null;
            } else {
                float a = Math.min(1f, Math.min(age / 180f, (3500 - age) / 300f));
                int tw = Fonts.width(msg) + 20;
                int tx = x + (w - tw) / 2, ty = height - 34 + (int) ((1 - a) * 10);
                int col = msgErr ? 0xFFE5303A : Theme.a1();
                Gfx.glow(ms, tx, ty, tx + tw, ty + 18, 6, col, 3, (int) (0x90 * a));
                Gfx.round(ms, tx, ty, tx + tw, ty + 18, 6, Theme.alpha(0xFF101018, (int) (0xF0 * a)));
                Gfx.round(ms, tx, ty + 4, tx + 2, ty + 14, 1, Theme.alpha(col, (int) (255 * a)));
                Fonts.draw(ms, msg, tx + 10, ty + 5, Theme.alpha(0xFFFFFFFF, Math.max(8, (int) (255 * a))));
            }
        }
    }

    @Override
    protected boolean contentClick(double mx, double my, int button) {
        if (Gfx.in(mx, my, nickX, themeY, nickW, rowH)) {
            if (button == 1) {
                Account.reset();
                editNick = false;
                msg("Ник лаунчера: " + client.getSession().getUsername(), false);
            } else if (button == 0 && !editNick) {
                editNick = true;
                nickBuf = client.getSession().getUsername();
            }
            return true;
        }
        if (editNick) {
            editNick = false;
        }
        if (button != 0) {
            return false;
        }
        if (Gfx.in(mx, my, playX, playY, playW, playH)) {
            go(2);
            return true;
        }
        for (int i = 0; i < 2; i++) {
            int cx = cX() + i * (cardW + 8);
            if (Gfx.in(mx, my, cx, cardY, cardW, cardH)) {
                go(1 + i);
                return true;
            }
        }
        int n = Theme.count();
        float step = Math.min(18, (themeW - 16) / (float) n);
        for (int i = 0; i < n; i++) {
            int sx = (int) (themeX + 8 + step / 2 + i * step), sy = themeY + 27;
            if (Gfx.in(mx, my, sx - 7, sy - 7, 14, 14)) {
                Theme.set(i);
                ModuleManager.save();
                return true;
            }
        }
        if (Gfx.in(mx, my, vanX, vanY - 2, vanW, 12)) {
            if (ModuleManager.customTitle != null) {
                ModuleManager.customTitle.value = false;
                ModuleManager.save();
            }
            client.openScreen(new TitleScreen());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (editNick) {
            if (key == 257 || key == 335) {
                String err = Account.change(nickBuf);
                if (err == null) {
                    editNick = false;
                    msg("Ник изменён: " + client.getSession().getUsername(), false);
                } else {
                    msg(err, true);
                }
                return true;
            }
            if (key == 256) {
                editNick = false;
                return true;
            }
            if (key == 259) {
                if (!nickBuf.isEmpty()) {
                    nickBuf = nickBuf.substring(0, nickBuf.length() - 1);
                }
                return true;
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (editNick) {
            if (nickBuf.length() < 16 && ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_')) {
                nickBuf += c;
            }
            return true;
        }
        secretBuf += c;
        if (secretBuf.length() > 32) {
            secretBuf = secretBuf.substring(secretBuf.length() - 32);
        }
        if (Secret.matches(secretBuf)) {
            secretBuf = "";
            boolean now = Secret.toggle();
            msg(now ? "✦ Секретный раздел открыт (Right Shift в мире)" : "Секретный раздел скрыт", false);
            return true;
        }
        return super.charTyped(c, mods);
    }
}
