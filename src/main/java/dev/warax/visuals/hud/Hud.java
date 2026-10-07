package dev.warax.visuals.hud;

import dev.warax.visuals.gui.Fonts;
import dev.warax.visuals.gui.Gfx;
import dev.warax.visuals.module.Module;
import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.module.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

/** Отрисовка HUD: водяной знак, список модулей, FPS/CPS/координаты, клавиши, броня, свой прицел. */
public final class Hud {
    private static int frames;
    private static long lastFpsTime = System.currentTimeMillis();
    private static int fps;

    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean prevLeft, prevRight;

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private Hud() {
    }

    public static void render(MatrixStack ms, float tickDelta) {
        dev.warax.visuals.module.SecretFx.render2D(ms);
        dev.warax.visuals.module.ExtraFx.render2D(ms);
        MinecraftClient mc = MinecraftClient.getInstance();
        countFrame();
        pollClicks(mc);
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        if (ModuleManager.CROSSHAIR.isActive() && mc.currentScreen == null
                && mc.options.getPerspective().isFirstPerson()) {
            crosshair(ms, sw / 2, sh / 2);
        }
        if (mc.options.debugEnabled) {
            return;
        }

        // ----- слева сверху -----
        int y = 4;
        int topY = 2;
        if (ModuleManager.WATERMARK.isActive()) {
            topY = watermark(ms, tr, mc, sw, 4);
        }
        List<String> lines = new ArrayList<>();
        if (ModuleManager.FPS.isActive()) {
            lines.add("FPS: " + fps);
        }
        if (ModuleManager.CPS.isActive()) {
            lines.add("CPS: " + cps(LEFT) + " | " + cps(RIGHT));
        }
        if (ModuleManager.COORDS.isActive()) {
            lines.add(String.format(Locale.ROOT, "XYZ: %.1f %.1f %.1f  [%s]",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), facing(mc.player.getHorizontalFacing())));
        }
        if (ModuleManager.PING.isActive()) {
            lines.add("Пинг: " + ping(mc) + " мс");
        }
        if (ModuleManager.CLOCK.isActive()) {
            lines.add("Время: " + LocalTime.now().format(CLOCK_FMT));
        }
        if (ModuleManager.BPS.isActive()) {
            lines.add(String.format(Locale.ROOT, "Скорость: %.1f б/с", ModuleManager.bps));
        }
        if (ModuleManager.SERVER.isActive()) {
            lines.add("Сервер: " + server(mc));
        }
        if (ModuleManager.SESSION.isActive()) {
            lines.add("Сессия: " + session());
        }
        if (!lines.isEmpty()) {
            infoCard(ms, 4, y, lines);
        }

        // ----- справа сверху: список модулей -----
        int ry = 4;
        if (ModuleManager.MODLIST.isActive()) {
            List<Module> on = new ArrayList<>();
            for (Module m : ModuleManager.MODULES) {
                if (m.category != Module.Category.MISC && m != ModuleManager.MODLIST && m.isActive()) {
                    on.add(m);
                }
            }
            on.sort((a, b) -> Integer.compare(Fonts.width(b.name), Fonts.width(a.name)));
            for (int i = 0; i < on.size(); i++) {
                Module m = on.get(i);
                Float f = SLIDE.get(m);
                float s = Gfx.approach(f == null ? 0f : f, 1f, 0.2f);
                SLIDE.put(m, s);
                String n = m.name;
                int w = Fonts.width(n);
                int off = (int) ((1 - s) * (w + 14));
                int color = Theme.grad(Gfx.wave(i * 0.45f));
                int x1 = sw - w - 12 + off;
                Gfx.glass(ms, x1, ry, sw - 4 + off, ry + 13, 4, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x98 : 0xC0));
                Gfx.hgrad(ms, x1 + 2, ry + 1, sw - 4 + off, ry + 12, 0x00000000, Theme.alpha(color, 0x22));
                Gfx.hgradA(ms, x1, ry, sw - 4 + off, ry + 1, 0x00FFFFFF, 0x30FFFFFF);
                Gfx.glow(ms, sw - 4 + off, ry + 2, sw - 2 + off, ry + 11, 1, color, 3, 0x70);
                Gfx.round(ms, sw - 4 + off, ry + 2, sw - 2 + off, ry + 11, 1, color);
                Fonts.shadow(ms, n, x1 + 4, ry + 3, color);
                ry += 13;
            }
            SLIDE.keySet().retainAll(on);
        }

        if (ModuleManager.KEYSTROKES.isActive()) {
            keystrokes(ms, tr, mc, 6, sh - 96);
        }
        if (ModuleManager.ARMOR.isActive()) {
            armor(ms, tr, mc, sw / 2, sh - (mc.player.isCreative() ? 44 : 70));
        }
        if (ModuleManager.POTIONS.isActive()) {
            potions(ms, tr, mc, sw, ry + 4);
        }
        if (ModuleManager.COMPASS.isActive()) {
            compass(ms, tr, mc, sw / 2, topY);
        }
        if (ModuleManager.TARGETHUD.isActive()) {
            targetHud(ms, tr, mc, sw / 2 + 12, sh / 2 + 12);
        }
        if (ModuleManager.ITEMCOUNTER.isActive()) {
            itemCounter(ms, tr, mc, sw / 2 + 96, sh - 19);
        }
        if (ModuleManager.HELDDUR.isActive()) {
            heldDurability(ms, tr, mc, sw / 2 - 97, sh - 14);
        }
        if (ModuleManager.NOTIFY.isActive()) {
            Notifications.render(ms, tr, sw, sh);
        }
    }

    // ---------- Новые элементы ----------

    private static String server(MinecraftClient mc) {
        if (mc.isInSingleplayer()) {
            return "Одиночная игра";
        }
        ServerInfo info = mc.getCurrentServerEntry();
        return info == null ? "—" : info.address;
    }

    private static String session() {
        long s = (System.currentTimeMillis() - ModuleManager.START_TIME) / 1000L;
        return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    private static String ticks(int t) {
        if (t > 20 * 60 * 60) {
            return "**:**";
        }
        int s = t / 20;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private static void potions(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int sw, int y) {
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String n = e.getEffectType().getName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            String t = ticks(e.getDuration());
            int w = Math.max(90, Fonts.width(n) + 12 + Fonts.width(t));
            int col = 0xFF000000 | e.getEffectType().getColor();
            int x = sw - w - 6;
            Gfx.glass(ms, x, y, sw - 4, y + 18, 5, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x98 : 0xC0));
            Gfx.glow(ms, x + 3, y + 4, x + 5, y + 14, 1, col, 2, 0x80);
            Gfx.round(ms, x + 3, y + 4, x + 5, y + 14, 1, col);
            Fonts.shadow(ms, n, x + 9, y + 3, Theme.lerp(col, 0xFFFFFFFF, 0.5f));
            Fonts.shadow(ms, t, sw - 8 - Fonts.width(t), y + 3, 0xFFA0A0B0);
            float k = Math.min(1f, e.getDuration() / 1200f);
            int bw = (int) ((sw - 8 - (x + 9)) * k);
            Gfx.round(ms, x + 9, y + 13, sw - 8, y + 15, 1, 0x40FFFFFF);
            if (bw > 0) {
                Gfx.round(ms, x + 9, y + 13, x + 9 + bw, y + 15, 1, col);
            }
            y += 20;
        }
    }

    private static void compass(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int cx, int y) {
        int half = 90;
        Gfx.glass(ms, cx - half, y, cx + half, y + 14, 6, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x98 : 0xC0));
        Gfx.hgradA(ms, cx - half + 6, y + 13, cx + half - 6, y + 14, 0x00FFFFFF, Theme.hi());
        float yaw = MathHelper.wrapDegrees(mc.player.yaw);
        for (int d = -180; d < 180; d += 15) {
            float diff = MathHelper.wrapDegrees(d - yaw);
            if (Math.abs(diff) > half - 4) {
                continue;
            }
            int px = cx + Math.round(diff);
            String label = null;
            boolean main = false;
            switch (d) {
                case 0: label = "Ю"; main = true; break;
                case 90: label = "З"; main = true; break;
                case -180: label = "С"; main = true; break;
                case -90: label = "В"; main = true; break;
                case 45: label = "ЮЗ"; break;
                case 135: label = "СЗ"; break;
                case -135: label = "СВ"; break;
                case -45: label = "ЮВ"; break;
                default: break;
            }
            if (label != null) {
                Fonts.shadow(ms, label, px - Fonts.width(label) / 2f, y + 3, main ? Theme.hi() : 0xFFDDDDDD);
            } else {
                DrawableHelper.fill(ms, px, y + 4, px + 1, y + 9, 0x80FFFFFF);
            }
        }
        Gfx.glow(ms, cx - 1, y + 1, cx + 2, y + 4, 1, Theme.hi(), 2, 0x90);
        DrawableHelper.fill(ms, cx - 1, y + 1, cx + 2, y + 3, Theme.hi());
    }

    private static LivingEntity lastTarget;
    private static float shownHp;

    private static void targetHud(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int x, int y) {
        LivingEntity t = ModuleManager.target;
        if (t == null) {
            return;
        }
        if (!t.isAlive() || System.currentTimeMillis() - ModuleManager.targetTime > 4000L || mc.player.distanceTo(t) > 12f) {
            ModuleManager.target = null;
            return;
        }
        float max = Math.max(1f, t.getMaxHealth());
        float hp = t.getHealth();
        if (t != lastTarget) {
            lastTarget = t;
            shownHp = hp;
        }
        shownHp += (hp - shownHp) * 0.15f;
        boolean head = ModuleManager.thHead.value && t instanceof PlayerEntity;
        int w = 140, h = 52;
        Gfx.card(ms, x, y, x + w, y + h, 8, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0xB0 : 0xE0), 0.5f + 0.5f * Gfx.wave(0f));
        Gfx.hgrad(ms, x + 8, y, x + w - 8, y + 1, Theme.hi(), Theme.a1());
        int tx = x + 6;
        if (head) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(t.getUuid());
            if (e != null) {
                Identifier skin = e.getSkinTexture();
                mc.getTextureManager().bindTexture(skin);
                RenderSystem.enableBlend();
                RenderSystem.color4f(1f, 1f, 1f, 1f);
                DrawableHelper.drawTexture(ms, x + 4, y + 5, 28, 28, 8f, 8f, 8, 8, 64, 64);
                DrawableHelper.drawTexture(ms, x + 4, y + 5, 28, 28, 40f, 8f, 8, 8, 64, 64);
                tx = x + 36;
            }
        }
        String name = t.getName().getString();
        if (Fonts.width(name) > x + w - tx - 4) {
            name = Fonts.trim(name, x + w - tx - 10) + "..";
        }
        Fonts.shadow(ms, name, tx, y + 5, 0xFFFFFFFF);
        float abs = t.getAbsorptionAmount();
        String hpText = String.format(Locale.ROOT, "HP: %.1f", hp) + (abs > 0 ? String.format(Locale.ROOT, " +%.0f", abs) : "");
        float my = mc.player.getHealth();
        int cmp = hp < my ? 0xFF55FF77 : hp > my ? 0xFFFF5566 : 0xFFFFDD55;
        Fonts.shadow(ms, hpText, tx, y + 16, cmp);
        int bw = x + w - 5 - tx;
        Gfx.round(ms, tx, y + 27, tx + bw, y + 31, 2, 0xFF202028);
        int fillW = (int) (bw * MathHelper.clamp(shownHp / max, 0f, 1f));
        if (fillW > 0) {
            Gfx.glow(ms, tx, y + 27, tx + fillW, y + 31, 2, Theme.a1(), 2, 0x60);
            Gfx.hgrad(ms, tx, y + 27, tx + fillW, y + 31, Theme.a1(), Theme.hi());
        }
        int ix = tx;
        ItemStack main = t.getMainHandStack();
        if (!main.isEmpty()) {
            mc.getItemRenderer().renderGuiItemIcon(main, ix, y + 34);
            ix += 17;
        }
        List<ItemStack> armor = new ArrayList<>();
        for (ItemStack st : t.getArmorItems()) {
            armor.add(0, st);
        }
        for (ItemStack st : armor) {
            if (!st.isEmpty() && ix + 16 <= x + w) {
                mc.getItemRenderer().renderGuiItemIcon(st, ix, y + 34);
                ix += 17;
            }
        }
    }

    private static final Item[] COUNTED = {Items.TOTEM_OF_UNDYING, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE,
            Items.ENDER_PEARL, Items.EXPERIENCE_BOTTLE};

    private static void itemCounter(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int x, int y) {
        for (Item item : COUNTED) {
            int c = mc.player.inventory.count(item);
            if (c <= 0) {
                continue;
            }
            ItemStack st = new ItemStack(item);
            mc.getItemRenderer().renderGuiItemIcon(st, x, y);
            mc.getItemRenderer().renderGuiItemOverlay(tr, st, x, y, String.valueOf(c));
            x += 18;
        }
    }

    private static void heldDurability(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int right, int y) {
        ItemStack st = mc.player.getMainHandStack();
        if (st.isEmpty() || !st.isDamageable()) {
            return;
        }
        int left = st.getMaxDamage() - st.getDamage();
        String s = left + "/" + st.getMaxDamage();
        int color = Theme.lerp(0xFFFF4444, 0xFF55FF55, left / (float) st.getMaxDamage());
        Fonts.shadow(ms, s, right - Fonts.width(s), y, color);
    }

    // ---------- Вспомогательное ----------

    private static void countFrame() {
        frames++;
        long now = System.currentTimeMillis();
        if (now - lastFpsTime >= 500) {
            fps = (int) (frames * 1000L / (now - lastFpsTime));
            frames = 0;
            lastFpsTime = now;
        }
    }

    private static void pollClicks(MinecraftClient mc) {
        if (mc.currentScreen != null) {
            prevLeft = prevRight = false;
            return;
        }
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (l && !prevLeft) {
            LEFT.addLast(now);
        }
        if (r && !prevRight) {
            RIGHT.addLast(now);
        }
        prevLeft = l;
        prevRight = r;
    }

    private static int cps(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) {
            q.pollFirst();
        }
        return q.size();
    }

    private static String facing(Direction d) {
        switch (d) {
            case NORTH:
                return "С";
            case SOUTH:
                return "Ю";
            case WEST:
                return "З";
            default:
                return "В";
        }
    }

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    private static final Map<Module, Float> SLIDE = new HashMap<>();

    /** Водяной знак по центру сверху. Pulse: плашка «лого | логин | FPS | пинг | время». Возвращает новую y. */
    private static int watermark(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int sw, int y) {
        String login = dev.warax.visuals.module.Account.login();
        if (ModuleManager.wmStyle != null && ModuleManager.wmStyle.index == 1) {
            String rest = " Visuals • " + login;
            int cw = Fonts.width("WARAX") + Fonts.width(rest);
            int x = (sw - cw) / 2;
            gradText(ms, tr, "WARAX", x, y);
            Fonts.shadow(ms, rest, x + Fonts.width("WARAX"), y, 0xFFB8B8C4);
            return y + 13;
        }
        String[] parts = {login, fps + " fps", ping(mc) + " ms", LocalTime.now().format(CLOCK_FMT)};
        int nameW = Fonts.width("Warax");
        int w = 18 + nameW + 4;
        for (String p : parts) {
            w += 9 + Fonts.width(p);
        }
        w += 4;
        int x = (sw - w) / 2;
        int h = 17;
        Gfx.card(ms, x, y, x + w, y + h, 5, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x90 : 0xD0), 0.5f + 0.5f * Gfx.wave(0f));
        Gfx.glow(ms, x + 2, y + 2, x + 15, y + 15, 4, Theme.grad(Gfx.wave(0f)), 1 + Gfx.glowLevel(), 0x70);
        Gfx.round(ms, x + 2, y + 2, x + 15, y + 15, 4, Theme.grad(Gfx.wave(0f)));
        Fonts.shadow(ms, "W", x + 9 - Fonts.width("W") / 2f, y + 5, 0xFFFFFFFF);
        Gfx.gradText(ms, tr, "Warax", x + 18, y + 5);
        int sx = x + 18 + nameW + 4;
        for (String p : parts) {
            DrawableHelper.fill(ms, sx, y + 5, sx + 1, y + 12, 0x40FFFFFF);
            Fonts.shadow(ms, p, sx + 5, y + 5, 0xFFD6D6E0);
            sx += 9 + Fonts.width(p);
        }
        Gfx.hgrad(ms, x + 5, y + h - 1, x + w - 5, y + h, Theme.hi(), Theme.a1());
        return y + h + 4;
    }

    private static void panel(MatrixStack ms, TextRenderer tr, int x, int y, String text) {
        int w = Fonts.width(text) + 12;
        Gfx.shadow(ms, x, y, x + w, y + 13, 3, 3);
        Gfx.glass(ms, x, y, x + w, y + 13, 3, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x90 : 0xB0));
        Gfx.glow(ms, x + 2, y + 3, x + 4, y + 10, 1, Theme.hi(), 2, 0x60);
        Gfx.round(ms, x + 2, y + 3, x + 4, y + 10, 1, Theme.hi());
        Fonts.shadow(ms, text, x + 7, y + 3, 0xFFFFFFFF);
    }

    private static void gradText(MatrixStack ms, TextRenderer tr, String s, float x, float y) {
        for (int i = 0; i < s.length(); i++) {
            String c = String.valueOf(s.charAt(i));
            Fonts.shadow(ms, c, x, y, Theme.grad(i / (float) Math.max(1, s.length() - 1)));
            x += Fonts.width(c);
        }
    }

    private static void keystrokes(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int x, int y) {
        net.minecraft.client.option.GameOptions o = mc.options;
        key(ms, tr, o.keyForward, x + 22, y, 20, 20, null);
        key(ms, tr, o.keyLeft, x, y + 22, 20, 20, null);
        key(ms, tr, o.keyBack, x + 22, y + 22, 20, 20, null);
        key(ms, tr, o.keyRight, x + 44, y + 22, 20, 20, null);
        key(ms, tr, o.keyAttack, x, y + 44, 31, 20, "ЛКМ");
        key(ms, tr, o.keyUse, x + 33, y + 44, 31, 20, "ПКМ");
        key(ms, tr, o.keyJump, x, y + 66, 64, 14, "—");
    }

    private static final Map<KeyBinding, Float> KEYA = new HashMap<>();

    private static void key(MatrixStack ms, TextRenderer tr, KeyBinding kb, int x, int y, int w, int h, String label) {
        boolean down = kb.isPressed();
        Float f = KEYA.get(kb);
        float a = Gfx.approach(f == null ? 0f : f, down ? 1f : 0f, 0.35f);
        KEYA.put(kb, a);
        if (a > 0.05f) {
            Gfx.glow(ms, x, y, x + w, y + h, 4, Theme.a1(), 2 + Gfx.glowLevel(), (int) (0xA0 * a));
        }
        Gfx.glass(ms, x, y, x + w, y + h, 4, Theme.lerp(Theme.alpha(Theme.panelDark(), 0xA0), Theme.alpha(Theme.a1(), 0xD0), a));
        String text = label != null ? label : kb.getBoundKeyLocalizedText().getString().toUpperCase(Locale.ROOT);
        if (text.length() > 3) {
            text = text.substring(0, 3);
        }
        Fonts.shadow(ms, text, x + (w - Fonts.width(text)) / 2f, y + (h - 8) / 2f, Theme.lerp(0xFFD0D0DA, 0xFFFFFFFF, a));
    }

    /** Броня в ряд над хотбаром с полосками прочности. */
    private static void armor(MatrixStack ms, TextRenderer tr, MinecraftClient mc, int cx, int y) {
        List<ItemStack> list = new ArrayList<>();
        for (int slot = 3; slot >= 0; slot--) {
            ItemStack st = mc.player.inventory.armor.get(slot);
            if (!st.isEmpty()) {
                list.add(st);
            }
        }
        if (list.isEmpty()) {
            return;
        }
        int w = list.size() * 20 + 4;
        int x = cx - w / 2;
        Gfx.glass(ms, x, y, x + w, y + 22, 6, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x90 : 0xC0));
        int ix = x + 4;
        for (ItemStack st : list) {
            mc.getItemRenderer().renderGuiItemIcon(st, ix + 1, y + 2);
            if (st.isDamageable()) {
                float k = (st.getMaxDamage() - st.getDamage()) / (float) st.getMaxDamage();
                int color = Theme.lerp(0xFFFF4444, 0xFF55FF77, k);
                DrawableHelper.fill(ms, ix + 1, y + 18, ix + 17, y + 20, 0x60000000);
                DrawableHelper.fill(ms, ix + 1, y + 18, ix + 1 + (int) (16 * k), y + 20, color);
            }
            ix += 20;
        }
    }

    /** Единая карточка информации слева сверху (FPS, CPS, координаты...). */
    private static void infoCard(MatrixStack ms, int x, int y, List<String> lines) {
        int w = Fonts.width("Информация") + 24;
        for (String l : lines) {
            w = Math.max(w, Fonts.width(l) + 20);
        }
        int h = 18 + lines.size() * 11 + 2;
        Gfx.card(ms, x, y, x + w, y + h, 6, Theme.alpha(Theme.panelDark(), Gfx.glassOn() ? 0x98 : 0xC8), 0.4f + 0.4f * Gfx.wave(0f));
        Gfx.round(ms, x + 5, y + 5, x + 11, y + 11, 2, Theme.grad(Gfx.wave(0f)));
        gradText(ms, null, "Информация", x + 15, y + 4);
        Gfx.hgradA(ms, x + 5, y + 15, x + w - 5, y + 16, 0x00FFFFFF, Theme.alpha(Theme.hi(), 0x90));
        int ly = y + 19;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            int c = l.indexOf(": ");
            Gfx.circle(ms, x + 8, ly + 4, 1, Theme.grad(Gfx.wave(i * 0.3f)));
            if (c > 0) {
                String lab = l.substring(0, c + 1);
                Fonts.shadow(ms, lab, x + 13, ly, 0xFF8A8A9C);
                Fonts.shadow(ms, l.substring(c + 2), x + 16 + Fonts.width(lab), ly, 0xFFFFFFFF);
            } else {
                Fonts.shadow(ms, l, x + 13, ly, 0xFFFFFFFF);
            }
            ly += 11;
        }
    }

    private static void crosshair(MatrixStack ms, int cx, int cy) {
        int size = ModuleManager.chSize.i();
        int gap = ModuleManager.chGap.i();
        int t = ModuleManager.chThick.i();
        int color = crosshairColor();
        int lo = t / 2;
        boolean outline = ModuleManager.chOutline.value;
        int out = 0xFF000000;

        // левая, правая, верхняя, нижняя перекладины
        int[][] bars = {
                {cx - gap - size, cy - lo, cx - gap, cy - lo + t},
                {cx + gap + 1, cy - lo, cx + gap + 1 + size, cy - lo + t},
                {cx - lo, cy - gap - size, cx - lo + t, cy - gap},
                {cx - lo, cy + gap + 1, cx - lo + t, cy + gap + 1 + size}
        };
        if (outline) {
            for (int[] b : bars) {
                DrawableHelper.fill(ms, b[0] - 1, b[1] - 1, b[2] + 1, b[3] + 1, out);
            }
        }
        for (int[] b : bars) {
            DrawableHelper.fill(ms, b[0], b[1], b[2], b[3], color);
        }
        if (ModuleManager.chDot.value) {
            if (outline) {
                DrawableHelper.fill(ms, cx - 1, cy - 1, cx + 2, cy + 2, out);
            }
            DrawableHelper.fill(ms, cx, cy, cx + 1, cy + 1, color);
        }
    }

    private static int crosshairColor() {
        switch (ModuleManager.chColor.index) {
            case 1:
                return 0xFFFFFFFF;
            case 2:
                return 0xFFFF3B3B;
            case 3:
                return 0xFF3BFF6A;
            case 4:
                return 0xFF3BD7FF;
            default:
                return Theme.hi();
        }
    }
}
