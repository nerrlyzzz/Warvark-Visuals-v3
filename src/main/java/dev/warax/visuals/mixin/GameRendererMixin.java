package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import dev.warax.visuals.render.Render3D;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /** Соотношение сторон: растягиваем проекционную матрицу по X. */
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void warax$aspect(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!ModuleManager.ASPECT.isActive()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        float window = (float) mc.getWindow().getFramebufferWidth() / (float) mc.getWindow().getFramebufferHeight();
        float target = (float) ModuleManager.aspectRatio();
        if (target < 0.01f || window < 0.01f) {
            return;
        }
        Matrix4f m = cir.getReturnValue().copy();
        m.multiply(Matrix4f.scale(window / target, 1.0f, 1.0f));
        cir.setReturnValue(m);
    }

    /** FOV и зум. Применяем только к «основному» полю зрения (не к руке). */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void warax$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov) {
            cir.setReturnValue(ModuleManager.modifyFov(cir.getReturnValueD()));
        }
    }

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void warax$noHurt(MatrixStack matrices, float f, CallbackInfo ci) {
        if (ModuleManager.NOHURT.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void warax$noBob(MatrixStack matrices, float f, CallbackInfo ci) {
        if (ModuleManager.NOBOB.isActive()) {
            ci.cancel();
        }
    }

    /** Точка отрисовки 3D-эффектов: мир уже нарисован, рука ещё нет. */
    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
    private void warax$world(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        Render3D.render(matrices, tickDelta);
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void warax$noTotem(ItemStack stack, CallbackInfo ci) {
        if (ModuleManager.NOTOTEM.isActive() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
            ci.cancel();
        }
    }
}
