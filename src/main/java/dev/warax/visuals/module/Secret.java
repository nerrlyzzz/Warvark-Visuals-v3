package dev.warax.visuals.module;

import dev.warax.visuals.module.Module.Category;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Секретный раздел визуалов. Открывается вводом пароля в главном меню
 * (просто напечатайте его на клавиатуре). Повторный ввод снова прячет раздел.
 * В коде хранится только SHA-256 пароля.
 */
public final class Secret {
    private static final String HASH = "6021d4b19fa9297b5c114eba99cd3b6381f6173824dfb588e08108e05fd0fbe6";
    private static Boolean unlocked;

    private Secret() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("waraxvisuals_secret.txt");
    }

    public static boolean unlocked() {
        if (unlocked == null) {
            try {
                unlocked = Files.exists(path());
            } catch (Throwable t) {
                unlocked = false;
            }
        }
        return unlocked;
    }

    private static String sha(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Проверяет, заканчивается ли набранный текст паролем. */
    public static boolean matches(String typed) {
        if (typed == null) {
            return false;
        }
        for (int i = 0; i < typed.length(); i++) {
            if (HASH.equals(sha(typed.substring(i)))) {
                return true;
            }
        }
        return false;
    }

    /** Переключает раздел. Возвращает новое состояние. */
    public static boolean toggle() {
        boolean now = !unlocked();
        unlocked = now;
        try {
            if (now) {
                Files.createDirectories(path().getParent());
                Files.write(path(), "unlocked".getBytes(StandardCharsets.UTF_8));
            } else {
                Files.deleteIfExists(path());
            }
        } catch (Throwable ignored) {
            // не критично
        }
        return now;
    }

    /** Категории для ClickGUI: «Секрет» только после ввода пароля. */
    public static Category[] cats() {
        List<Category> out = new ArrayList<>();
        for (Category c : Category.values()) {
            if (c != Category.SECRET || unlocked()) {
                out.add(c);
            }
        }
        return out.toArray(new Category[0]);
    }
}
