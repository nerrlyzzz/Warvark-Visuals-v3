package dev.warax.visuals.mixin;

import dev.warax.visuals.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import dev.warax.visuals.render.Particles3D;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void warax$addBlockBreakParticles(CallbackInfo ci) {
        if (ModuleManager.NOBLOCKPARTICLES.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void warax$addBlockBreakingParticles(CallbackInfo ci) {
        if (ModuleManager.NOBLOCKPARTICLES.isActive()) {
            ci.cancel();
        }
    }

    /** Ванильные частицы боя → свои светящиеся. */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void warax$replace(ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz,
                               CallbackInfoReturnable<Particle> cir) {
        if (Particles3D.replaceVanilla(fx, x, y, z, vx, vy, vz)) {
            cir.setReturnValue(null);
        }
    }

    /** Эмиттер критов на сущности → свой взрыв. */
    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V",
            at = @At("HEAD"), cancellable = true)
    private void warax$emitter(Entity e, ParticleEffect fx, CallbackInfo ci) {
        if (ModuleManager.PARTICLES != null && ModuleManager.PARTICLES.isActive() && ModuleManager.ptReplace.value
                && Particles3D.replaceVanilla(fx, e.getX(), e.getY(), e.getZ(), 0, 0, 0)) {
            Particles3D.onEntity(e, ModuleManager.ptCount.i(), Particles3D.SPARK, ModuleManager.ptColor);
            ci.cancel();
        }
    }
}
