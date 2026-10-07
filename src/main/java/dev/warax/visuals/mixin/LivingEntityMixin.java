package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** Длительность анимации взмаха — только для своего игрока, только визуал. */
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void warax$swing(CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.SLOWSWING != null && ModuleManager.SLOWSWING.isActive()
                && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValueI() * ModuleManager.swingMul.value)));
        }
    }
}
