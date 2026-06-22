package com.zmod.doglevels.client;

import com.zmod.doglevels.abilities.DogAbilities;
import com.zmod.doglevels.capability.DogBehavior;
import com.zmod.doglevels.capability.DogLevelData;
import com.zmod.doglevels.config.DogLevelsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;

import java.util.ArrayList;
import java.util.List;

/**
 * A clean Screen for viewing and managing a dog, with a scrollable stats panel.
 *
 * Layout (positions computed from screen dimensions — nothing hardcoded):
 *
 *   ┌─────────────────────────────────────┐
 *   │              Dog Stats               │  ← title (fixed at top)
 *   ├─────────────────────────────────────┤
 *   │  === Stats ===                      │  ← scrollable viewport
 *   │  Level: 15                          │     (clipped with scissoring)
 *   │  Health: 34 / 35                    │
 *   │  ...                                 │
 *   │  === Abilities ===                  │
 *   │  ✓ Splash Bite                      │
 *   │  ...                                 │  ← scrollbar on the right
 *   ├─────────────────────────────────────┤
 *   │      [ Behavior: DEFAULT ]          │  ← behavior button (fixed)
 *   │            [ Done ]                  │  ← done button (fixed)
 *   └─────────────────────────────────────┘
 *
 * The background is a semi-transparent dark overlay so you can see the game
 * world behind the screen (like the chat screen or sign screen).
 */
public class DogScreen extends Screen
{
    // Layout constants (pixels in GUI scale)
    private static final int LINE_HEIGHT = 12;
    private static final int TOP_PADDING = 16;          // above title
    private static final int TITLE_HEIGHT = 16;         // title text height
    private static final int VIEWPORT_TOP_GAP = 8;      // gap between title and viewport
    private static final int VIEWPORT_BOTTOM_GAP = 8;   // gap between viewport and buttons
    private static final int BUTTON_SPACING = 6;        // between behavior and done
    private static final int BOTTOM_PADDING = 16;       // below done button
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DONE_WIDTH = 100;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int VIEWPORT_PADDING_X = 16;   // horizontal padding inside viewport
    private static final int SCROLL_STEP = LINE_HEIGHT * 2; // pixels per scroll wheel click

    private final int wolfEntityId;
    private CycleButton<DogBehavior> behaviorButton;
    private Button doneButton;

    // Predicted behavior mode, remembered across screen opens.
    // STATIC so that closing and reopening the screen (e.g. shift+right-clicking
    // the dog again) preserves the last-selected mode. The server is authoritative,
    // but we don't sync the mode to the client — so we use this local prediction
    // for the cycle button's initial display.
    private static DogBehavior predictedBehavior = DogBehavior.DEFAULT;

    // Scroll state
    private int scrollOffset = 0;
    private int contentHeight = 0;  // total height of all text lines (updated each frame)

    // Computed layout positions (set in init, used in render)
    private int titleY;
    private int viewportX, viewportY, viewportW, viewportH;
    private int buttonY, doneY;

    public DogScreen(int wolfEntityId)
    {
        super(Component.literal("Dog Stats"));
        this.wolfEntityId = wolfEntityId;
    }

    private Wolf getWolf()
    {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        var entity = level.getEntity(wolfEntityId);
        return entity instanceof Wolf wolf ? wolf : null;
    }

    private int computeLevel(Wolf wolf)
    {
        AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return 1;
        AttributeModifier mod = healthAttr.getModifier(DogLevelData.HEALTH_MOD_ID);
        if (mod == null) return 1;
        double healthPerLevel = cfg(DogLevelsConfig.HEALTH_PER_LEVEL, 1.0);
        if (healthPerLevel <= 0) return 1;
        return 1 + (int) Math.round(mod.amount() / healthPerLevel);
    }

    private List<Component> buildLines(Wolf wolf)
    {
        List<Component> lines = new ArrayList<>();
        if (wolf == null) {
            lines.add(Component.literal("Dog not found").withStyle(ChatFormatting.RED));
            return lines;
        }

        int level = computeLevel(wolf);

        lines.add(Component.literal("=== Stats ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.literal("Level: " + level + (level >= DogLevelData.MAX_LEVEL ? " (MAX)" : "")));
        lines.add(Component.literal(String.format("Health: %.0f / %.0f", wolf.getHealth(), wolf.getMaxHealth())));
        lines.add(Component.literal(String.format("Damage: %.1f", attrValue(wolf, Attributes.ATTACK_DAMAGE))));
        lines.add(Component.literal(String.format("Speed: %.3f", attrValue(wolf, Attributes.MOVEMENT_SPEED))));
        lines.add(Component.literal(String.format("Armor: %.1f", attrValue(wolf, Attributes.ARMOR))));

        double perLevel = cfg(DogLevelsConfig.SIZE_PER_LEVEL, 0.015);
        double maxBonus = cfg(DogLevelsConfig.MAX_SIZE_BONUS, 0.6);
        double bonus = Math.min(maxBonus, perLevel * (level - 1));
        lines.add(Component.literal(String.format("Size: %.0f%%", (1.0 + bonus) * 100)));

        lines.add(Component.empty());

        lines.add(Component.literal("=== Abilities ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(abilityLine("Splash Bite", level >= DogAbilities.SPLASH_BITE_LEVEL, DogAbilities.SPLASH_BITE_LEVEL));
        lines.add(abilityLine("Fire Resistance", level >= DogAbilities.FIRE_RESIST_LEVEL, DogAbilities.FIRE_RESIST_LEVEL));
        lines.add(abilityLine("Pack Hunter", level >= DogAbilities.PACK_HUNTER_LEVEL, DogAbilities.PACK_HUNTER_LEVEL));
        lines.add(abilityLine("Howl of Vitality", level >= DogAbilities.HOWL_OF_VITALITY_LEVEL, DogAbilities.HOWL_OF_VITALITY_LEVEL));
        lines.add(abilityLine("Lifesteal", level >= DogAbilities.LIFESTEAL_LEVEL, DogAbilities.LIFESTEAL_LEVEL));
        lines.add(abilityLine("Bonded Endurance", level >= DogAbilities.BONDED_ENDURANCE_LEVEL, DogAbilities.BONDED_ENDURANCE_LEVEL));

        return lines;
    }

    private static Component abilityLine(String name, boolean unlocked, int reqLevel)
    {
        if (unlocked) {
            return Component.literal("  ✓ " + name).withStyle(ChatFormatting.GREEN);
        }
        return Component.literal("  ✗ " + name + " (Lv " + reqLevel + ")").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static double attrValue(Wolf wolf, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr)
    {
        AttributeInstance inst = wolf.getAttribute(attr);
        return inst != null ? inst.getValue() : 0.0;
    }

    @Override
    protected void init()
    {
        super.init();

        int cx = this.width / 2;

        // Compute vertical layout from top to bottom
        titleY = TOP_PADDING;
        int viewportTop = titleY + TITLE_HEIGHT + VIEWPORT_TOP_GAP;

        // Footer: done button at bottom, behavior button above it
        doneY = this.height - BOTTOM_PADDING - BUTTON_HEIGHT;
        buttonY = doneY - BUTTON_SPACING - BUTTON_HEIGHT;

        int viewportBottom = buttonY - VIEWPORT_BOTTOM_GAP;
        viewportY = viewportTop;
        viewportH = viewportBottom - viewportTop;

        // Viewport width: centered, with some max width
        viewportW = Math.min(this.width - 40, 280);
        viewportX = cx - viewportW / 2;

        // Behavior cycle button
        behaviorButton = CycleButton.<DogBehavior>builder(
                (DogBehavior mode) -> Component.literal("Behavior: " + mode.name()))
                .withValues(DogBehavior.DEFAULT, DogBehavior.AGGRESSIVE, DogBehavior.PASSIVE)
                .withInitialValue(predictedBehavior)
                .create(cx - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Behavior"),
                        (button, mode) -> onBehaviorChanged(mode));
        this.addRenderableWidget(behaviorButton);

        // Done button
        doneButton = Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - DONE_WIDTH / 2, doneY, DONE_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(doneButton);
    }

    private void onBehaviorChanged(DogBehavior newMode)
    {
        predictedBehavior = newMode;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendCommand("doglevels behavior " + wolfEntityId + " " + newMode.name());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        // Override the default dirt/blur background with a simple semi-transparent overlay.
        // This prevents the default background from being drawn ON TOP of our content
        // (which happens if super.render() calls the default renderBackground).
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        // super.render() calls our overridden renderBackground (the overlay) + renders buttons.
        // We call it FIRST so the title and viewport content are drawn ON TOP,
        // not behind the background.
        super.render(graphics, mouseX, mouseY, partialTick);

        // Title
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.getTitle(), cx, titleY, 0xFFFFFF);

        // Build the lines (live data)
        Wolf wolf = getWolf();
        List<Component> lines = buildLines(wolf);

        // Compute content height
        contentHeight = 0;
        for (Component line : lines) {
            contentHeight += line.getString().isEmpty() ? LINE_HEIGHT / 2 : LINE_HEIGHT;
        }

        // Clamp scroll offset
        int maxScroll = Math.max(0, contentHeight - viewportH);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // Render the scrollable content with scissoring
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportW, viewportY + viewportH);

        int textX = cx;  // centered
        int y = viewportY - scrollOffset;
        for (Component line : lines) {
            int lineH = line.getString().isEmpty() ? LINE_HEIGHT / 2 : LINE_HEIGHT;
            // Only render if at least partially visible
            if (y + lineH > viewportY && y < viewportY + viewportH) {
                if (!line.getString().isEmpty()) {
                    graphics.drawCenteredString(this.font, line, textX, y, 0xFFFFFF);
                }
            }
            y += lineH;
        }

        graphics.disableScissor();

        // Draw scrollbar if content overflows
        if (contentHeight > viewportH) {
            int scrollbarX = viewportX + viewportW - SCROLLBAR_WIDTH;
            int scrollbarH = viewportH;
            // Track background
            graphics.fill(scrollbarX, viewportY, scrollbarX + SCROLLBAR_WIDTH, viewportY + scrollbarH, 0x40808080);
            // Thumb
            int thumbH = Math.max(20, scrollbarH * viewportH / contentHeight);
            int thumbY = viewportY + (scrollbarH - thumbH) * scrollOffset / maxScroll;
            graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbH, 0xC0A0A0A0);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        // scrollY is positive when scrolling up, negative when scrolling down
        if (scrollY != 0) {
            int delta = (int) (-scrollY * SCROLL_STEP);
            scrollOffset += delta;
            // Clamp
            int maxScroll = Math.max(0, contentHeight - viewportH);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static double cfg(net.minecraftforge.common.ForgeConfigSpec.DoubleValue v, double fallback)
    {
        try {
            return v != null && v.get() != null ? v.get() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
