package com.zmod.doglevels.capability;

/**
 * Behavior modes for tamed dogs.
 *
 * DEFAULT: Vanilla wolf behavior — only attacks what the player attacks or what attacks the player.
 * AGGRESSIVE: Automatically targets and attacks nearby hostile mobs within 16 blocks.
 * PASSIVE: Never attacks anything; clears target every tick.
 */
public enum DogBehavior
{
    DEFAULT,
    AGGRESSIVE,
    PASSIVE
}
