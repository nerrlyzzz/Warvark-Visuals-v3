package dev.warax.visuals.module;

import dev.warax.visuals.mixin.MinecraftClientAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Логин аккаунта лаунчера и смена игрового ника из главного меню. */
public final class Account {
    private static Session original;
    private static boolean applied;

    private Account() {
    }

    /** Логин аккаунта, переданный лаунчером (-Dwarax.login). Если его нет — текущий ник. */
    public static String login() {
        String s = System.getProperty("warax.login");
        if (s == null || s.trim().isEmpty()) {
            return MinecraftClient.getInstance().getSession().getUsername();
        }
        return s.trim();
    }

    public static boolean valid(String n) {
        return n != null && n.matches("[A-Za-z0-9_]{3,16}");
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("waraxvisuals_nick.txt");
    }

    private static void setSession(String nick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Session old = mc.getSession();
        if (original == null) {
            original = old;
        }
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        ((MinecraftClientAccessor) mc).warax$setSession(new Session(nick, uuid, old.getAccessToken(), "legacy"));
    }

    /** Меняет ник и запоминает его. Возвращает текст ошибки или null. */
    public static String change(String nick) {
        nick = nick == null ? "" : nick.trim();
        if (!valid(nick)) {
            return "Ник: 3–16 символов, A-Z, 0-9 и _";
        }
        try {
            setSession(nick);
            Files.createDirectories(path().getParent());
            Files.write(path(), nick.getBytes(StandardCharsets.UTF_8));
            return null;
        } catch (Throwable t) {
            return "Не удалось сменить ник";
        }
    }

    /** Возвращает ник, выданный лаунчером. */
    public static void reset() {
        try {
            Files.deleteIfExists(path());
            if (original != null) {
                ((MinecraftClientAccessor) MinecraftClient.getInstance()).warax$setSession(original);
            }
        } catch (Throwable ignored) {
            // не критично
        }
    }

    /** Один раз при запуске применяет сохранённый ник. */
    public static void applySaved() {
        if (applied) {
            return;
        }
        applied = true;
        try {
            if (Files.exists(path())) {
                String n = new String(Files.readAllBytes(path()), StandardCharsets.UTF_8).trim();
                if (valid(n) && !n.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                    setSession(n);
                }
            }
        } catch (Throwable ignored) {
            // битый файл — оставляем ник лаунчера
        }
    }
}
