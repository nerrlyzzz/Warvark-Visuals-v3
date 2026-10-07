package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void warax$noFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ModuleManager.NOFIRE.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void warax$noWater(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ModuleManager.NOWATER.isActive()) {
            ci.cancel();
        }
    }
}
