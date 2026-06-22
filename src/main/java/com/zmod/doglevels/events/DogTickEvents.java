package com.zmod.doglevels.events;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import com.zmod.doglevels.capability.DogLevelData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-tick handler that maintains ability effects and behavior modes.
 *
 * Behavior modes:
 *   DEFAULT: Vanilla wolf behavior (only attacks what the player attacks)
 *   AGGRESSIVE: Auto-targets nearby hostile mobs (Monster) within 16 blocks
 *   PASSIVE: Clears target every tick; never attacks
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogTickEvents
{
    private static final Map<UUID, Long> howlCooldowns = new HashMap<>();
    private static final long HOWL_PERIOD_MS = 5_000L;
    private static final double AGGRO_RANGE = 16.0D;

    public static void register() {}

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        if (!wolf.isTame()) return;

        CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
            data.applyStats(wolf);
        });
    }

    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getAnimal() instanceof Wolf wolf)) return;

        CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
            if (!data.isStatsApplied()) {
                data.setLevel(1);
                data.setXP(0);
            }
            data.applyStats(wolf);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        var player = event.player;
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        var aabb = player.getBoundingBox().inflate(64.0D);
        var wolves = player.level().getEntitiesOfClass(Wolf.class, aabb, w -> w.isTame() && w.getOwner() == player);

        for (Wolf wolf : wolves) {
            CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
                // Ensure stats are applied
                if (!data.isStatsApplied()) {
                    data.applyStats(wolf);
                }

                // Apply passive abilities
                if (DogAbilities.hasFireResistance(data)) {
                    DogAbilities.applyFireResistance(wolf);
                }
                if (DogAbilities.hasBondedEndurance(data)) {
                    DogAbilities.applyBondedEndurance(wolf);
                }

                // Throttled howl
                UUID id = wolf.getUUID();
                long now = System.currentTimeMillis();
                Long last = howlCooldowns.get(id);
                if (last == null || now - last >= HOWL_PERIOD_MS) {
                    if (DogAbilities.hasPackHunter(data)) {
                        DogAbilities.doPackHunterHowl(wolf);
                    }
                    howlCooldowns.put(id, now);
                }

                // Apply behavior mode
                applyBehavior(wolf, data);
            });
        }
    }

    /**
     * Apply the dog's behavior mode:
     *   AGGRESSIVE: If no current target, scan for nearby Monster and set as target.
     *   PASSIVE: Clear target.
     *   DEFAULT: Do nothing (vanilla handles it).
     */
    private static void applyBehavior(Wolf wolf, DogLevelData data)
    {
        DogBehavior mode = data.getBehavior();
        switch (mode) {
            case AGGRESSIVE -> {
                // Only acquire a new target if we don't already have one
                LivingEntity currentTarget = wolf.getTarget();
                if (currentTarget != null && currentTarget.isAlive()) return;

                var searchBox = wolf.getBoundingBox().inflate(AGGRO_RANGE);
                List<Monster> monsters = wolf.level().getEntitiesOfClass(Monster.class, searchBox,
                        m -> m.isAlive() && wolf.canAttack(m) && wolf.distanceToSqr(m) < AGGRO_RANGE * AGGRO_RANGE);
                if (!monsters.isEmpty()) {
                    // Pick the closest
                    Monster nearest = null;
                    double minDist = Double.MAX_VALUE;
                    for (Monster m : monsters) {
                        double d = wolf.distanceToSqr(m);
                        if (d < minDist) {
                            minDist = d;
                            nearest = m;
                        }
                    }
                    if (nearest != null) {
                        wolf.setTarget(nearest);
                    }
                }
            }
            case PASSIVE -> {
                if (wolf.getTarget() != null) {
                    wolf.setTarget(null);
                }
            }
            // DEFAULT: do nothing
        }
    }
}
