package com.nnpg.blazed.mixin.protection;

import com.nnpg.blazed.protection.ModRegistry;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// REWRITE RATIONALE:
// The previous approach tried to intercept the internal
// Language.load(InputStream, BiConsumer) call inside TranslationStorage's
// loading pipeline, attributing each key to whichever ResourcePack it came
// from as it streamed in. That call site's exact bytecode shape differs
// between Loom's dev-environment recompiled Minecraft jar and the real,
// officially-compiled production jar - so the injection matched fine in
// dev but silently failed to match in every real launcher we tested,
// leaving ModRegistry permanently empty (0 vanilla keys tracked).
//
// This version sidesteps that entirely: instead of watching *how* keys get
// loaded, it just reads the *result* - the fully merged translations map -
// straight from TranslationStorage via the existing accessor, after each
// load completes. That only depends on the public/stable `translations`
// field, not on any specific internal call shape.
//
// Trust model:
//   - The FIRST load of a session happens at client boot, before any
//     server pack could possibly be involved. Everything present in that
//     snapshot is either vanilla or a locally-installed mod's own lang
//     file - both trusted. This becomes the baseline.
//   - Any LATER reload (e.g. triggered by joining a server that pushes its
//     own resource pack) is diffed against that baseline. Keys that are
//     genuinely new are classified as server-pack-provided.
//   - Known limitation: this only detects *new* keys, not a malicious
//     server overriding the *value* of an existing vanilla/mod key with
//     the same name. The original pack-attribution approach could catch
//     that in principle; this simpler diff-based approach does not. Worth
//     revisiting if that threat model matters for your use case.
//   - Known limitation: since we no longer track which specific mod
//     contributed a given key (we only see the final merged map, not
//     per-pack), all baseline keys are recorded as "vanilla" rather than
//     preserving the original vanilla-vs-specific-mod distinction. This
//     doesn't weaken the actual protection (both vanilla and server-pack
//     keys are treated as "trusted, allow original translation" in
//     TranslatableTextContentMixin), it just loses per-mod attribution
//     used by ModRegistry's whitelisting-by-mod features.
@Mixin(TranslationStorage.class)
public class TranslationStorageMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("Blazed-Protection");

    @Unique
    private static boolean blazed$baselineCaptured = false;

    @Unique
    private static final Set<String> blazed$baselineKeys = new HashSet<>();

    @Inject(
        method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;",
        at = @At("HEAD"),
        require = 0
    )
    private static void blazed$onLoadStart(
            ResourceManager resourceManager,
            List<String> definitions,
            boolean rightToLeft,
            CallbackInfoReturnable<TranslationStorage> cir) {
        try {
            if (!blazed$baselineCaptured) {
                // Full clear only makes sense before the baseline exists.
                ModRegistry.clearTranslationKeys();
            } else {
                // Later reload: only reset server-pack tracking. The trusted
                // baseline (vanilla + local mods) must survive every reload
                // for the whole client session, or every lookup after the
                // first reload would wrongly look "unrecognized".
                ModRegistry.clearServerPackKeys();
            }
            LOGGER.info("[Blazed Protection] Starting language load, clearing caches");
        } catch (Throwable t) {
            LOGGER.error("[Blazed Protection] Error clearing translation keys", t);
        }
    }

    @Inject(
        method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;",
        at = @At("RETURN"),
        require = 0
    )
    private static void blazed$onLoadComplete(
            ResourceManager resourceManager,
            List<String> definitions,
            boolean rightToLeft,
            CallbackInfoReturnable<TranslationStorage> cir) {
        try {
            TranslationStorage storage = cir.getReturnValue();
            if (storage == null) {
                LOGGER.error("[Blazed Protection] load() returned null, cannot read translations");
                return;
            }

            Map<String, String> translations = ((TranslationStorageAccessor) storage).blazed$getTranslations();
            if (translations == null) {
                LOGGER.error("[Blazed Protection] Accessor returned null translations map");
                return;
            }

            if (!blazed$baselineCaptured) {
                blazed$baselineKeys.clear();
                blazed$baselineKeys.addAll(translations.keySet());
                for (String key : translations.keySet()) {
                    ModRegistry.recordVanillaTranslationKey(key);
                }
                blazed$baselineCaptured = true;
                LOGGER.info("[Blazed Protection] Captured baseline of {} translation keys (vanilla + local mods)",
                    blazed$baselineKeys.size());
            } else {
                int newKeys = 0;
                for (String key : translations.keySet()) {
                    if (!blazed$baselineKeys.contains(key)) {
                        ModRegistry.recordServerPackKey(key);
                        newKeys++;
                    }
                }
                LOGGER.info("[Blazed Protection] Reload complete - {} new (server-pack) keys detected", newKeys);
            }

            ModRegistry.markInitialized();
            LOGGER.info("[Blazed Protection] Translation system initialized - {} vanilla keys, {} server pack keys, {} total keys tracked",
                ModRegistry.getVanillaKeyCount(), ModRegistry.getServerPackKeyCount(), ModRegistry.getTranslationKeyCount());
        } catch (Throwable t) {
            LOGGER.error("[Blazed Protection] Error in load complete", t);
        }
    }
}
