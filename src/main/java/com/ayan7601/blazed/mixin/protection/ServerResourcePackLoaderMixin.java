package com.ayan7601.blazed.mixin.protection;

import net.minecraft.client.resource.server.ServerResourcePackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// NOTE: In 1.21.4, the old `loadServerPack(ResourcePackProfile, List)` method
// was removed/restructured. The closest modern equivalent - the method that
// actually drives a server resource pack reload - is the private
// `reload(ReloadScheduler.ReloadContext)` method. This mixin only ever did
// logging (it doesn't feed ModRegistry or the translation-protection logic),
// so this retarget is low-risk; it just restores the missing log lines.
//
// ReloadScheduler$ReloadContext is package-private, so we can't import or
// reference it by its real type from outside net.minecraft.client.resource.server.
// @Coerce tells Mixin to skip strict type validation on this parameter and
// bind it loosely instead - a plain `Object` parameter WITHOUT @Coerce fails
// with InvalidInjectionException at runtime, since Mixin validates injected
// handler descriptors exactly against the real target method signature.
@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("Blazed-Protection");

    @Inject(
        method = "reload(Lnet/minecraft/client/resource/server/ReloadScheduler$ReloadContext;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void blazed$onServerPackLoad(@Coerce Object reloadContext, CallbackInfo ci) {
        try {
            LOGGER.info("[Blazed Protection] Server resource pack reload starting");
        } catch (Throwable t) {
            LOGGER.error("[Blazed Protection] Error in server pack load", t);
        }
    }

    @Inject(
        method = "reload(Lnet/minecraft/client/resource/server/ReloadScheduler$ReloadContext;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void blazed$afterServerPackLoad(@Coerce Object reloadContext, CallbackInfo ci) {
        try {
            LOGGER.info("[Blazed Protection] Server resource pack load complete");
        } catch (Throwable t) {
            LOGGER.error("[Blazed Protection] Error after server pack load", t);
        }
    }
}
