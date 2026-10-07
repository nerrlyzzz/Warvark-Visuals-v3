package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Shadow
    @Final
    public boolean isClient;

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void warax$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (isClient && ModuleManager.WEATHER.isActive()) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void warax$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (isClient && ModuleManager.WEATHER.isActive()) {
            cir.setReturnValue(0.0f);
        }
    }
}
