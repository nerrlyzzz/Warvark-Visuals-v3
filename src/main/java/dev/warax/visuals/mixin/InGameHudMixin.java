package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /** Прячем ванильный прицел, когда включён свой. */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void warax$crosshair(MatrixStack matrices, CallbackInfo ci) {
        if (ModuleManager.CROSSHAIR.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
    private void warax$renderPumpkinOverlay(CallbackInfo ci) {
        if (ModuleManager.NOPUMPKIN.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void warax$renderVignetteOverlay(CallbackInfo ci) {
        if (ModuleManager.NOVIGNETTE.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void warax$renderPortalOverlay(CallbackInfo ci) {
        if (ModuleManager.NOPORTAL.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void warax$renderScoreboardSidebar(CallbackInfo ci) {
        if (ModuleManager.NOSCOREBOARD.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void warax$effects(CallbackInfo ci) {
        if (ModuleManager.POTIONS.isActive() && ModuleManager.potHide.value) {
            ci.cancel();
        }
    }
}
