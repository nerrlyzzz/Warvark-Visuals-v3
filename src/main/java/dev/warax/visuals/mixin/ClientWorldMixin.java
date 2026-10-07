package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    /** Цвет неба (в разных сборках yarn метод называется по-разному). */
    @Inject(method = {"getSkyColor", "method_23777"}, at = @At("HEAD"), cancellable = true)
    private void warax$sky(CallbackInfoReturnable<Vec3d> cir) {
        if (ModuleManager.WORLDCOLOR.isActive() && ModuleManager.wcSky.value) {
            int c = ModuleManager.color(ModuleManager.wcColor, 0f);
            cir.setReturnValue(new Vec3d(((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0, (c & 255) / 255.0));
        }
    }
}
