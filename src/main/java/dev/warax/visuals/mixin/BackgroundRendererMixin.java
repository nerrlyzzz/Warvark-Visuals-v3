package dev.warax.visuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {

    /** Без тумана: отодвигаем туман далеко за пределы прорисовки. */
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void warax$noFog(CallbackInfo ci) {
        if (ModuleManager.NOFOG.isActive() && (ModuleManager.fogFluids.value || ModuleManager.cameraDry())) {
            RenderSystem.fogStart(1.0E6F);
            RenderSystem.fogEnd(1.0E6F + 1.0F);
        }
    }

    /** Цвет тумана/горизонта. */
    @Inject(method = "render", at = @At("TAIL"))
    private static void warax$clearColor(CallbackInfo ci) {
        if (active()) {
            int c = ModuleManager.color(ModuleManager.wcColor, 0f);
            RenderSystem.clearColor(r(c), g(c), b(c), 0.0F);
        }
    }

    @Inject(method = "setFogBlack", at = @At("TAIL"))
    private static void warax$fogColor(CallbackInfo ci) {
        if (active()) {
            int c = ModuleManager.color(ModuleManager.wcColor, 0f);
            RenderSystem.fog(2918, r(c), g(c), b(c), 1.0F);
        }
    }

    private static boolean active() {
        return ModuleManager.WORLDCOLOR.isActive() && ModuleManager.wcFog.value && ModuleManager.cameraDry();
    }

    private static float r(int c) { return ((c >> 16) & 255) / 255f; }
    private static float g(int c) { return ((c >> 8) & 255) / 255f; }
    private static float b(int c) { return (c & 255) / 255f; }
}
