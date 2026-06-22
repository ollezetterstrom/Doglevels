package com.zmod.doglevels.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import com.zmod.doglevels.capability.DogLevelData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the /doglevels command.
 *
 * Subcommands:
 *   /doglevels behavior <entityId> <mode>  — sets behavior mode (DEFAULT, AGGRESSIVE, PASSIVE)
 *   /doglevels info <entityId>             — prints dog stats to the sender's chat
 *
 * The entityId is the wolf's entity ID (visible in the DogScreen UI).
 * The command validates that the entity is a tamed wolf owned by the executing player.
 */
@Mod.EventBusSubscriber(modid = "doglevels", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogCommand
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
            Commands.literal("doglevels")
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
                .then(Commands.literal("info")
                    .then(Commands.argument("entityId", IntegerArgumentType.integer())
                        .executes(ctx -> showInfo(
                            ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "entityId")
                        ))
                    )
                )
        );
    }

    private static int setBehavior(CommandSourceStack source, int entityId, String modeStr)
    {
        DogBehavior mode;
        try {
            mode = DogBehavior.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid behavior mode: " + modeStr +
                    ". Valid: DEFAULT, AGGRESSIVE, PASSIVE"));
            return 0;
        }

        Entity entity = source.getLevel().getEntity(entityId);
        if (!(entity instanceof Wolf wolf)) {
            source.sendFailure(Component.literal("Entity " + entityId + " is not a wolf."));
            return 0;
        }
        if (!wolf.isTame()) {
            source.sendFailure(Component.literal("That wolf is not tamed."));
            return 0;
        }
        var executor = source.getEntity();
        if (executor == null || wolf.getOwner() != executor) {
            source.sendFailure(Component.literal("You don't own that wolf."));
            return 0;
        }

        return CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL)
            .map(data -> {
                data.setBehavior(mode);
                source.sendSuccess(() -> Component.literal(
                    "Dog behavior set to: " + mode.name()), false);
                return 1;
            })
            .orElseGet(() -> {
                source.sendFailure(Component.literal("Dog has no level data."));
                return 0;
            });
    }

    private static int showInfo(CommandSourceStack source, int entityId)
    {
        Entity entity = source.getLevel().getEntity(entityId);
        if (!(entity instanceof Wolf wolf)) {
            source.sendFailure(Component.literal("Entity " + entityId + " is not a wolf."));
            return 0;
        }
        return CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL)
            .map(data -> {
                source.sendSuccess(() -> Component.literal(
                    "Dog Lv " + data.getLevel() +
                    (data.isMaxLevel() ? " (MAX)" : " (" + data.getXP() + "/" + data.xpToNextLevel() + " XP)") +
                    " | Behavior: " + data.getBehavior().name()
                ), false);
                return 1;
            })
            .orElse(0);
    }
}
