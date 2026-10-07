package dev.warax.visuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public enum Category {
        VISUAL("Визуалы"),
        WORLD("Мир"),
        SCREEN("Экран"),
        HUD("HUD"),
        MISC("Меню"),
        SECRET("Секрет");

        public final String title;

        Category(String title) {
            this.title = title;
        }
    }

    public final String name;
    public final String desc;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();

    public boolean enabled;
    /** Код клавиши GLFW, -1 — не назначена. */
    public int key = -1;
    /** Работает, пока зажата клавиша (например, зум). */
    public boolean hold;
    /** Нельзя выключить (настройки самого меню). */
    public boolean alwaysOn;
    public boolean prevDown;

    public Module(String name, String desc, Category category) {
        this.name = name;
        this.desc = desc;
        this.category = category;
    }

    public <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public void toggle() {
        if (!alwaysOn) {
            enabled = !enabled;
            ModuleManager.onToggle(this);
        }
    }

    public boolean keyDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return key >= 0 && mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
    }

    /** Модуль сейчас действует: включён и (для hold-модулей) зажата клавиша. */
    public boolean isActive() {
        if (!enabled) {
            return false;
        }
        if (category == Category.SECRET && !Secret.unlocked()) {
            return false;
        }
        return !hold || keyDown();
    }
}
