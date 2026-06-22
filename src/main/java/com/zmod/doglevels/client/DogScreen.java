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
import java.util.Locale;

/**
 * A clean Screen for viewing and managing a dog, with a scrollable stats panel.
 *
 * CHANGES IN 1.4.0:
 *   - Reads level/XP/behavior from {@link ClientDogLevelCache} (synced via the
 *     new {@link com.zmod.doglevels.network.DogLevelSyncMessage}) instead of
 *     inferring the level from the synced MAX_HEALTH attribute.
 *   - Removed the broken static {@code predictedBehavior} field (it was shared
 *     across all dogs and would show the wrong initial value for dog B if you
 *     had cycled dog A's button).
 *   - Added an XP progress bar.
 *   - All {@code String.format} calls now use {@link Locale#ROOT} (was using
 *     the default locale, which produced comma decimals on EU clients).
 *   - Falls back to attribute-derived level if no sync packet has arrived yet
 *     (so the screen still works on a vanilla server without the mod).
 *
 * Layout (positions computed from screen dimensions — nothing hardcoded):
 *
 *   ┌─────────────────────────────────────┐
 *   │              Dog Stats               │  ← title (fixed at top)
 *   ├─────────────────────────────────────┤
 *   │  === Stats ===                      │  ← scrollable viewport
 *   │  Level: 15  [████░░░░] 12/30 XP      │     (clipped with scissoring)
 *   │  Health: 34 / 35                    │
 *   │  ...                                 │
 *   ├─────────────────────────────────────┤
 *   │      [ Behavior: DEFAULT ]          │  ← behavior button (fixed)
 *   │            [ Done ]                  │  ← done button (fixed)
 *   └─────────────────────────────────────┘
 */
public class DogScreen extends Screen
{
    // Layout constants (pixels in GUI scale)
    private static final int LINE_HEIGHT = 12;
    private static final int TOP_PADDING = 16;
    private static final int TITLE_HEIGHT = 16;
    private static final int VIEWPORT_TOP_GAP = 8;
    private static final int VIEWPORT_BOTTOM_GAP = 8;
    private static final int BUTTON_SPACING = 6;
    private static final int BOTTOM_PADDING = 16;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DONE_WIDTH = 100;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int VIEWPORT_PADDING_X = 16;
    private static final int SCROLL_STEP = LINE_HEIGHT * 2;

    // XP bar dimensions
    private static final int XP_BAR_WIDTH = 120;
    private static final int XP_BAR_HEIGHT = 6;
    private static final int XP_BLOCK_HEIGHT = XP_BAR_HEIGHT + 2 + 11 + 4; // bar + gap + label + gap below
    private static final int XP_BAR_COLOR_BG     = 0xFF202020;
    private static final int XP_BAR_COLOR_FILL   = 0xFF7FE47F;
    private static final int XP_BAR_COLOR_BORDER = 0xFFA0A0A0;

    private final int wolfEntityId;
    private CycleButton<DogBehavior> behaviorButton;
    private Button doneButton;

    // Scroll state
    private int scrollOffset = 0;
    private int contentHeight = 0;

    // Computed layout positions
    private int titleY;
    private int viewportX, viewportY, viewportW, viewportH;
    private int buttonY, doneY;

    public DogScreen(int wolfEntityId)
    {
        super(Component.translatable("doglevels.screen.title"));
        this.wolfEntityId = wolfEntityId;
    }

    private Wolf getWolf()
    {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        var entity = level.getEntity(wolfEntityId);
        return entity instanceof Wolf wolf ? wolf : null;
    }

    /**
     * Returns the synced entry from {@link ClientDogLevelCache}, or {@code null}
     * if no sync packet has been received yet (e.g. on a vanilla server).
     */
    private ClientDogLevelCache.Entry syncedData()
    {
        return ClientDogLevelCache.get(wolfEntityId);
    }

    /** Fallback level inference from the synced MAX_HEALTH attribute. */
    private int computeLevelFromAttributes(Wolf wolf)
    {
        if (wolf == null) return 1;
        AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return 1;
        AttributeModifier mod = healthAttr.getModifier(DogLevelData.HEALTH_MOD_ID);
        if (mod == null) return 1;
        double healthPerLevel = DogLevelsConfig.getDouble(DogLevelsConfig.HEALTH_PER_LEVEL, 1.0);
        if (healthPerLevel <= 0) return 1;
        return 1 + (int) Math.round(mod.amount() / healthPerLevel);
    }

    private List<Component> buildLines(Wolf wolf, ClientDogLevelCache.Entry sync)
    {
        List<Component> lines = new ArrayList<>();
        if (wolf == null) {
            lines.add(Component.translatable("doglevels.screen.not_found").withStyle(ChatFormatting.RED));
            return lines;
        }

        int level = sync != null ? sync.level() : computeLevelFromAttributes(wolf);
        int maxLevel = sync != null ? sync.maxLevel() : DogLevelData.getMaxLevel();
        boolean isMax = level >= maxLevel;

        lines.add(Component.translatable("doglevels.screen.stats_header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.translatable("doglevels.screen.level", level,
                isMax ? Component.translatable("doglevels.screen.max").withStyle(ChatFormatting.GOLD) : Component.literal("")));
        lines.add(Component.literal(String.format(Locale.ROOT, "%.0f / %.0f",
                wolf.getHealth(), wolf.getMaxHealth())));
        lines.add(Component.literal(String.format(Locale.ROOT, "%.1f",
                attrValue(wolf, Attributes.ATTACK_DAMAGE))));
        lines.add(Component.literal(String.format(Locale.ROOT, "%.3f",
                attrValue(wolf, Attributes.MOVEMENT_SPEED))));
        lines.add(Component.literal(String.format(Locale.ROOT, "%.1f",
                attrValue(wolf, Attributes.ARMOR))));

        double perLevel = DogLevelsConfig.getDouble(DogLevelsConfig.SIZE_PER_LEVEL, 0.015);
        double maxBonus = DogLevelsConfig.getDouble(DogLevelsConfig.MAX_SIZE_BONUS, 0.6);
        double bonus = Math.min(maxBonus, perLevel * (level - 1));
        lines.add(Component.literal(String.format(Locale.ROOT, "%.0f%%", (1.0 + bonus) * 100)));

        lines.add(Component.empty());

        lines.add(Component.translatable("doglevels.screen.abilities_header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(abilityLine("doglevels.ability.splash_bite",       level >= DogAbilities.splashBiteLevel(),      DogAbilities.splashBiteLevel()));
        lines.add(abilityLine("doglevels.ability.fire_resist",       level >= DogAbilities.fireResistLevel(),      DogAbilities.fireResistLevel()));
        lines.add(abilityLine("doglevels.ability.pack_hunter",       level >= DogAbilities.packHunterLevel(),      DogAbilities.packHunterLevel()));
        lines.add(abilityLine("doglevels.ability.howl_of_vitality",  level >= DogAbilities.howlOfVitalityLevel(),  DogAbilities.howlOfVitalityLevel()));
        lines.add(abilityLine("doglevels.ability.lifesteal",         level >= DogAbilities.lifestealLevel(),       DogAbilities.lifestealLevel()));
        lines.add(abilityLine("doglevels.ability.bonded_endurance",  level >= DogAbilities.bondedEnduranceLevel(), DogAbilities.bondedEnduranceLevel()));

        return lines;
    }

    private static Component abilityLine(String nameKey, boolean unlocked, int reqLevel)
    {
        if (unlocked) {
            return Component.translatable("doglevels.screen.ability_unlocked",
                    Component.translatable(nameKey)).withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("doglevels.screen.ability_locked",
                Component.translatable(nameKey), reqLevel).withStyle(ChatFormatting.DARK_GRAY);
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

        // Layout (top → bottom):
        //   TOP_PADDING
        //   TITLE_HEIGHT (title text)
        //   XP_BLOCK_HEIGHT (reserved for XP bar + label; only drawn when synced & not max)
        //   VIEWPORT_TOP_GAP
        //   <viewport>   (flexible)
        //   VIEWPORT_BOTTOM_GAP
        //   BUTTON_HEIGHT (behavior)
        //   BUTTON_SPACING
        //   BUTTON_HEIGHT (done)
        //   BOTTOM_PADDING
        titleY = TOP_PADDING;
        int afterTitle = titleY + TITLE_HEIGHT + XP_BLOCK_HEIGHT + VIEWPORT_TOP_GAP;
        int viewportTop = afterTitle;

        doneY = this.height - BOTTOM_PADDING - BUTTON_HEIGHT;
        buttonY = doneY - BUTTON_SPACING - BUTTON_HEIGHT;

        int viewportBottom = buttonY - VIEWPORT_BOTTOM_GAP;
        viewportY = viewportTop;
        viewportH = viewportBottom - viewportTop;

        viewportW = Math.min(this.width - 40, 280);
        viewportX = cx - viewportW / 2;

        // Behavior cycle button — initial value from synced data (or DEFAULT if no sync yet)
        ClientDogLevelCache.Entry sync = syncedData();
        DogBehavior initial = sync != null ? sync.behavior() : DogBehavior.DEFAULT;

        behaviorButton = CycleButton.<DogBehavior>builder(
                (DogBehavior mode) -> Component.translatable("doglevels.screen.behavior", mode.name()))
                .withValues(DogBehavior.DEFAULT, DogBehavior.AGGRESSIVE, DogBehavior.PASSIVE)
                .withInitialValue(initial)
                .create(cx - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("doglevels.screen.behavior_label"),
                        (button, mode) -> onBehaviorChanged(mode));
        this.addRenderableWidget(behaviorButton);

        doneButton = Button.builder(Component.translatable("doglevels.screen.done"), b -> onClose())
                .bounds(cx - DONE_WIDTH / 2, doneY, DONE_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(doneButton);
    }

    private void onBehaviorChanged(DogBehavior newMode)
    {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendCommand("doglevels behavior " + wolfEntityId + " " + newMode.name());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.getTitle(), cx, titleY, 0xFFFFFF);

        Wolf wolf = getWolf();
        ClientDogLevelCache.Entry sync = syncedData();
        List<Component> lines = buildLines(wolf, sync);

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

        int textX = cx;
        int y = viewportY - scrollOffset;
        for (Component line : lines) {
            int lineH = line.getString().isEmpty() ? LINE_HEIGHT / 2 : LINE_HEIGHT;
            if (y + lineH > viewportY && y < viewportY + viewportH) {
                if (!line.getString().isEmpty()) {
                    graphics.drawCenteredString(this.font, line, textX, y, 0xFFFFFF);
                }
            }
            y += lineH;
        }

        graphics.disableScissor();

        // XP bar (drawn in the reserved block between title and viewport — always visible, not scrolled)
        if (sync != null && !sync.isMaxLevel()) {
            int barX = cx - XP_BAR_WIDTH / 2;
            int barY = titleY + TITLE_HEIGHT + 2; // 2px gap below the title text
            float progress = sync.progress();
            drawXpBar(graphics, barX, barY, XP_BAR_WIDTH, XP_BAR_HEIGHT, progress);
            graphics.drawCenteredString(this.font,
                    Component.literal(sync.xp() + "/" + sync.xpToNext()),
                    cx, barY + XP_BAR_HEIGHT + 2, 0xA0FFFFFF);
        }

        // Draw scrollbar if content overflows
        if (contentHeight > viewportH) {
            int scrollbarX = viewportX + viewportW - SCROLLBAR_WIDTH;
            int scrollbarH = viewportH;
            graphics.fill(scrollbarX, viewportY, scrollbarX + SCROLLBAR_WIDTH, viewportY + scrollbarH, 0x40808080);
            int thumbH = Math.max(20, scrollbarH * viewportH / contentHeight);
            int thumbY = viewportY + (scrollbarH - thumbH) * scrollOffset / maxScroll;
            graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbH, 0xC0A0A0A0);
        }
    }

    private static void drawXpBar(GuiGraphics g, int x, int y, int w, int h, float progress)
    {
        // Background
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, XP_BAR_COLOR_BORDER);
        g.fill(x, y, x + w, y + h, XP_BAR_COLOR_BG);
        // Fill
        int fillW = (int) Math.max(0, Math.min(w, w * progress));
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + h, XP_BAR_COLOR_FILL);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (scrollY != 0) {
            int delta = (int) (-scrollY * SCROLL_STEP);
            scrollOffset += delta;
            int maxScroll = Math.max(0, contentHeight - viewportH);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
