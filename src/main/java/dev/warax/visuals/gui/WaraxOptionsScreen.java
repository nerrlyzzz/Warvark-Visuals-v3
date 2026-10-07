package dev.warax.visuals.gui;

import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/** Своё меню настроек: вкладки Графика / Звук / Управление / Warax со слайдерами и переключателями. */
public class WaraxOptionsScreen extends WaraxBaseScreen {
    private static final String[] TABS = {"Графика", "Звук", "Управление", "Warax"};
    private static final String[] TAB_I = {"▣", "♪", "⌨", "✦"};
    private static final int ROW = 24, GAP = 4, TOP = 28;
    private static int lastTab;

    private static final int SLIDER = 0, TOGGLE = 1, ACTION = 2;

    private static final class Row {
        String name;
        int type;
        double min, max, step;
        DoubleSupplier get;
        DoubleConsumer set;
        DoubleFunction<String> fmt;
        BooleanSupplier bget;
        Consumer<Boolean> bset;
        Runnable action;
        String actionText;
    }

    private int tab = lastTab;
    private float tabAnim = -1;
    private final List<List<Row>> rows = new ArrayList<>();
    private final Map<Row, Float> anim = new HashMap<>();
    private Row drag;
    private double scroll, shown;
    private int listY, listH;
    private boolean terrainDirty;

    public WaraxOptionsScreen() {
        super("Настройки");
    }

    private static Row slider(String n, double min, double max, double step, DoubleSupplier g, DoubleConsumer s, DoubleFunction<String> f) {
        Row r = new Row();
        r.name = n; r.type = SLIDER; r.min = min; r.max = max; r.step = step; r.get = g; r.set = s; r.fmt = f;
        return r;
    }

    private static Row toggle(String n, BooleanSupplier g, Consumer<Boolean> s) {
        Row r = new Row();
        r.name = n; r.type = TOGGLE; r.bget = g; r.bset = s;
        return r;
    }

    private static Row action(String n, String text, Runnable a) {
        Row r = new Row();
        r.name = n; r.type = ACTION; r.actionText = text; r.action = a;
        return r;
    }

    private static DoubleFunction<String> pct() {
        return v -> Math.round(v * 100) + "%";
    }

    private Row sound(String n, SoundCategory c) {
        GameOptions o = client.options;
        return slider(n, 0, 1, 0.01, () -> o.getSoundVolume(c), v -> o.setSoundVolume(c, (float) v), pct());
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        GameOptions o = client.options;
        List<Row> g = new ArrayList<>();
        g.add(slider("Поле зрения (FOV)", 30, 110, 1, () -> o.fov, v -> o.fov = v, v -> (int) v + "°"));
        g.add(slider("Дальность прорисовки", 2, 32, 1, () -> o.viewDistance, v -> { o.viewDistance = (int) v; terrainDirty = true; }, v -> (int) v + " чанков"));
        g.add(slider("Яркость", 0, 1, 0.01, () -> Math.min(1, o.gamma), v -> o.gamma = v, pct()));
        g.add(slider("Макс. FPS", 10, 260, 10, () -> o.maxFps, v -> {
            o.maxFps = (int) v;
            client.getWindow().setFramerateLimit(o.maxFps);
        }, v -> v >= 260 ? "Без лимита" : String.valueOf((int) v)));
        g.add(slider("Масштаб интерфейса", 0, 4, 1, () -> o.guiScale, v -> {
            if (o.guiScale != (int) v) {
                o.guiScale = (int) v;
                client.onResolutionChanged();
            }
        }, v -> v == 0 ? "Авто" : String.valueOf((int) v)));
        g.add(toggle("Полноэкранный режим", () -> o.fullscreen, b -> {
            if (o.fullscreen != b) {
                o.fullscreen = b;
                client.getWindow().toggleFullscreen();
                o.fullscreen = client.getWindow().isFullscreen();
            }
        }));
        g.add(toggle("Вертикальная синхронизация", () -> o.enableVsync, b -> {
            o.enableVsync = b;
            client.getWindow().setVsync(b);
        }));
        g.add(toggle("Тени сущностей", () -> o.entityShadows, b -> o.entityShadows = b));
        g.add(toggle("Покачивание камеры", () -> o.bobView, b -> o.bobView = b));
        g.add(action("Все настройки графики", "Открыть", this::vanillaOptions));
        rows.add(g);

        List<Row> s = new ArrayList<>();
        s.add(sound("Общая громкость", SoundCategory.MASTER));
        s.add(sound("Музыка", SoundCategory.MUSIC));
        s.add(sound("Игроки", SoundCategory.PLAYERS));
        s.add(sound("Враждебные существа", SoundCategory.HOSTILE));
        s.add(sound("Дружелюбные существа", SoundCategory.NEUTRAL));
        s.add(sound("Блоки", SoundCategory.BLOCKS));
        s.add(sound("Окружение", SoundCategory.AMBIENT));
        s.add(sound("Погода", SoundCategory.WEATHER));
        rows.add(s);

        List<Row> c = new ArrayList<>();
        c.add(slider("Чувствительность мыши", 0, 1, 0.01, () -> o.mouseSensitivity, v -> o.mouseSensitivity = v, pct()));
        c.add(toggle("Инверсия мыши", () -> o.invertYMouse, b -> o.invertYMouse = b));
        c.add(toggle("Автопрыжок", () -> o.autoJump, b -> o.autoJump = b));
        c.add(action("Назначение клавиш", "Открыть", this::vanillaOptions));
        rows.add(c);

        List<Row> w = new ArrayList<>();
        w.add(toggle("Своё главное меню", () -> ModuleManager.customTitle.value, b -> ModuleManager.customTitle.value = b));
        w.add(toggle("Стеклянный стиль", () -> ModuleManager.glassUi.value, b -> ModuleManager.glassUi.value = b));
        w.add(toggle("Размытие фона", () -> ModuleManager.blurBg.value, b -> ModuleManager.blurBg.value = b));
        w.add(slider("Сила размытия", 1, 3, 1, () -> ModuleManager.blurPower.value, v -> ModuleManager.blurPower.value = v, v -> String.valueOf((int) v)));
        w.add(slider("Свечение", 0, 5, 1, () -> ModuleManager.glowPower.value, v -> ModuleManager.glowPower.value = v, v -> String.valueOf((int) v)));
        w.add(slider("Тема", 0, Theme.count() - 1, 1, () -> Theme.index(), v -> Theme.set((int) v), v -> Theme.NAMES[Math.max(0, Math.min(Theme.count() - 1, (int) v))]));
        w.add(action("Меню функций (ClickGUI)", "Открыть", () -> client.openScreen(new MenuScreen())));
        rows.add(w);
    }

    /** Открывает ванильные настройки через настоящую кнопку ванильного меню. */
    private void vanillaOptions() {
        TitleScreen ts = new TitleScreen();
        ts.init(client, width, height);
        for (net.minecraft.client.gui.Element e : ts.children()) {
            if (e instanceof ButtonWidget) {
                Text m = ((ButtonWidget) e).getMessage();
                if (m instanceof TranslatableText && "menu.options".equals(((TranslatableText) m).getKey())) {
                    ((ButtonWidget) e).onPress();
                    return;
                }
            }
        }
    }

    @Override
    public void removed() {
        lastTab = tab;
        if (terrainDirty && client.worldRenderer != null) {
            client.worldRenderer.scheduleTerrainUpdate();
        }
        client.options.write();
        ModuleManager.save();
    }

    @Override
    protected int navIndex() {
        return 3;
    }

    @Override
    protected String header() {
        return "Настройки";
    }

    @Override
    protected String subHeader() {
        return "Изменения применяются сразу и сохраняются автоматически";
    }

    private int tabW() {
        return (cW() - 3 * 4) / 4;
    }

    private int trackW() {
        return Math.min(120, cW() / 3);
    }

    @Override
    protected void content(MatrixStack ms, int mx, int my, float delta, int x, int y, int w, int h) {
        // вкладки с плавающей подложкой
        int tw = tabW();
        Gfx.glass(ms, x, y, x + w, y + 22, 8, 0x40000000);
        tabAnim = tabAnim < 0 ? tab : Gfx.approach(tabAnim, tab, 0.3f);
        int ax = (int) (x + tabAnim * (tw + 4));
        Gfx.glow(ms, ax + 2, y + 2, ax + tw - 2, y + 20, 6, Theme.a1(), 2 + Gfx.glowLevel(), 0x70);
        Gfx.round(ms, ax + 2, y + 2, ax + tw - 2, y + 20, 6, Theme.grad(Gfx.wave(0)));
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + i * (tw + 4);
            String t = TAB_I[i] + " " + TABS[i];
            t = Fonts.trim(t, tw - 6);
            boolean hv = Gfx.in(mx, my, tx, y, tw, 22);
            Fonts.shadow(ms, t, tx + (tw - Fonts.width(t)) / 2f, y + 7, i == tab ? 0xFFFFFFFF : (hv ? Theme.hi() : 0xFF9A9AAA));
        }

        listY = y + TOP;
        listH = h - TOP;
        List<Row> l = rows.get(tab);
        int content = l.size() * (ROW + GAP);
        int max = Math.max(0, content - listH);
        scroll = Math.max(0, Math.min(max, scroll));
        shown += (scroll - shown) * 0.35;
        if (drag != null) {
            applyDrag(mx);
        }

        scissor(client, x - 4, listY, w + 8, listH);
        int trw = trackW();
        for (int i = 0; i < l.size(); i++) {
            Row r = l.get(i);
            int ry = listY + i * (ROW + GAP) - (int) shown;
            if (ry + ROW < listY || ry > listY + listH) {
                continue;
            }
            boolean hv = my >= listY && my <= listY + listH && Gfx.in(mx, my, x, ry, w, ROW);
            Gfx.glass(ms, x, ry, x + w, ry + ROW, 7, hv ? Theme.alpha(Theme.a1(), 0x38) : 0x40000000);
            Gfx.round(ms, x + 6, ry + 7, x + 8, ry + ROW - 7, 1, Theme.alpha(Theme.hi(), hv ? 0xFF : 0x90));
            Fonts.shadow(ms, Fonts.trim(r.name, w - trw - 70), x + 14, ry + 8, 0xFFE4E4EC);
            if (r.type == SLIDER) {
                double v = r.get.getAsDouble();
                double t = Math.max(0, Math.min(1, (v - r.min) / (r.max - r.min)));
                int tx = x + w - trw - 60;
                int fw = (int) (trw * t);
                Gfx.round(ms, tx, ry + 10, tx + trw, ry + 14, 2, 0x60000000);
                if (fw > 0) {
                    Gfx.glow(ms, tx, ry + 10, tx + fw, ry + 14, 2, Theme.a1(), 2, 0x50);
                    Gfx.hgrad(ms, tx, ry + 10, tx + fw, ry + 14, Theme.a1(), Theme.hi());
                }
                Gfx.circle(ms, tx + fw, ry + 12, drag == r ? 5 : 4, 0xFFFFFFFF);
                String val = r.fmt.apply(v);
                int vw = Math.max(44, Fonts.width(val) + 10);
                int vx = x + w - 6 - Math.min(52, vw);
                Gfx.round(ms, vx, ry + 5, x + w - 6, ry + ROW - 5, 4, Theme.alpha(Theme.a1(), 0x40));
                String vt = Fonts.trim(val, x + w - 10 - vx);
                Fonts.draw(ms, vt, vx + (x + w - 6 - vx - Fonts.width(vt)) / 2f, ry + 8, Theme.hi());
            } else if (r.type == TOGGLE) {
                boolean b = r.bget.getAsBoolean();
                float a = Gfx.approach(anim.containsKey(r) ? anim.get(r) : (b ? 1 : 0), b ? 1 : 0, 0.25f);
                anim.put(r, a);
                int sx = x + w - 34, sy = ry + 6;
                if (a > 0.05f) {
                    Gfx.glow(ms, sx, sy, sx + 26, sy + 12, 6, Theme.a1(), 3, (int) (0x70 * a));
                }
                Gfx.round(ms, sx, sy, sx + 26, sy + 12, 6, Theme.lerp(0xFF2A2A36, Theme.a1(), a));
                Gfx.circle(ms, (int) (sx + 6 + 14 * a), sy + 6, 4, Theme.lerp(0xFF8C8C9C, 0xFFFFFFFF, a));
            } else {
                int bw = Fonts.width(r.actionText) + 24;
                boolean bh = Gfx.in(mx, my, x + w - bw - 6, ry + 3, bw, ROW - 6);
                button(ms, x + w - bw - 6, ry + 3, bw, ROW - 6, "›", r.actionText, bh, false);
            }
        }
        noScissor();
        if (content > listH) {
            int barH = Math.max(18, (int) (listH * (listH / (double) content)));
            int barY = listY + (int) ((listH - barH) * (max == 0 ? 0 : shown / max));
            Gfx.round(ms, x + w + 3, barY, x + w + 6, barY + barH, 1, Theme.alpha(Theme.hi(), 0xA0));
        }
    }

    private void applyDrag(double mx) {
        int trw = trackW();
        int tx = cX() + cW() - trw - 60;
        double t = Math.max(0, Math.min(1, (mx - tx) / trw));
        double v = drag.min + (drag.max - drag.min) * t;
        v = Math.round(v / drag.step) * drag.step;
        v = Math.max(drag.min, Math.min(drag.max, v));
        if (Math.abs(v - drag.get.getAsDouble()) > 1e-6) {
            drag.set.accept(v);
        }
    }

    @Override
    protected boolean contentClick(double mx, double my, int button) {
        int x = cX(), y = cY(), w = cW(), tw = tabW();
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < TABS.length; i++) {
            if (Gfx.in(mx, my, x + i * (tw + 4), y, tw, 22)) {
                tab = i;
                scroll = shown = 0;
                return true;
            }
        }
        if (my < listY || my > listY + listH) {
            return false;
        }
        List<Row> l = rows.get(tab);
        for (int i = 0; i < l.size(); i++) {
            Row r = l.get(i);
            int ry = listY + i * (ROW + GAP) - (int) shown;
            if (!Gfx.in(mx, my, x, ry, w, ROW)) {
                continue;
            }
            if (r.type == SLIDER) {
                drag = r;
                applyDrag(mx);
            } else if (r.type == TOGGLE) {
                r.bset.accept(!r.bget.getAsBoolean());
            } else {
                r.action.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll -= amount * 24;
        return true;
    }
}
