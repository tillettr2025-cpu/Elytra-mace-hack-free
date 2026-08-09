package com.ryxn.elytramace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class ElytraMaceClient implements ClientModInitializer {

    public static Config CONFIG;
    public static KeyBinding ACTIVATION_KEY;

    private static boolean active = false;
    private static int attackTimer = 0;
    private static int launchTimer = 0;
    private static int fireworkTimer = 0;

    private static int previousSlot = -1;
    private static Entity currentTarget = null;

    @Override
    public void onInitializeClient() {

        CONFIG = Config.load();

        ACTIVATION_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.elytra_mace.activate",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "category.elytra_mace"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                ElytraMaceClient::tick
        );
    }

    private static void tick(MinecraftClient client) {

        if (client.player == null || client.world == null) {
            return;
        }

        while (ACTIVATION_KEY.wasPressed()) {

            if (active) {
                stop(client.player);
            } else {
                start(client.player);
            }
        }

        if (!active || !CONFIG.enabled) {
            return;
        }

        ClientPlayerEntity player = client.player;

        if (attackTimer > 0) {
            attackTimer--;
        }

        if (launchTimer > 0) {
            launchTimer--;
        }

        if (fireworkTimer > 0) {
            fireworkTimer--;
        }

        int maceSlot = findMace(player);

        if (maceSlot == -1) {
            stop(player);
            return;
        }

        /*
         * Automatically find a target.
         */
        if (CONFIG.autoTarget
                || currentTarget == null
                || !validTarget(player, currentTarget)) {

            currentTarget = chooseTarget(client, player);
        }

        /*
         * No target.
         */
        if (currentTarget == null) {

            if (CONFIG.requireTarget) {
                return;
            }

        } else {

            /*
             * Automatically aim at target.
             */
            if (CONFIG.autoAim) {
                aimAt(player, currentTarget);
            }
        }

        /*
         * Elytra is already active.
         */
        if (player.isGliding()) {

            /*
             * Optional firework.
             */
            if (CONFIG.autoFirework
                    && fireworkTimer <= 0
                    && hasFirework(player)) {

                useFirework(client);

                fireworkTimer = Math.max(
                        1,
                        CONFIG.fireworkDelayTicks
                );
            }
        }

        /*
         * We need to be gliding before attempting the attack.
         */
        if (!player.isGliding()) {
            return;
        }

        /*
         * No valid target.
         */
        if (currentTarget == null
                || !validTarget(player, currentTarget)) {

            return;
        }

        /*
         * Attack cooldown.
         */
        if (attackTimer > 0) {
            return;
        }

        /*
         * Only attack while descending if enabled.
         */
        if (CONFIG.attackOnlyWhileDescending
                && player.getVelocity().y >= -0.04) {

            return;
        }

        /*
         * Wait for vanilla attack cooldown.
         */
        if (player.getAttackCooldownProgress(0.0f) < 0.90f) {
            return;
        }

        /*
         * Switch to Mace.
         */
        if (player.getInventory().getSelectedSlot() != maceSlot) {

            player.getInventory().setSelectedSlot(maceSlot);
        }

        /*
         * Attack target.
         */
        if (client.interactionManager != null) {

            client.interactionManager.attackEntity(
                    player,
                    currentTarget
            );

            player.swingHand(Hand.MAIN_HAND);

            attackTimer = Math.max(
                    1,
                    CONFIG.attackDelayTicks
            );
        }
    }

    private static void start(ClientPlayerEntity player) {

        active = true;

        attackTimer = 0;
        launchTimer = 0;
        fireworkTimer = 0;

        currentTarget = null;

        previousSlot =
                player.getInventory().getSelectedSlot();
    }

    private static void stop(ClientPlayerEntity player) {

        active = false;

        attackTimer = 0;
        launchTimer = 0;
        fireworkTimer = 0;

        currentTarget = null;

        if (CONFIG.restoreSlot && previousSlot >= 0) {

            player.getInventory().setSelectedSlot(
                    previousSlot
            );
        }

        previousSlot = -1;
    }

    private static Entity chooseTarget(
            MinecraftClient client,
            ClientPlayerEntity player
    ) {

        double range =
                Math.max(4.0, CONFIG.targetRange);

        List<Entity> candidates =
                client.world.getOtherEntities(
                        player,
                        player.getBoundingBox().expand(range),
                        entity ->
                                entity instanceof LivingEntity
                                        && entity.isAlive()
                                        && !entity.isSpectator()
                                        && entity != player
                );

        /*
         * Prefer whatever the player is already looking at.
         */
        Entity crosshair =
                client.targetedEntity;

        if (crosshair != null
                && candidates.contains(crosshair)
                && angleTo(player, crosshair)
                <= CONFIG.aimFov) {

            return crosshair;
        }

        /*
         * Otherwise choose the closest entity
         * to the center of the player's view.
         */
        return candidates.stream()
                .filter(entity ->
                        angleTo(player, entity)
                                <= CONFIG.aimFov)
                .filter(entity ->
                        player.squaredDistanceTo(entity)
                                <= range * range)
                .min(
                        Comparator
                                .comparingDouble(
                                        (Entity entity) ->
                                                angleTo(player, entity)
                                )
                                .thenComparingDouble(
                                        player::squaredDistanceTo
                                )
                )
                .orElse(null);
    }

    private static boolean validTarget(
            ClientPlayerEntity player,
            Entity target
    ) {

        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target != player
                && player.distanceTo(target)
                <= CONFIG.targetRange;
    }

    private static double angleTo(
            ClientPlayerEntity player,
            Entity target
    ) {

        Vec3d targetPosition =
                target.getPos()
                        .add(
                                target.getVelocity()
                                        .multiply(CONFIG.leadTicks)
                        )
                        .add(
                                0.0,
                                target.getHeight() * 0.55,
                                0.0
                        );

        Vec3d eye =
                player.getEyePos();

        double dx =
                targetPosition.x - eye.x;

        double dy =
                targetPosition.y - eye.y;

        double dz =
                targetPosition.z - eye.z;

        double horizontal =
                Math.sqrt(
                        dx * dx + dz * dz
                );

        float targetYaw =
                (float) (
                        Math.toDegrees(
                                Math.atan2(dz, dx)
                        ) - 90.0
                );

        float targetPitch =
                (float) (
                        -Math.toDegrees(
                                Math.atan2(
                                        dy,
                                        horizontal
                                )
                        )
                );

        float yawDifference =
                Math.abs(
                        MathHelper.wrapDegrees(
                                targetYaw - player.getYaw()
                        )
                );

        float pitchDifference =
                Math.abs(
                        targetPitch - player.getPitch()
                );

        return Math.sqrt(
                yawDifference * yawDifference
                        + pitchDifference * pitchDifference
        );
    }

    private static void aimAt(
            ClientPlayerEntity player,
            Entity target
    ) {

        Vec3d targetPosition =
                target.getPos()
                        .add(
                                target.getVelocity()
                                        .multiply(CONFIG.leadTicks)
                        )
                        .add(
                                0.0,
                                target.getHeight() * 0.55,
                                0.0
                        );

        Vec3d eye =
                player.getEyePos();

        double dx =
                targetPosition.x - eye.x;

        double dy =
                targetPosition.y - eye.y;

        double dz =
                targetPosition.z - eye.z;

        double horizontal =
                Math.sqrt(
                        dx * dx + dz * dz
                );

        float wantedYaw =
                (float) (
                        Math.toDegrees(
                                Math.atan2(dz, dx)
                        ) - 90.0
                );

        float wantedPitch =
                (float) (
                        -Math.toDegrees(
                                Math.atan2(
                                        dy,
                                        horizontal
                                )
                        )
                );

        float speed =
                (float) MathHelper.clamp(
                        CONFIG.aimSpeed,
                        0.05,
                        1.0
                );

        float yaw =
                player.getYaw()
                        + MathHelper.wrapDegrees(
                                wantedYaw
                                        - player.getYaw()
                        ) * speed;

        float pitch =
                player.getPitch()
                        + (
                                wantedPitch
                                        - player.getPitch()
                        ) * speed;

        player.setYaw(yaw);

        player.setPitch(
                MathHelper.clamp(
                        pitch,
                        -90.0f,
                        90.0f
                )
        );
    }

    private static int findMace(
            ClientPlayerEntity player
    ) {

        for (int slot = 0; slot < 9; slot++) {

            if (player.getInventory()
                    .getStack(slot)
                    .isOf(Items.MACE)) {

                return slot;
            }
        }

        return -1;
    }

    private static boolean hasFirework(
            ClientPlayerEntity player
    ) {

        for (int slot = 0; slot < 9; slot++) {

            if (player.getInventory()
                    .getStack(slot)
                    .isOf(Items.FIREWORK_ROCKET)) {

                return true;
            }
        }

        return false;
    }

    private static void useFirework(
            MinecraftClient client
    ) {

        ClientPlayerEntity player =
                client.player;

        if (player == null) {
            return;
        }

        int oldSlot =
                player.getInventory()
                        .getSelectedSlot();

        for (int slot = 0; slot < 9; slot++) {

            if (player.getInventory()
                    .getStack(slot)
                    .isOf(Items.FIREWORK_ROCKET)) {

                player.getInventory()
                        .setSelectedSlot(slot);

                if (client.interactionManager != null) {

                    client.interactionManager.interactItem(
                            player,
                            Hand.MAIN_HAND
                    );
                }

                player.getInventory()
                        .setSelectedSlot(oldSlot);

                return;
            }
        }
    }
}
