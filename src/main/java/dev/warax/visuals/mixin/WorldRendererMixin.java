package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.WorldRenderer;

/** Ванильная обводка блока скрывается, когда включена своя. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void warax$drawBlockOutline(CallbackInfo ci) {
        if (ModuleManager.BLOCKOUTLINE.isActive()) {
            ci.cancel();
        }
    }
}
