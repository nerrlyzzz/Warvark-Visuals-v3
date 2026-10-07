package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void warax$selfName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (ModuleManager.SELFNAME.isActive() && entity == mc.player && !mc.options.hudHidden
                && !mc.options.getPerspective().isFirstPerson()) {
            cir.setReturnValue(true);
        }
    }
}
