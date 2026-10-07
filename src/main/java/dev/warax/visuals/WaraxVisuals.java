package dev.warax.visuals;

import dev.warax.visuals.gui.MenuScreen;
import dev.warax.visuals.gui.WaraxTitleScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import dev.warax.visuals.hud.Hud;
import dev.warax.visuals.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WaraxVisuals implements ClientModInitializer {
    /** Открытие меню. По умолчанию — правый Shift, переназначается в настройках управления. */
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ModuleManager.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.waraxvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.waraxvisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) {
                    mc.openScreen(new MenuScreen());
                }
            }
            if (mc.currentScreen instanceof TitleScreen && ModuleManager.customTitle != null
                    && ModuleManager.customTitle.value) {
                mc.openScreen(new WaraxTitleScreen());
            }
            dev.warax.visuals.module.Account.applySaved();
            ModuleManager.tick(mc);
        });
        HudRenderCallback.EVENT.register(Hud::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(ModuleManager::shutdown);
    }
}
