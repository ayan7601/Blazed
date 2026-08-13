package com.ayan7601.blazed.modules.main;

import com.ayan7601.blazed.BlazedAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.List;
import java.util.stream.Collectors;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> itemsToSell = sgGeneral.add(new ItemListSetting.Builder()
            .name("items-to-sell")
            .description("Items that will be automatically sold.")
            .defaultValue(Items.COBBLESTONE, Items.DIRT)
            .build());

    private final Setting<Integer> sellDelay = sgGeneral.add(new IntSetting.Builder()
            .name("sell-delay")
            .description("Delay in seconds between each /sellall command.")
            .defaultValue(60)
            .min(1)
            .sliderMax(300)
            .build());

    private final Setting<Boolean> sellOnActivate = sgGeneral.add(new BoolSetting.Builder()
            .name("sell-on-activate")
            .description("Sends a sell command immediately when the module is enabled, instead of waiting for the first delay.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> notifyOnSell = sgGeneral.add(new BoolSetting.Builder()
            .name("notify-on-sell")
            .description("Shows a chat message in the client whenever a sell command is sent.")
            .defaultValue(true)
            .build());

    private int tickCounter;

    public AutoSell() {
        super(BlazedAddon.CATEGORY, "auto-sell", "Automatically sells configured items using the /sellall command.");
    }

    @Override
    public void onActivate() {
        tickCounter = sellOnActivate.get() ? 0 : secondsToTicks(sellDelay.get());
    }

    @Override
    public void onDeactivate() {
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null)
            return;

        if (itemsToSell.get().isEmpty())
            return;

        if (tickCounter <= 0) {
            sendSellCommand();
            tickCounter = secondsToTicks(sellDelay.get());
        } else {
            tickCounter--;
        }
    }

    private void sendSellCommand() {
        String itemNames = itemsToSell.get().stream()
                .map(item -> Registries.ITEM.getId(item).getPath())
                .collect(Collectors.joining(" "));

        if (itemNames.isEmpty())
            return;

        mc.player.networkHandler.sendChatCommand("sellall " + itemNames);

        if (notifyOnSell.get()) {
            ChatUtils.info("Selling: " + itemNames);
        }
    }

    private int secondsToTicks(int seconds) {
        return seconds * 20;
    }
}
