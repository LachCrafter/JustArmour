package com.coriander.justarmour;

import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class HudEditorScreen extends Screen {
    private int armorDragOffsetX, armorDragOffsetY;
    private int heldDragOffsetX, heldDragOffsetY;
    private int offhandDragOffsetX, offhandDragOffsetY;
    private boolean draggingArmor = false;
    private boolean draggingHeldItem = false;
    private boolean draggingOffhandItem = false;
    private static final int MOUSE_LEFTCLICK = 1;

    protected HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();

        // Register mouse event handlers using Fabric API
        ScreenMouseEvents.beforeMouseClick(this).register((screen, click) -> {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();

            if (button == MOUSE_LEFTCLICK) {
                // Check offhand item box first
                if (shouldShowOffhandItem() && isInsideOffhandItemBox(mouseX, mouseY)) {
                    draggingOffhandItem = true;
                    offhandDragOffsetX = (int) mouseX - JustArmourClient.config.offhandItemX;
                    offhandDragOffsetY = (int) mouseY - JustArmourClient.config.offhandItemY;
                    return;
                }

                // Check held item box
                if (shouldShowHeldItem() && isInsideHeldItemBox(mouseX, mouseY)) {
                    draggingHeldItem = true;
                    heldDragOffsetX = (int) mouseX - JustArmourClient.config.heldItemX;
                    heldDragOffsetY = (int) mouseY - JustArmourClient.config.heldItemY;
                    return;
                }

                // Check armor box
                if (isInsideArmorBox(mouseX, mouseY)) {
                    draggingArmor = true;
                    armorDragOffsetX = (int) mouseX - JustArmourClient.config.hudX;
                    armorDragOffsetY = (int) mouseY - JustArmourClient.config.hudY;
                }
            }
        });

        ScreenMouseEvents.beforeMouseRelease(this).register((screen, click) -> {
            int button = click.button();
            if (button == MOUSE_LEFTCLICK && (draggingArmor || draggingHeldItem || draggingOffhandItem)) {
                draggingArmor = false;
                draggingHeldItem = false;
                draggingOffhandItem = false;
                JustArmourClient.saveConfig();
            }
        });
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Handle dragging
        if (draggingArmor) {
            JustArmourClient.config.hudX = (mouseX - armorDragOffsetX);
            JustArmourClient.config.hudY = (mouseY - armorDragOffsetY);
        } else if (draggingHeldItem) {
            JustArmourClient.config.heldItemX = (mouseX - heldDragOffsetX);
            JustArmourClient.config.heldItemY = (mouseY - heldDragOffsetY);
        } else if (draggingOffhandItem) {
            JustArmourClient.config.offhandItemX = (mouseX - offhandDragOffsetX);
            JustArmourClient.config.offhandItemY = (mouseY - offhandDragOffsetY);
        }

        // Draw box around armor HUD area
        drawArmorBox(graphics);

        // Draw box around held item if visible
        if (shouldShowHeldItem()) {
            drawHeldItemBox(graphics);
        }

        // Draw box around offhand item if visible
        if (shouldShowOffhandItem()) {
            drawOffhandItemBox(graphics);
        }

        // Render the armor HUD
        JustArmourClient.renderArmorHUD(graphics, JustArmourClient.config.hudX, JustArmourClient.config.hudY);

        // Render held item HUD
        JustArmourClient.renderHeldItemHUD(graphics);

        // Render offhand item HUD
        JustArmourClient.renderOffhandItemHUD(graphics);
    }

    private boolean shouldShowHeldItem() {
        if (!JustArmourClient.config.showHeldItem) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        ItemStack heldItem = client.player.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        return JustArmourClient.config.showAllHeldItems || heldItem.isDamageableItem();
    }

    private boolean shouldShowOffhandItem() {
        if (!JustArmourClient.config.showOffhandItem) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;

        ItemStack offhandItem = client.player.getOffhandItem();
        if (offhandItem.isEmpty()) return false;

        return JustArmourClient.config.showAllHeldItems || offhandItem.isDamageableItem();
    }

    private void drawArmorBox(GuiGraphicsExtractor graphics) {
        int hudX = JustArmourClient.config.hudX;
        int hudY = JustArmourClient.config.hudY;
        int spacing = JustArmourClient.config.spacing;
        float scale = JustArmourClient.config.scale;

        // Calculate bounds with scale
        int armorSlots = 4;
        int scaledSpacing = (int)(spacing * scale);
        int topY = hudY - (scaledSpacing * (armorSlots - 1)) - (int)(4 * scale);
        int bottomY = hudY + (int)(20 * scale);

        int leftX, rightX;
        if (JustArmourClient.config.durabilityOnRight) {
            leftX = hudX - (int)(18 * scale);
            rightX = hudX + (int)(64 * scale);
        } else {
            leftX = hudX - (int)(44 * scale);
            rightX = hudX + (int)(30 * scale);
        }

        drawBoxBackground(graphics, leftX, topY, bottomY, rightX);
    }

    private void drawHeldItemBox(GuiGraphicsExtractor graphics) {
        int x = JustArmourClient.config.heldItemX;
        int y = JustArmourClient.config.heldItemY;
        float scale = JustArmourClient.config.scale;

        int topY = y - (int)(4 * scale);
        int bottomY = y + (int)(20 * scale);

        int leftX, rightX;
        if (JustArmourClient.config.durabilityOnRight) {
            leftX = x - (int)(18 * scale);
            rightX = x + (int)(64 * scale);
        } else {
            leftX = x - (int)(44 * scale);
            rightX = x + (int)(30 * scale);
        }

        drawBoxBackground(graphics, leftX, topY, bottomY, rightX);
    }

    private void drawOffhandItemBox(GuiGraphicsExtractor graphics) {
        int x = JustArmourClient.config.offhandItemX;
        int y = JustArmourClient.config.offhandItemY;
        float scale = JustArmourClient.config.scale;

        int topY = y - (int)(4 * scale);
        int bottomY = y + (int)(20 * scale);

        int leftX, rightX;
        if (JustArmourClient.config.durabilityOnRight) {
            leftX = x - (int)(18 * scale);
            rightX = x + (int)(64 * scale);
        } else {
            leftX = x - (int)(44 * scale);
            rightX = x + (int)(30 * scale);
        }

        drawBoxBackground(graphics, leftX, topY, bottomY, rightX);
    }

    private void drawBoxBackground(GuiGraphicsExtractor graphics, int leftX, int topY, int bottomY, int rightX) {
        // Draw semi-transparent background
        int bgColor = 0x80000000;
        graphics.fill(leftX, topY, rightX, bottomY, bgColor);

        // Draw border
        int borderColor = 0xFF404040;
        graphics.fill(leftX, topY, rightX, topY + 1, borderColor);
        graphics.fill(leftX, bottomY - 1, rightX, bottomY, borderColor);
        graphics.fill(leftX, topY, leftX + 1, bottomY, borderColor);
        graphics.fill(rightX - 1, topY, rightX, bottomY, borderColor);
    }

    private boolean isInsideArmorBox(double mouseX, double mouseY) {
        int hudX = JustArmourClient.config.hudX;
        int hudY = JustArmourClient.config.hudY;
        int spacing = JustArmourClient.config.spacing;
        float scale = JustArmourClient.config.scale;

        int armorSlots = 4;
        int scaledSpacing = (int)(spacing * scale);
        int topY = hudY - (scaledSpacing * (armorSlots - 1)) - (int)(4 * scale);
        int bottomY = hudY + (int)(20 * scale);

        int hitboxXStart, hitboxXEnd;
        if (JustArmourClient.config.durabilityOnRight) {
            hitboxXStart = hudX - (int)(18 * scale);
            hitboxXEnd = hudX + (int)(64 * scale);
        } else {
            hitboxXStart = hudX - (int)(44 * scale);
            hitboxXEnd = hudX + (int)(30 * scale);
        }

        return mouseX >= hitboxXStart && mouseX <= hitboxXEnd &&
                mouseY >= topY && mouseY <= bottomY;
    }

    private boolean isInsideHeldItemBox(double mouseX, double mouseY) {
        int x = JustArmourClient.config.heldItemX;
        int y = JustArmourClient.config.heldItemY;
        float scale = JustArmourClient.config.scale;

        int topY = y - (int)(4 * scale);
        int bottomY = y + (int)(20 * scale);

        int hitboxXStart, hitboxXEnd;
        if (JustArmourClient.config.durabilityOnRight) {
            hitboxXStart = x - (int)(18 * scale);
            hitboxXEnd = x + (int)(64 * scale);
        } else {
            hitboxXStart = x - (int)(44 * scale);
            hitboxXEnd = x + (int)(30 * scale);
        }

        return mouseX >= hitboxXStart && mouseX <= hitboxXEnd &&
                mouseY >= topY && mouseY <= bottomY;
    }

    private boolean isInsideOffhandItemBox(double mouseX, double mouseY) {
        int x = JustArmourClient.config.offhandItemX;
        int y = JustArmourClient.config.offhandItemY;
        float scale = JustArmourClient.config.scale;

        int topY = y - (int)(4 * scale);
        int bottomY = y + (int)(20 * scale);

        int hitboxXStart, hitboxXEnd;
        if (JustArmourClient.config.durabilityOnRight) {
            hitboxXStart = x - (int)(18 * scale);
            hitboxXEnd = x + (int)(64 * scale);
        } else {
            hitboxXStart = x - (int)(44 * scale);
            hitboxXEnd = x + (int)(30 * scale);
        }

        return mouseX >= hitboxXStart && mouseX <= hitboxXEnd &&
                mouseY >= topY && mouseY <= bottomY;
    }

    @Override
    public void onClose() {
        super.onClose();
        JustArmourClient.saveConfig();
    }
}