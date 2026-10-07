package dev.warax.visuals.gui;

import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.level.storage.LevelSummary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Своё меню одиночной игры: карточки миров, поиск, запуск в один клик. */
public class WaraxWorldsScreen extends WaraxBaseScreen {
    private static final int ROW = 34, GAP = 5, TOP = 26;
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final List<LevelSummary> all = new ArrayList<>();
    private String error;
    private String search = "";
    private boolean searchFocus;
    private double scroll, shown;
    private final List<Float> hov = new ArrayList<>();
    private int listY, listH;
    private boolean loaded;

    public WaraxWorldsScreen() {
        super("Одиночная игра");
    }

    @Override
    protected void init() {
        super.init();
        if (!loaded) {
            loaded = true;
            try {
                List<LevelSummary> l = client.getLevelStorage().getLevelList();
                Collections.sort(l);
                all.addAll(l);
            } catch (Exception e) {
                error = "Не удалось загрузить миры: " + e.getMessage();
            }
        }
    }

    @Override
    protected int navIndex() {
        return 1;
    }

    @Override
    protected String header() {
        return "Одиночная игра";
    }

    @Override
    protected String subHeader() {
        return all.size() + " " + (all.size() % 10 == 1 && all.size() % 100 != 11 ? "мир" : "миров") + " • двойной клик не нужен — просто нажми на мир";
    }

    private List<LevelSummary> list() {
        if (search.isEmpty()) {
            return all;
        }
        List<LevelSummary> r = new ArrayList<>();
        String q = search.toLowerCase();
        for (LevelSummary s : all) {
            if (s.getDisplayName().toLowerCase().contains(q) || s.getName().toLowerCase().contains(q)) {
                r.add(s);
            }
        }
        return r;
    }

    private int btnW() {
        return 104;
    }

    @Override
    protected void content(MatrixStack ms, int mx, int my, float delta, int x, int y, int w, int h) {
        // панель действий
        int bw = btnW();
        field(ms, x, y, w - bw - 6, 20, "⌕", search, "Поиск мира...", searchFocus);
        button(ms, x + w - bw, y, bw, 20, "+", "Создать / упр.", Gfx.in(mx, my, x + w - bw, y, bw, 20), true);

        listY = y + TOP;
        listH = h - TOP;
        List<LevelSummary> l = list();
        while (hov.size() < l.size()) {
            hov.add(0f);
        }
        int content = l.size() * (ROW + GAP);
        int max = Math.max(0, content - listH);
        scroll = Math.max(0, Math.min(max, scroll));
        shown += (scroll - shown) * 0.35;

        if (error != null || l.isEmpty()) {
            String t = error != null ? error : (all.isEmpty() ? "Миров пока нет — создай первый!" : "Ничего не найдено");
            Gfx.glass(ms, x, listY, x + w, listY + 50, 8, 0x40000000);
            Fonts.draw(ms, Fonts.trim(t, w - 20), x + (w - Math.min(w - 20, Fonts.width(t))) / 2f, listY + 21, 0xFF8A8A9C);
            return;
        }

        scissor(client, x - 4, listY, w + 8, listH);
        for (int i = 0; i < l.size(); i++) {
            LevelSummary s = l.get(i);
            int ry = listY + i * (ROW + GAP) - (int) shown;
            if (ry + ROW < listY || ry > listY + listH) {
                continue;
            }
            boolean hv = my >= listY && my <= listY + listH && Gfx.in(mx, my, x, ry, w, ROW);
            float a = Gfx.approach(hov.get(i), hv ? 1 : 0, 0.3f);
            hov.set(i, a);
            if (a > 0.02f) {
                Gfx.glow(ms, x, ry, x + w, ry + ROW, 8, Theme.a1(), 2 + Gfx.glowLevel(), (int) (0x70 * a));
            }
            Gfx.glass(ms, x, ry, x + w, ry + ROW, 8, Theme.lerp(0x48000000, Theme.alpha(Theme.a1(), 0x48), a));
            // иконка мира
            int ic = s.isHardcore() ? 0xFFE5303A : Theme.grad(Gfx.wave(i * 0.15f));
            Gfx.round(ms, x + 6, ry + 5, x + 30, ry + 29, 6, ic);
            Gfx.vgrad(ms, x + 8, ry + 6, x + 28, ry + 16, 0x30FFFFFF, 0x00FFFFFF);
            String nm = s.getDisplayName();
            String ini = nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase();
            Fonts.shadow(ms, ini, x + 18 - Fonts.width(ini) / 2f, ry + 13, 0xFFFFFFFF);
            int tx = x + 38 + (int) (a * 3);
            int right = x + w - (hv ? 70 : 12);
            Fonts.shadow(ms, Fonts.trim(nm, right - tx), tx, ry + 7, 0xFFFFFFFF);
            String mode;
            try {
                mode = s.isHardcore() ? "Хардкор" : modeName(s.getGameMode().getName());
            } catch (Throwable t) {
                mode = "Мир";
            }
            String info = mode + (s.hasCheats() ? " • читы" : "") + " • " + DATE.format(new Date(s.getLastPlayed()));
            Fonts.draw(ms, Fonts.trim(info, right - tx), tx, ry + 19, 0xFF8A8A9C);
            if (hv) {
                button(ms, x + w - 62, ry + 7, 54, 20, "▶", "Играть", true, true);
            }
        }
        noScissor();
        if (content > listH) {
            int barH = Math.max(18, (int) (listH * (listH / (double) content)));
            int barY = listY + (int) ((listH - barH) * (max == 0 ? 0 : shown / max));
            Gfx.round(ms, x + w + 3, barY, x + w + 6, barY + barH, 1, Theme.alpha(Theme.hi(), 0xA0));
        }
    }

    private static String modeName(String id) {
        switch (id) {
            case "survival": return "Выживание";
            case "creative": return "Творчество";
            case "adventure": return "Приключение";
            case "spectator": return "Наблюдение";
            default: return "Мир";
        }
    }

    @Override
    protected boolean contentClick(double mx, double my, int button) {
        int x = cX(), y = cY(), w = cW();
        searchFocus = Gfx.in(mx, my, x, y, w - btnW() - 6, 20);
        if (searchFocus) {
            return true;
        }
        if (button == 0 && Gfx.in(mx, my, x + w - btnW(), y, btnW(), 20)) {
            client.openScreen(new SelectWorldScreen(this));
            return true;
        }
        if (button == 0 && my >= listY && my <= listY + listH) {
            List<LevelSummary> l = list();
            for (int i = 0; i < l.size(); i++) {
                int ry = listY + i * (ROW + GAP) - (int) shown;
                if (Gfx.in(mx, my, x, ry, w, ROW)) {
                    play(l.get(i));
                    return true;
                }
            }
        }
        return false;
    }

    private void play(LevelSummary s) {
        try {
            client.startIntegratedServer(s.getName());
        } catch (Throwable t) {
            error = "Ошибка запуска: " + t.getMessage();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll -= amount * 24;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (searchFocus) {
            if (key == 259 && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (key == 256) {
                searchFocus = false;
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (!typable(c)) {
            return false;
        }
        searchFocus = true;
        if (search.length() < 32) {
            search += c;
            scroll = 0;
        }
        return true;
    }
}
