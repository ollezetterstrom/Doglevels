package com.zmod.doglevels.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.network.DogLevelNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the /doglevels command.
 *
 * CHANGES IN 1.4.0:
 *   - New subcommands: setlevel, addxp, reset
 *   - `info` and `behavior` now accept either an explicit entityId OR no argument
 *     (in which case the player's raytrace target is used).
 *   - Admin subcommands (setlevel/addxp/reset) accept entity selectors
 *     (`@e[type=wolf]`) so operators can manage multiple dogs at once.
 *
 * Subcommands:
 *   /doglevels info [entityId]                      — prints dog stats to chat
 *   /doglevels behavior <entityId|sel> <mode>       — sets behavior mode
 *   /doglevels setlevel <entityId|sel> <level>      — sets the dog's level (admin)
 *   /doglevels addxp   <entityId|sel> <amount>      — adds XP to the dog (admin)
 *   /doglevels reset   <entityId|sel>               — resets to level 1, 0 XP (admin)
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogCommand
{
    private static final int OP_LEVEL = 2; // vanilla operator permission level for admin cmds

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
            Commands.literal("doglevels")
                // info [entityId]
                .then(Commands.literal("info")
                    .executes(ctx -> showInfoRaytrace(ctx.getSource()))
                    .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .executes(ctx -> showInfo(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "entityId")))
                    )
                )
                // behavior <entityId> <mode>
                .then(Commands.literal("behavior")
                    .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .then(Commands.argument("mode", StringArgumentType.string())
                            .executes(ctx -> setBehavior(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "entityId"),
                                StringArgumentType.getString(ctx, "mode")
                            ))
                        )
                    )
                )
                // setlevel <targets> <level>  (admin)
                .then(Commands.literal("setlevel")
                    .requires(src -> src.hasPermission(OP_LEVEL))
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 1000))
                            .executes(ctx -> setLevel(
                                ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets"),
                                IntegerArgumentType.getInteger(ctx, "level")
                            ))
                        )
                    )
                )
                // addxp <targets> <amount>  (admin)
                .then(Commands.literal("addxp")
                    .requires(src -> src.hasPermission(OP_LEVEL))
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(ctx -> addXP(
                                ctx.getSource(),
                                EntityArgument.getEntities(ctx, "targets"),
                                IntegerArgumentType.getInteger(ctx, "amount")
                            ))
                        )
                    )
                )
                // reset <targets>  (admin)
                .then(Commands.literal("reset")
                    .requires(src -> src.hasPermission(OP_LEVEL))
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> reset(
                            ctx.getSource(),
                            EntityArgument.getEntities(ctx, "targets")
                        ))
                    )
                )
        );
    }

    // ---------- helpers ----------

    /** Find the tamed wolf the player is currently looking at, within 8 blocks. */
    private static Wolf raytraceWolf(ServerPlayer player)
    {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        double reach = 8.0D;
        Vec3 end = eye.add(look.scale(reach));
        AABB box = new AABB(eye, end).inflate(1.0D);
        Wolf best = null;
        double bestDist = reach * reach;
        for (Wolf w : player.level().getEntitiesOfClass(Wolf.class, box, w -> w.isTame() && w.getOwner() == player)) {
            // Pick the wolf whose bounding box is hit first along the ray
            var hit = w.getBoundingBox().clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = w;
                }
            }
        }
        return best;
    }

    private static DogLevelData resolveData(CommandSourceStack source, int entityId)
    {
        Entity entity = source.getLevel().getEntity(entityId);
        if (!(entity instanceof Wolf wolf)) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_wolf", entityId));
            return null;
        }
        if (!wolf.isTame()) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_tamed"));
            return null;
        }
        var executor = source.getEntity();
        if (executor == null || wolf.getOwner() != executor) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_owner"));
            return null;
        }
        return CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).orElseGet(() -> {
            source.sendFailure(Component.translatable("doglevels.cmd.no_data"));
            return null;
        });
    }

    // ---------- info ----------

    private static int showInfoRaytrace(CommandSourceStack source)
    {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("doglevels.cmd.player_only"));
            return 0;
        }
        Wolf wolf = raytraceWolf(player);
        if (wolf == null) {
            source.sendFailure(Component.translatable("doglevels.cmd.no_target"));
            return 0;
        }
        return showInfoFor(source, wolf);
    }

    private static int showInfo(CommandSourceStack source, int entityId)
    {
        Entity entity = source.getLevel().getEntity(entityId);
        if (!(entity instanceof Wolf wolf)) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_wolf", entityId));
            return 0;
        }
        return showInfoFor(source, wolf);
    }

    private static int showInfoFor(CommandSourceStack source, Wolf wolf)
    {
        return CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL)
            .map(data -> {
                Component line;
                if (data.isMaxLevel()) {
                    line = Component.translatable("doglevels.cmd.info.max",
                            data.getLevel(), data.getBehavior().name());
                } else {
                    line = Component.translatable("doglevels.cmd.info.normal",
                            data.getLevel(), data.getXP(), data.xpToNextLevel(), data.getBehavior().name());
                }
                source.sendSuccess(() -> line, false);
                return 1;
            })
            .orElseGet(() -> {
                source.sendFailure(Component.translatable("doglevels.cmd.no_data"));
                return 0;
            });
    }

    // ---------- behavior ----------

    private static int setBehavior(CommandSourceStack source, int entityId, String modeStr)
    {
        DogBehavior mode;
        try {
            mode = DogBehavior.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("doglevels.cmd.invalid_behavior", modeStr));
            return 0;
        }

        Entity entity = source.getLevel().getEntity(entityId);
        if (!(entity instanceof Wolf wolf)) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_wolf", entityId));
            return 0;
        }
        if (!wolf.isTame()) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_tamed"));
            return 0;
        }
        var executor = source.getEntity();
        if (executor == null || wolf.getOwner() != executor) {
            source.sendFailure(Component.translatable("doglevels.cmd.not_owner"));
            return 0;
        }

        return CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL)
            .map(data -> {
                data.setBehavior(mode);
                DogLevelNetwork.sendDogLevelUpdate(wolf);
                source.sendSuccess(() -> Component.translatable(
                        "doglevels.cmd.behavior_set", mode.name()), false);
                return 1;
            })
            .orElseGet(() -> {
                source.sendFailure(Component.translatable("doglevels.cmd.no_data"));
                return 0;
            });
    }

    // ---------- admin commands ----------

    private static int setLevel(CommandSourceStack source, java.util.Collection<? extends Entity> targets, int level)
    {
        int count = 0;
        for (Entity e : targets) {
            if (!(e instanceof Wolf wolf)) continue;
            var dataOpt = CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL);
            if (!dataOpt.isPresent()) continue;
            var data = dataOpt.orElse(null);
            if (data == null) continue;
            data.setLevel(level);
            data.setXP(0);
            data.applyStats(wolf);
            DogLevelNetwork.sendDogLevelUpdate(wolf);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable("doglevels.cmd.setlevel.done", finalCount, level), true);
        return count;
    }

    private static int addXP(CommandSourceStack source, java.util.Collection<? extends Entity> targets, int amount)
    {
        int count = 0;
        for (Entity e : targets) {
            if (!(e instanceof Wolf wolf)) continue;
            var dataOpt = CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL);
            if (!dataOpt.isPresent()) continue;
            var data = dataOpt.orElse(null);
            if (data == null) continue;
            int ups = data.addXP(amount);
            if (ups > 0) data.applyStats(wolf);
            DogLevelNetwork.sendDogLevelUpdate(wolf);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable("doglevels.cmd.addxp.done", finalCount, amount), true);
        return count;
    }

    private static int reset(CommandSourceStack source, java.util.Collection<? extends Entity> targets)
    {
        int count = 0;
        for (Entity e : targets) {
            if (!(e instanceof Wolf wolf)) continue;
            var dataOpt = CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL);
            if (!dataOpt.isPresent()) continue;
            var data = dataOpt.orElse(null);
            if (data == null) continue;
            data.setLevel(1);
            data.setXP(0);
            data.setBehavior(DogBehavior.DEFAULT);
            data.applyStats(wolf);
            DogLevelNetwork.sendDogLevelUpdate(wolf);
            count++;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable("doglevels.cmd.reset.done", finalCount), true);
        return count;
    }
}
