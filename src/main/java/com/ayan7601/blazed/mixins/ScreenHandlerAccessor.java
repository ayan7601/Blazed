// com/ayan7601/blazed/mixins/ScreenHandlerAccessor.java
package com.ayan7601.blazed.mixins;

import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerAccessor {
    @Accessor("revision")
    int getRevision();
}