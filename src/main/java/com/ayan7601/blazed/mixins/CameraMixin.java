package com.ayan7601.blazed.mixins;

import com.ayan7601.blazed.modules.main.BlazedFreecam;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public class CameraMixin {

    // sus
    @Inject(method = "update", at = @At("HEAD"))
    private void blazed$stepFreecam(DeltaTracker deltaTracker, CallbackInfo ci) {
        BlazedFreecam freecam = Modules.get().get(BlazedFreecam.class);
        if (freecam != null && freecam.isActive()) freecam.onGameRender();
    }

    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void blazed$setPos(Args args) {
        BlazedFreecam freecam = Modules.get().get(BlazedFreecam.class);
        if (freecam == null || !freecam.isActive()) return;

        args.set(0, freecam.getInterpolatedX(0.0f));
        args.set(1, freecam.getInterpolatedY(0.0f));
        args.set(2, freecam.getInterpolatedZ(0.0f));
    }

    // aye
    @ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void blazed$setRotation(Args args) {
        BlazedFreecam freecam = Modules.get().get(BlazedFreecam.class);
        if (freecam == null || !freecam.isActive()) return;

        args.set(0, (float) freecam.getInterpolatedYaw(0.0f));
        args.set(1, (float) freecam.getInterpolatedPitch(0.0f));
    }
}
