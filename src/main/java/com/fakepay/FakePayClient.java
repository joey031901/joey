package com.fakepay;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

public class FakePayClient implements ClientModInitializer {
    public static boolean fakeMode = false;
    public static double fakeBalance = 0.0;

    private static KeyBinding openMenuKey;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    @Override
    public void onInitializeClient() {
        FakePayConfig.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fakepay.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "category.fakepay"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                client.setScreen(new BalanceScreen(client.currentScreen));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommand(dispatcher));

        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            if (!fakeMode) return true;

            String normalized = command.startsWith("/") ? command.substring(1) : command;
            String[] parts = normalized.trim().split("\\s+");

            if (parts.length != 3 || !parts[0].equalsIgnoreCase("pay")) {
                return true;
            }

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
                .then(ClientCommandManager.literal("toggle")
                        .executes(context -> {
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(message));
        }
    }

    public static class BalanceScreen extends Screen {
        private final Screen parent;
        private TextFieldWidget amountField;

        protected BalanceScreen(Screen parent) {
            super(Text.literal("FakePay"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            amountField = new TextFieldWidget(
                    textRenderer,
                    width / 2 - 100,
                    80,
                    200,
                    20,
                    Text.literal("Fake balance")
            );
            amountField.setText(MONEY.format(fakeBalance).replace(",", ""));
            amountField.setMaxLength(20);
            addDrawableChild(amountField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
                try {
                    double value = Double.parseDouble(amountField.getText().replace(",", "").trim());
                    if (Double.isFinite(value) && value >= 0) {
                        fakeBalance = value;
                        FakePayConfig.save();
                    }
                } catch (NumberFormatException ignored) {
                    // Leave the previous balance unchanged.
                }
                close();
            }).dimensions(width / 2 - 105, 115, 100, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                    .dimensions(width / 2 + 5, 115, 100, 20).build());

            amountField.setFocused(true);
        }

        private void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 35, 0xFFFFFF);
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("Fake money amount"),
                    width / 2,
                    65,
                    0xFFFFFF
            );
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("Client-side only — does not change server money."),
                    width / 2,
                    150,
                    0xAAAAAA
            );
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
