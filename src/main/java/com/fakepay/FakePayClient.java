package com.fakepay;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

public class FakePayClient implements ClientModInitializer {
    public static boolean fakeMode = false;
    public static double fakeBalance = 0.0;

    private static KeyMapping openMenuKey;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    @Override
    public void onInitializeClient() {
        FakePayConfig.load();

        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fakepay.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                client.gui.setScreen(new BalanceScreen(client.gui.screen()));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommand(dispatcher));

        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (!fakeMode) return true;
            String normalized = command.startsWith("/") ? command.substring(1) : command;
            String[] parts = normalized.trim().split("\\s+");
            if (parts.length != 3 || !parts[0].equalsIgnoreCase("pay")) return true;
            try {
                double amount = Double.parseDouble(parts[2]);
                if (!Double.isFinite(amount) || amount <= 0) return true;
                fakePayment(parts[1], amount);
                return false;
            } catch (NumberFormatException ignored) {
                return true;
            }
        });
    }

    private static void registerCommand(CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("fakepay")
                .then(ClientCommandManager.literal("toggle").executes(context -> {
                    fakeMode = !fakeMode;
                    FakePayConfig.save();
                    showLocalMessage("FakePay: " + (fakeMode ? "ON" : "OFF"));
                    return 1;
                })));
    }

    public static void fakePayment(String player, double amount) {
        showLocalMessage("Paid $" + MONEY.format(amount) + " to " + player);
    }

    public static void showLocalMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.hud.getChat().addMessage(Component.literal(message));
        }
    }

    public static class BalanceScreen extends Screen {
        private final Screen parent;
        private EditBox amountField;

        protected BalanceScreen(Screen parent) {
            super(Component.literal("FakePay"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            amountField = new EditBox(font, width / 2 - 100, 80, 200, 20, Component.literal("Fake balance"));
            amountField.setValue(MONEY.format(fakeBalance).replace(",", ""));
            amountField.setMaxLength(20);
            addRenderableWidget(amountField);

            addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
                try {
                    double value = Double.parseDouble(amountField.getValue().replace(",", "").trim());
                    if (Double.isFinite(value) && value >= 0) {
                        fakeBalance = value;
                        FakePayConfig.save();
                    }
                } catch (NumberFormatException ignored) {
                }
                returnToParent();
            }).bounds(width / 2 - 105, 115, 100, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                    .bounds(width / 2 + 5, 115, 100, 20).build());

            amountField.setFocused(true);
        }

        @Override
        public void onClose() {
            returnToParent();
        }

        private void returnToParent() {
            Minecraft.getInstance().gui.setScreen(parent);
        }
    }
}
