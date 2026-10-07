package dev.warax.visuals.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.util.Identifier;

/** Размытие фона под меню (ванильный шейдер blur). */
public final class Blur {
    private static ShaderEffect fx;
    private static boolean broken;
    private static int lw = -1, lh = -1;

    private Blur() {
    }

    public static boolean enabled() {
        return !broken && ModuleManager.blurBg != null && ModuleManager.blurBg.value;
    }

    public static void render(float delta) {
        if (!enabled()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Framebuffer fb = mc.getFramebuffer();
            if (fx == null) {
                fx = new ShaderEffect(mc.getTextureManager(), mc.getResourceManager(), fb,
                        new Identifier("minecraft", "shaders/post/blur.json"));
                lw = -1;
            }
            int w = mc.getWindow().getFramebufferWidth(), h = mc.getWindow().getFramebufferHeight();
            if (w != lw || h != lh) {
                fx.setupDimensions(w, h);
                lw = w;
                lh = h;
            }
            int n = ModuleManager.blurPower == null ? 1 : ModuleManager.blurPower.i();
            for (int i = 0; i < n; i++) {
                fx.render(delta);
            }
            fb.beginWrite(true);
        } catch (Throwable t) {
            broken = true;
            fx = null;
            mc.getFramebuffer().beginWrite(true);
        }
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.enableAlphaTest();
    }
}
