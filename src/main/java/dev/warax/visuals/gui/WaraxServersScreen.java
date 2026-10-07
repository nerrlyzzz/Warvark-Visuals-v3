package dev.warax.visuals.gui;

import dev.warax.visuals.module.Theme;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Своё меню сетевой игры: карточки серверов с пингом и онлайном, быстрое подключение. */
public class WaraxServersScreen extends WaraxBaseScreen {
    private static final int ROW = 36, GAP = 5, TOP = 26;
    private static final ExecutorService EX = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "warax-ping");
        t.setDaemon(true);
        return t;
    });

    private ServerList servers;
    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    private final Set<ServerInfo> pinged = new HashSet<>();
    private final List<Float> hov = new ArrayList<>();
    private String addr = "";
    private boolean addrFocus;
    private double scroll, shown;
    private int listY, listH;
    private ServerInfo delArm;
    private long delTime;
    private String note;

    public WaraxServersScreen() {
        super("Сетевая игра");
    }

    @Override
    protected void init() {
        super.init();
        if (servers == null) {
            servers = new ServerList(client);
            servers.loadFile();
        }
        for (int i = 0; i < servers.size(); i++) {
            ping(servers.get(i));
        }
    }

    private void ping(ServerInfo s) {
        if (!pinged.add(s)) {
            return;
        }
        s.online = false;
        s.ping = -2L;
        s.label = Fonts.of("Опрос сервера...");
        s.playerCountLabel = null;
        EX.submit(() -> {
            try {
                for (Method m : MultiplayerServerListPinger.class.getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length >= 1 && p[0] == ServerInfo.class && m.getReturnType() == void.class) {
                        Object[] a = new Object[p.length];
                        a[0] = s;
                        for (int i = 1; i < p.length; i++) {
                            a[i] = p[i] == Runnable.class ? (Runnable) () -> { } : null;
                        }
                        m.invoke(pinger, a);
                        return;
                    }
                }
            } catch (Throwable t) {
                s.ping = -1L;
                s.label = Fonts.of("Не удалось подключиться");
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        try {
            pinger.tick();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void removed() {
        try {
            pinger.cancel();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected int navIndex() {
        return 2;
    }

    @Override
    protected String header() {
        return "Сетевая игра";
    }

    @Override
    protected String subHeader() {
        return note != null ? note : "ЛКМ — зайти • ПКМ дважды — удалить • F5 — обновить";
    }

    private int fieldW() {
        return cW() - 3 * 64 - 12;
    }

    @Override
    protected void content(MatrixStack ms, int mx, int my, float delta, int x, int y, int w, int h) {
        int fw = fieldW();
        field(ms, x, y, fw, 20, "↪", addr, "IP адрес сервера...", addrFocus);
        String[] bl = {"Войти", "Добавить", "Список"};
        String[] bi = {"▶", "+", "≡"};
        for (int i = 0; i < 3; i++) {
            int bx = x + fw + 6 + i * 66;
            button(ms, bx, y, 62, 20, bi[i], bl[i], Gfx.in(mx, my, bx, y, 62, 20), i == 0);
        }

        listY = y + TOP;
        listH = h - TOP;
        int n = servers.size();
        while (hov.size() < n) {
            hov.add(0f);
        }
        if (n == 0) {
            Gfx.glass(ms, x, listY, x + w, listY + 50, 8, 0x40000000);
            String t = "Серверов нет — впиши IP и нажми «Добавить»";
            Fonts.draw(ms, Fonts.trim(t, w - 20), x + 10, listY + 21, 0xFF8A8A9C);
            return;
        }
        int content = n * (ROW + GAP);
        int max = Math.max(0, content - listH);
        scroll = Math.max(0, Math.min(max, scroll));
        shown += (scroll - shown) * 0.35;

        scissor(client, x - 4, listY, w + 8, listH);
        for (int i = 0; i < n; i++) {
            ServerInfo s = servers.get(i);
            int ry = listY + i * (ROW + GAP) - (int) shown;
            if (ry + ROW < listY || ry > listY + listH) {
                continue;
            }
            boolean hv = my >= listY && my <= listY + listH && Gfx.in(mx, my, x, ry, w, ROW);
            float a = Gfx.approach(hov.get(i), hv ? 1 : 0, 0.3f);
            hov.set(i, a);
            boolean armed = s == delArm && System.currentTimeMillis() - delTime < 2500;
            int accent = armed ? 0xFFE5303A : Theme.a1();
            if (a > 0.02f || armed) {
                Gfx.glow(ms, x, ry, x + w, ry + ROW, 8, accent, 2 + Gfx.glowLevel(), (int) (0x70 * Math.max(a, armed ? 1 : 0)));
            }
            Gfx.glass(ms, x, ry, x + w, ry + ROW, 8, Theme.lerp(0x48000000, Theme.alpha(accent, 0x48), armed ? 1 : a));
            // иконка
            int ic = s.online ? Theme.grad(Gfx.wave(i * 0.15f)) : 0xFF3A3A48;
            Gfx.round(ms, x + 6, ry + 6, x + 30, ry + 30, 6, ic);
            Gfx.vgrad(ms, x + 8, ry + 7, x + 28, ry + 17, 0x30FFFFFF, 0x00FFFFFF);
            String nm = s.name == null || s.name.isEmpty() ? s.address : s.name;
            String ini = nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase();
            Fonts.shadow(ms, ini, x + 18 - Fonts.width(ini) / 2f, ry + 14, 0xFFFFFFFF);

            // пинг и онлайн справа
            int rx = x + w - 10;
            int bars = s.ping < 0 ? 0 : s.ping < 80 ? 5 : s.ping < 150 ? 4 : s.ping < 300 ? 3 : s.ping < 600 ? 2 : 1;
            int bc = bars >= 4 ? 0xFF3BE37A : bars >= 2 ? 0xFFF5C542 : 0xFFE5303A;
            if (s.ping == -2L) {
                bars = (int) (System.currentTimeMillis() / 150 % 6);
                bc = Theme.hi();
            }
            for (int b = 0; b < 5; b++) {
                int bh = 2 + b * 2;
                int bx = rx - (5 - b) * 4;
                fill(ms, bx, ry + 16 - bh, bx + 3, ry + 16, b < bars ? bc : 0x40FFFFFF);
            }
            String pingStr = s.ping >= 0 ? s.ping + " ms" : (s.ping == -2L ? "..." : "оффлайн");
            Fonts.draw(ms, pingStr, rx - 22 - Fonts.width(pingStr), ry + 8, 0xFF8A8A9C);
            Text pc = s.playerCountLabel;
            if (pc != null) {
                Fonts.draw(ms, pc, rx - Fonts.width(pc), ry + 21, 0xFFC8C8D4);
            }
            int tx = x + 38 + (int) (a * 3);
            int right = rx - 80;
            Fonts.shadow(ms, Fonts.trim(nm, right - tx), tx, ry + 7, 0xFFFFFFFF);
            String sub = armed ? "Нажми ПКМ ещё раз, чтобы удалить" : null;
            if (sub != null) {
                Fonts.draw(ms, sub, tx, ry + 20, 0xFFFF6B6B);
            } else if (s.label != null && s.online) {
                List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(s.label, Math.max(20, right - tx + 40));
                if (!lines.isEmpty()) {
                    Fonts.draw(ms, lines.get(0), tx, ry + 20, 0xFFA0A0B0);
                }
            } else {
                Fonts.draw(ms, Fonts.trim(s.address, right - tx), tx, ry + 20, 0xFF6E6E80);
            }
        }
        noScissor();
        if (content > listH) {
            int barH = Math.max(18, (int) (listH * (listH / (double) content)));
            int barY = listY + (int) ((listH - barH) * (max == 0 ? 0 : shown / max));
            Gfx.round(ms, x + w + 3, barY, x + w + 6, barY + barH, 1, Theme.alpha(Theme.hi(), 0xA0));
        }
    }

    private void join(ServerInfo s) {
        client.openScreen(new ConnectScreen(this, client, s));
    }

    @Override
    protected boolean contentClick(double mx, double my, int button) {
        int x = cX(), y = cY(), w = cW(), fw = fieldW();
        addrFocus = Gfx.in(mx, my, x, y, fw, 20);
        if (addrFocus) {
            return true;
        }
        if (button == 0) {
            for (int i = 0; i < 3; i++) {
                int bx = x + fw + 6 + i * 66;
                if (Gfx.in(mx, my, bx, y, 62, 20)) {
                    String a = addr.trim();
                    if (i == 2) {
                        client.openScreen(new MultiplayerScreen(this));
                    } else if (a.isEmpty()) {
                        note = "Сначала впиши IP адрес";
                        addrFocus = true;
                    } else if (i == 0) {
                        join(new ServerInfo(a, a, false));
                    } else {
                        ServerInfo s = new ServerInfo(a, a, false);
                        servers.add(s);
                        servers.saveFile();
                        ping(s);
                        addr = "";
                        note = "Сервер добавлен: " + a;
                    }
                    return true;
                }
            }
        }
        if (my >= listY && my <= listY + listH) {
            for (int i = 0; i < servers.size(); i++) {
                int ry = listY + i * (ROW + GAP) - (int) shown;
                if (Gfx.in(mx, my, x, ry, w, ROW)) {
                    ServerInfo s = servers.get(i);
                    if (button == 0) {
                        join(s);
                    } else if (button == 1) {
                        if (s == delArm && System.currentTimeMillis() - delTime < 2500) {
                            servers.remove(s);
                            servers.saveFile();
                            delArm = null;
                            note = "Сервер удалён";
                        } else {
                            delArm = s;
                            delTime = System.currentTimeMillis();
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll -= amount * 24;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 294) { // F5
            pinged.clear();
            for (int i = 0; i < servers.size(); i++) {
                ping(servers.get(i));
            }
            return true;
        }
        if (addrFocus) {
            if (key == 259 && !addr.isEmpty()) {
                addr = addr.substring(0, addr.length() - 1);
                return true;
            }
            if ((key == 257 || key == 335) && !addr.trim().isEmpty()) {
                join(new ServerInfo(addr.trim(), addr.trim(), false));
                return true;
            }
            if (key == 256) {
                addrFocus = false;
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (!typable(c) || c == ' ') {
            return false;
        }
        addrFocus = true;
        if (addr.length() < 64) {
            addr += c;
        }
        return true;
    }
}
