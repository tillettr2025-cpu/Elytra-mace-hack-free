package com.ryxn.elytramace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ElytraMaceClient implements ClientModInitializer {
    public static Config CONFIG;
    public static KeyBinding ACTIVATION_KEY;
    private static boolean active;
    private static int timer;
    private static int previousSlot = -1;

    @Override public void onInitializeClient() {
        CONFIG = Config.load();
        ACTIVATION_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytra_mace.activate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R,
                "category.elytra_mace"));

        ClientTickEvents.END_CLIENT_TICK.register(ElytraMaceClient::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        while (ACTIVATION_KEY.wasPressed()) {
            if (!active) start(client.player); else stop(client.player);
        }
        if (!active || !CONFIG.enabled) return;

        PlayerEntity p = client.player;
        if (timer > 0) { timer--; return; }

        if (CONFIG.autoStartGliding && !p.isGliding() && p.isFallFlying()) {
            // no-op: kept as a state check; vanilla flight starts from the jump key.
        }

        if (!p.isGliding()) {
            if (CONFIG.autoStartGliding && p.isFallFlying() && p.getVelocity().y < 0) {
                client.options.jumpKey.setPressed(true);
                client.options.jumpKey.setPressed(false);
            }
            timer = CONFIG.launchDelayTicks;
            return;
        }

        if (CONFIG.autoFirework && hasFirework(p)) {
            useFirework(client);
            timer = 4;
            return;
        }

        Entity target = client.targetedEntity;
        if (target == null || !target.isAlive()) { timer = 1; return; }

        int maceSlot = findMace(p);
        if (maceSlot < 0) { stop(p); return; }
        if (p.getInventory().selectedSlot != maceSlot) p.getInventory().selectedSlot = maceSlot;

        // Mace attacks are strongest during a downward fall. The vanilla attack
        // packet is sent through the normal client interaction manager.
        if (p.getVelocity().y < -0.08) {
            if (client.interactionManager != null) {
                client.interactionManager.attackEntity(p, client.targetedEntity);
                p.swingHand(Hand.MAIN_HAND);
            }
            timer = Math.max(1, CONFIG.attackDelayTicks);
        }
    }

    private static void start(PlayerEntity p) {
        active = true; timer = 0; previousSlot = p.getInventory().selectedSlot;
    }
    private static void stop(PlayerEntity p) {
        active = false; timer = 0;
        if (previousSlot >= 0) p.getInventory().selectedSlot = previousSlot;
        previousSlot = -1;
    }
    private static int findMace(PlayerEntity p) {
        for (int i=0;i<9;i++) if (p.getInventory().getStack(i).isOf(Items.MACE)) return i;
        return -1;
    }
    private static boolean hasFirework(PlayerEntity p) {
        for (int i=0;i<9;i++) if (p.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) return true;
        return false;
    }
    private static void useFirework(MinecraftClient client) {
        int old = client.player.getInventory().selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                client.player.getInventory().selectedSlot = i;
                if (client.interactionManager != null) client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                client.player.getInventory().selectedSlot = old;
                return;
            }
        }
    }
  }
          
