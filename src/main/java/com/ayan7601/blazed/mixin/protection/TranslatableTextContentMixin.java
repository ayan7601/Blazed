package com.ayan7601.blazed.mixin.protection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ayan7601.blazed.protection.PacketContext;
import com.ayan7601.blazed.protection.TranslationProtectionHandler;
import com.ayan7601.blazed.protection.TranslationProtectionHandler.InterceptionType;
import com.ayan7601.blazed.protection.ModRegistry;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(value = TranslatableTextContent.class, priority = 1500)
public abstract class TranslatableTextContentMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("Blazed-Protection");

    @Shadow @Final private String key;
    @Shadow @Final private String fallback;

    @Unique
    private boolean blazed$fromPacket = false;

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("TAIL"), require = 0)
    private void blazed$tagFromPacket(String key, String fallback, Object[] args, CallbackInfo ci) {
        try {
            this.blazed$fromPacket = PacketContext.isProcessingPacket();
            if (this.blazed$fromPacket) {
                LOGGER.info("[Blazed-Debug] TranslatableTextContent created from packet: {} | key='{}' fallback='{}'",
                    PacketContext.getPacketName(), key, fallback);
            }
        } catch (Throwable t) {

            this.blazed$fromPacket = false;
        }
    }

    @Unique
    private static final String BLAZED_ALLOW_ORIGINAL = "\0__blazed_allow__";

    @WrapOperation(
        method = "updateTranslations()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;)Ljava/lang/String;"),
        require = 0
    )
    private String blazed$wrapGetSingle(Language instance, String keyArg, Operation<String> original) {

        if (!this.blazed$fromPacket) {
            return original.call(instance, keyArg);
        }

        String result = blazed$handleTranslationLookup(keyArg, keyArg);
        if (result == BLAZED_ALLOW_ORIGINAL) {
            return original.call(instance, keyArg);
        }
        return result;
    }

    @WrapOperation(
        method = "updateTranslations()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        require = 0
    )
    private String blazed$wrapGet(Language instance, String keyArg, String fallbackArg, Operation<String> original) {

        if (!this.blazed$fromPacket) {
            return original.call(instance, keyArg, fallbackArg);
        }

        String result = blazed$handleTranslationLookup(keyArg, fallbackArg);
        if (result == BLAZED_ALLOW_ORIGINAL) {
            return original.call(instance, keyArg, fallbackArg);
        }
        return result;
    }

    @Unique
    private String blazed$handleTranslationLookup(String translationKey, String defaultValue) {

        try {

            if (!this.blazed$fromPacket || blazed$isIntegratedServerRunning()) {
                return BLAZED_ALLOW_ORIGINAL;
            }
        } catch (Throwable t) {

            return BLAZED_ALLOW_ORIGINAL;
        }

        if (ModRegistry.isVanillaTranslationKey(translationKey)) {
            return BLAZED_ALLOW_ORIGINAL;
        }

        if (ModRegistry.isServerPackTranslationKey(translationKey)) {
            return BLAZED_ALLOW_ORIGINAL;
        }

        String blockedValue = defaultValue;
        blazed$logBlocked(translationKey, blockedValue);
        return blockedValue;
    }

    @Unique
    private static boolean blazed$isIntegratedServerRunning() {
        try {

            return net.minecraft.client.MinecraftClient.getInstance().isIntegratedServerRunning();
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private void blazed$logBlocked(String translationKey, String defaultValue) {
        String originalValue = blazed$getRealTranslation(translationKey, defaultValue);

        TranslationProtectionHandler.logDetection(InterceptionType.TRANSLATION, translationKey, originalValue, defaultValue);
    }

    @Unique
    private String blazed$getRealTranslation(String translationKey, String defaultValue) {
        try {
            Language lang = Language.getInstance();
            if (lang instanceof TranslationStorageAccessor accessor) {
                Map<String, String> translations = accessor.blazed$getTranslations();
                String value = translations.get(translationKey);
                return value != null ? value : defaultValue;
            }
        } catch (Exception e) {

        }
        return defaultValue;
    }
}
