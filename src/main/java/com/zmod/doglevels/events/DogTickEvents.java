package com.zmod.doglevels.events;

import com.zmod.doglevels.DogLevelsMod;
import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.CapabilityHelper;
import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelCapabilities;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import com.zmod.doglevels.items.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Per-tick handler that maintains ability effects and behavior modes.
 *
 * CHANGES IN 1.4.0:
 *   - {@link #howlCooldowns} is now a {@link ConcurrentHashMap} and entries are
 *     removed when the wolf leaves the level (was an unbounded HashMap leak).
 *   - Howl period is read from config (was hardcoded 5s).
 *   - AGGRESSIVE target acquisition now uses the {@link Enemy} interface
 *     (was instanceof Monster, which missed Slimes/MagmaCubes/etc.).
 *   - AGGRESSIVE range is read from config (was hardcoded 16).
 *
 * Behavior modes:
 *   DEFAULT: Vanilla wolf behavior (only attacks what the player attacks)
 *   AGGRESSIVE: Auto-targets nearby hostile mobs (Enemy) within configurable range
 *   PASSIVE: Clears target every tick; never attacks
 */
@Mod.EventBusSubscriber(modid = DogLevelsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DogTickEvents
{
    /** Per-wolf cooldown tracking for the Pack Hunter howl, in game ticks. */
    private static final Map<UUID, Long> howlCooldowns = new ConcurrentHashMap<>();

    /**
     * In-memory (non-persistent) set of wolf UUIDs that have already had the
     * Treat TemptGoal added during this session. Cleared on world unload so
     * the goal gets re-added correctly after a world reload (because the
     * goalSelector is rebuilt on entity load).
     */
    private static final java.util.Set<UUID> temptGoalAdded = ConcurrentHashMap.newKeySet();

    /** Tick counter — used as the time source for cooldowns (much cheaper than System.currentTimeMillis()). */
    private static long tickCounter = 0L;

    public static void register() {}

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Wolf wolf)) return;

        // QoL: add a TemptGoal so tamed AND untamed wolves are attracted to Treats
        // (same way vanilla wolves are attracted to bones). Added once per session
        // per wolf; the goalSelector is rebuilt when the entity is loaded from NBT,
        // so we re-add on every EntityJoinLevelEvent if our in-memory flag is clear.
        UUID id = wolf.getUUID();
        if (temptGoalAdded.add(id)) { // returns true if newly added
            Predicate<ItemStack> isTreat = stack -> stack != null && stack.getItem() == ModItems.TREAT_ITEM.get();
            // Priority 5 matches vanilla's bone TemptGoal so they compete evenly.
            wolf.goalSelector.addGoal(5, new TemptGoal(wolf, 1.0D, isTreat, false));
        }

        if (!wolf.isTame()) return;

        CapabilityHelper.getCapability(wolf, DogLevelCapabilities.DOG_LEVEL).ifPresent(data -> {
            data.applyStats(wolf);
        });
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event)
    {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        // Clean up cooldown entry to avoid unbounded map growth (1.4.0 leak fix).
        howlCooldowns.remove(wolf.getUUID());
        // Don't remove from temptGoalAdded here — we only want to re-add the goal
        // when the entity is reloaded from NBT (i.e. world reload), not when it
        // changes dimensions or chunks unload.
    }

    /**
     * Called from DogLevelsMod on server stop to reset all session state so a
     * subsequent world load re-adds TemptGoals correctly.
     */
    public static void clearSessionState()
    {
        howlCooldowns.clear();
        temptGoalAdded.clear();
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

        tickCounter++;

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

                // Throttled howl — uses game ticks (not wall clock) so pauses with /tick freeze
                int howlPeriod = DogLevelsConfig.getInt(DogLevelsConfig.PACK_HUNTER_PERIOD_TICKS, 100) / 20;
                if (howlPeriod < 1) howlPeriod = 5; // safety floor (5 player-ticks = ~1s minimum re-eval)
                if (DogAbilities.hasPackHunter(data) && shouldHowl(wolf.getUUID(), howlPeriod)) {
                    DogAbilities.doPackHunterHowl(wolf);
                }

                // Apply behavior mode
                applyBehavior(wolf, data);
            });
        }
    }

    private static boolean shouldHowl(UUID id, int periodInTicks)
    {
        long now = tickCounter;
        Long last = howlCooldowns.get(id);
        if (last == null || (now - last) >= periodInTicks) {
            howlCooldowns.put(id, now);
            return true;
        }
        return false;
    }

    /**
     * Apply the dog's behavior mode:
     *   AGGRESSIVE: If no current target, scan for nearby Enemy and set as target.
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

                double range = DogLevelsConfig.getDouble(DogLevelsConfig.AGGRESSIVE_RANGE, 16.0);
                var searchBox = wolf.getBoundingBox().inflate(range);
                List<LivingEntity> hostiles = wolf.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        m -> m.isAlive()
                                && m.isAttackable()
                                && m instanceof Enemy
                                && wolf.canAttack(m)
                                && wolf.distanceToSqr(m) < range * range);
                if (!hostiles.isEmpty()) {
                    // Pick the closest
                    LivingEntity nearest = null;
                    double minDist = Double.MAX_VALUE;
                    for (LivingEntity m : hostiles) {
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
