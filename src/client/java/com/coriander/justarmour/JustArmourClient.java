package com.coriander.justarmour;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLScancode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JustArmourClient implements ClientModInitializer {

	public static JustArmourConfigData config;
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final File configFile = new File(Minecraft.getInstance().gameDirectory, "config/justarmour_config.json");

	private static KeyMapping toggleHudKeybind;
	private static KeyMapping openConfigScreenKeybind;
	private static final KeyMapping.Category JUSTARMOUR_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("justarmour", "main"));

	@Override
	public void onInitializeClient() {
		loadConfig();

		HudElementRegistry.addLast(Identifier.parse("just_armour:hud"), ((graphics, deltaTracker) -> {
			if (config.hudX == -1 || config.hudY == -1) {
				setDefaultPosition();
			}

			if (config.hudEnabled) {
				renderArmorHUD(graphics, config.hudX, config.hudY);
				renderHeldItemHUD(graphics);
				renderOffhandItemHUD(graphics);
			}
		}));

		registerKeybinds();
	}

	private void setDefaultPosition() {
		Minecraft client = Minecraft.getInstance();
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();

		// Calculate perfect bottom right position for armor
		int rightMargin = config.durabilityOnRight ? 80 : 60;
		int bottomMargin = 40;

		config.hudX = screenWidth - rightMargin;
		config.hudY = screenHeight - bottomMargin;

		// Set held item position below armor
		config.heldItemX = config.hudX;
		config.heldItemY = config.hudY + config.spacing;

		// Set offhand item position to left of armor
		config.offhandItemX = config.hudX - 80;
		config.offhandItemY = config.hudY;

		saveConfig();
	}

	private void registerKeybinds() {
		// Toggle HUD keybind (G)
		toggleHudKeybind = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.justarmour.toggle",
				InputConstants.Type.KEYBOARD,
				SDLScancode.SDL_SCANCODE_G,
				JUSTARMOUR_CATEGORY
		));

		// Config Screen keybind (J)
		openConfigScreenKeybind = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.justarmour.config",
				InputConstants.Type.KEYBOARD,
				SDLScancode.SDL_SCANCODE_J,
				JUSTARMOUR_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Toggle HUD
			while (toggleHudKeybind.consumeClick()) {
				config.hudEnabled = !config.hudEnabled;
				saveConfig();

				if (client.player != null) {
					String message = "Armor HUD " + (config.hudEnabled ? "on" : "off");
					client.player.sendSystemMessage(Component.literal(message));
				}
			}

			// Open Config Screen
			while (openConfigScreenKeybind.consumeClick()) {
				if (client.player != null) {
					client.gui.setScreen(new TransparentConfigScreen());
				}
			}
		});
	}

	public static void renderArmorHUD(GuiGraphicsExtractor graphics, int baseX, int baseY) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) return;

		int y = baseY;

		// Get armor slots in order: boots, leggings, chestplate, helmet
		EquipmentSlot[] armorSlots = {
				EquipmentSlot.FEET,
				EquipmentSlot.LEGS,
				EquipmentSlot.CHEST,
				EquipmentSlot.HEAD
		};

		// Render armor pieces
		for (EquipmentSlot slot : armorSlots) {
			ItemStack stack = client.player.getItemBySlot(slot);
			if (!stack.isEmpty()) {
				renderArmorPiece(graphics, stack, baseX, y);
			}
			y -= (int)(config.spacing * config.scale);
		}
	}

	public static void renderHeldItemHUD(GuiGraphicsExtractor graphics) {
		if (!config.showHeldItem) return;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) return;

		ItemStack heldItem = client.player.getMainHandItem();
		if (heldItem.isEmpty()) return;

		// Show all items if enabled, otherwise only damageable
		if (!config.showAllHeldItems && !heldItem.isDamageableItem()) return;

		renderArmorPiece(graphics, heldItem, config.heldItemX, config.heldItemY);
	}

	public static void renderOffhandItemHUD(GuiGraphicsExtractor graphics) {
		if (!config.showOffhandItem) return;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) return;

		ItemStack offhandItem = client.player.getOffhandItem();
		if (offhandItem.isEmpty()) return;

		// Show all items if enabled, otherwise only damageable
		if (!config.showAllHeldItems && !offhandItem.isDamageableItem()) return;

		renderArmorPiece(graphics, offhandItem, config.offhandItemX, config.offhandItemY);
	}

	private static void renderArmorPiece(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
		Minecraft client = Minecraft.getInstance();

		// Calculate positions with scale
		int iconX = config.durabilityOnRight ? x - (int)(10 * config.scale) : x + (int)(10 * config.scale);

		// SCALE ITEMS AND TEXT using 1.21.11 matrix method
		graphics.pose().pushMatrix();
		graphics.pose().scale(config.scale, config.scale);

		int scaledIconX = (int)(iconX / config.scale);
		int scaledY = (int)(y / config.scale);

		// Draw item with vanilla durability bar if enabled
		if (config.showDurabilityBar) {
			graphics.item(stack, scaledIconX, scaledY);
			graphics.itemDecorations(client.font, stack, scaledIconX, scaledY);
		} else {
			// Draw item without bar
			graphics.item(stack, scaledIconX, scaledY);
		}

		// Don't render durability text if hidden or item isn't damageable
		if (!config.hideDurabilityNumbers && stack.isDamageableItem()) {
			int durability = stack.getMaxDamage() - stack.getDamageValue();
			int max = stack.getMaxDamage();
			int color;

			// Color logic
			if (config.disableColors) {
				color = 0xFFFFFFFF;
			} else {
				if (durability == max) {
					color = 0xFF55FF55;
				} else if (durability == max - 1) {
					color = 0xFFFFFFFF;
				} else if (durability <= 71) {
					color = 0xFFFF5555;
				} else if (durability <= 149) {
					color = 0xFFFFA500;
				} else if (durability <= 281) {
					color = 0xFFFFFF55;
				} else {
					color = 0xFFFFFFFF;
				}
			}

			String text = config.showMaxDamage ? durability + "/" + max : String.valueOf(durability);
			int textWidth = client.font.width(text);

			int textX;
			if (config.durabilityOnRight) {
				textX = (int)((x + 10) / config.scale);
			} else {
				textX = (int)((x - textWidth * config.scale - 10) / config.scale) + 16;
			}

			int textY = scaledY + 5;

			graphics.text(client.font, text, textX, textY, color, config.showShadow);
		}

		graphics.pose().popMatrix();
	}

	public static void loadConfig() {
		try {
			if (!configFile.exists()) {
				config = new JustArmourConfigData();
				saveConfig();
				return;
			}
			FileReader reader = new FileReader(configFile);
			config = gson.fromJson(reader, JustArmourConfigData.class);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			config = new JustArmourConfigData();
		}
	}

	public static void saveConfig() {
		try {
			configFile.getParentFile().mkdirs();
			FileWriter writer = new FileWriter(configFile);
			gson.toJson(config, writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}