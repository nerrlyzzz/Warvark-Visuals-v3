package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.hud.BossBarHud;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void warax$render(CallbackInfo ci) {
        if (ModuleManager.NOBOSSBAR.isActive()) {
            ci.cancel();
        }
    }
}
