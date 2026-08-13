package com.ayan7601.blazed.modules.main;

import baritone.api.BaritoneAPI;
import com.ayan7601.blazed.BlazedAddon;
import com.ayan7601.blazed.VersionUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

public class AutoMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> blocksToMine = sgGeneral.add(new ItemListSetting.Builder()
            .name("blocks-to-mine")
            .description("Blocks that Baritone should mine.")
            .filter(item -> item instanceof BlockItem)
            .defaultValue(Items.STONE, Items.COAL_ORE)
            .build());

    private final Setting<Boolean> autoEat = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-eat")
            .description("Automatically pauses mining and eats food when hunger drops below the threshold.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> eatAtHunger = sgGeneral.add(new IntSetting.Builder()
            .name("eat-at-hunger")
            .description("Eat when hunger is at or below this value.")
            .defaultValue(16)
            .min(0)
            .max(20)
            .sliderMax(20)
            .visible(autoEat::get)
            .build());

        private final Setting<List<Item>> foodItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("food-items")
            .description("Food items the module is allowed to use.")
            .defaultValue(Items.BREAD, Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN, Items.COOKED_MUTTON)
            .visible(autoEat::get)
            .build());

    private boolean wasPaused;

    public AutoMine() {
        super(BlazedAddon.CATEGORY, "auto-mine", "Automatically mines selected blocks using Baritone.");
    }

    @Override
    public void onActivate() {
        wasPaused = false;
        startMining();
    }

    @Override
    public void onDeactivate() {
        cancelMineProcess();
        wasPaused = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        boolean pausedThisTick = false;

        // Safety routine (auto-eat takes priority)
        if (autoEat.get() && handleAutoEat()) {
            pausedThisTick = true;
        }

        if (pausedThisTick) {
            wasPaused = true;
            return;
        }

        // Resume mining if paused by safety routine
        var baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            if (wasPaused) {
                startMining();
                wasPaused = false;
            }
        }
    }

    private void startMining() {
        Block[] selectedBlocks = getSelectedBlocks();
        if (selectedBlocks.length == 0) {
            cancelMineProcess();
            return;
        }

        var baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            baritone.getMineProcess().mine(selectedBlocks);
        }
    }

    private boolean handleAutoEat() {
        if (mc.player.getFoodData().getFoodLevel() > eatAtHunger.get())
            return false;
        if (mc.player.isUsingItem())
            return true;

        FindItemResult foodResult = InvUtils.findInHotbar(stack -> foodItems.get().contains(stack.getItem()));
        if (!foodResult.found())
            return false;

        if (foodResult.isOffhand()) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
        } else {
            if (foodResult.isHotbar() && foodResult.slot() != VersionUtil.getSelectedSlot(mc.player)) {
                InvUtils.swap(foodResult.slot(), false);
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }

        mc.player.swing(InteractionHand.MAIN_HAND);
        cancelMineProcess();
        return true;
    }

    private Block[] getSelectedBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (Item item : blocksToMine.get()) {
            if (item instanceof BlockItem blockItem) {
                blocks.add(blockItem.getBlock());
            }
        }
        return blocks.toArray(Block[]::new);
    }

    private void cancelMineProcess() {
        if (BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().cancel();
        }
    }
}
