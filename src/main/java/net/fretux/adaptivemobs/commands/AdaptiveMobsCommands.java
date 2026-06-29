package net.fretux.adaptivemobs.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.data.AdaptiveSavedData;
import net.fretux.adaptivemobs.difficulty.AdaptiveDifficultyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public final class AdaptiveMobsCommands {

    private AdaptiveMobsCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adaptivemobs")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get").executes(ctx -> get(ctx.getSource())))
                .then(Commands.literal("debug").executes(ctx -> debug(ctx.getSource())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("setglobaltier")
                        .then(Commands.argument("tier", integer(0, 5))
                                .executes(ctx -> setTier(ctx.getSource(), getInteger(ctx, "tier")))))
                .then(Commands.literal("getmob")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> getMob(ctx.getSource(), EntityArgument.getEntity(ctx, "target").getType())))));
    }

    private static int get(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        AdaptiveSavedData data = AdaptiveSavedData.get(level);
        source.sendSuccess(() -> Component.literal("Adaptive Mobs: global tier "
                + AdaptiveDifficultyManager.getGlobalTier(level)
                + ", day " + AdaptiveDifficultyManager.getWorldDay(level)
                + ", hostile kills " + data.getTotalHostileKills()
                + ", manual override " + (data.getManualGlobalTier() >= 0 ? data.getManualGlobalTier() : "off")), false);
        return 1;
    }

    private static int getMob(CommandSourceStack source, EntityType<?> type) {
        AdaptiveSavedData data = AdaptiveSavedData.get(source.getLevel());
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        String typeKey = key == null ? type.toString() : key.toString();
        source.sendSuccess(() -> Component.literal("Adaptive Mobs: " + typeKey + " kills "
                + data.getKillsForType(typeKey)), false);
        return 1;
    }

    private static int setTier(CommandSourceStack source, int tier) {
        AdaptiveSavedData.get(source.getLevel()).setManualGlobalTier(Math.min(tier, AMConfig.maxTier));
        source.sendSuccess(() -> Component.literal("Adaptive Mobs: manual global tier set to " + tier), true);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        AdaptiveSavedData.get(source.getLevel()).resetProgression();
        source.sendSuccess(() -> Component.literal("Adaptive Mobs: progression reset"), true);
        return 1;
    }

    private static int debug(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        AdaptiveSavedData data = AdaptiveSavedData.get(level);
        source.sendSuccess(() -> Component.literal("Adaptive Mobs debug: enabled=" + AMConfig.enabled
                + ", configMaxTier=" + AMConfig.maxTier
                + ", worldDay=" + AdaptiveDifficultyManager.getWorldDay(level)
                + ", totalKills=" + data.getTotalHostileKills()
                + ", storedTypes=" + data.getKillsByType().size()
                + ", dataVersion=" + data.getVersion()), false);
        return 1;
    }
}
