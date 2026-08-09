package com.ryxn.elytramace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class ElytraMaceClient implements ClientModInitializer {

    public static Config CONFIG;
    public static KeyBinding ACTIVATION_KEY;

    private static boolean active = false;
    private static boolean releaseReady = false;

    private static int fireworkTimer = 0;
    private static int cueTimer = 0;

    private static PlayerEntity currentTarget = null;

    /*
     * Used to avoid constantly changing targets.
     */
    private static int targetLostTicks = 0;

    /*
     * Last predicted position.
     */
    private static Vec3d predictedPosition = null;

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
                stop(client);
            } else {
                start(client.player);
            }
        }

        if (!active || !CONFIG.enabled) {
            return;
        }

        ClientPlayerEntity player = client.player;

        if (fireworkTimer > 0) {
            fireworkTimer--;
        }

        if (cueTimer > 0) {
            cueTimer--;
        }

        /*
         * ----------------------------------------
         * TARGET SELECTION
         * ----------------------------------------
         *
         * Only actual players are allowed.
         *
         * This prevents the camera from locking
         * onto the floor, blocks, items, etc.
         */

        if (currentTarget == null
                || !validTarget(player, currentTarget)) {

            targetLostTicks++;

            if (targetLostTicks >= 5) {

                currentTarget =
                        findPlayerTarget(client, player);

                targetLostTicks = 0;
            }
        } else {
            targetLostTicks = 0;
        }

        /*
         * No target:
         *
         * DO NOT MOVE THE CAMERA.
         */

        if (currentTarget == null) {

            predictedPosition = null;
            releaseReady = false;

            return;
        }

        /*
         * ----------------------------------------
         * TARGET PREDICTION
         * ----------------------------------------
         */

        predictedPosition =
                predictTargetPosition(
                        player,
                        currentTarget
                );

        /*
         * ----------------------------------------
         * AIM ASSIST
         * ----------------------------------------
         */

        if (CONFIG.autoAim && predictedPosition != null) {

            aimAt(
                    player,
                    predictedPosition
            );
        }

        /*
         * ----------------------------------------
         * ELYTRA / FIREWORK ASSIST
         * ----------------------------------------
         */

        if (player.isGliding()) {

            if (CONFIG.autoFirework
                    && fireworkTimer <= 0
                    && hasFirework(player)) {

                useFirework(client);

                fireworkTimer =
                        Math.max(
                                1,
                                CONFIG.fireworkDelayTicks
                        );
            }

            /*
             * Determine whether we're approaching
             * the mace impact window.
             */

            releaseReady =
                    shouldReleaseElytra(
                            player,
                            currentTarget,
                            predictedPosition
                    );

            if (releaseReady) {

                /*
                 * We do not perform the attack.
                 *
                 * Instead, give the player a clear
                 * indication that the mace timing
                 * window is ready.
                 */

                showMaceCue(client);

                /*
                 * The player can now manually:
                 *
                 * - release Elytra
                 * - switch to mace
                 * - attack
                 */

                return;
            }

            return;
        }

        /*
         * ----------------------------------------
         * NOT GLIDING
         * ----------------------------------------
         *
         * If the player has manually left Elytra
         * and is now falling toward the target,
         * keep the camera pointed at the predicted
         * impact point.
         */

        if (predictedPosition != null
                && player.getVelocity().y < 0.0) {

            if (CONFIG.autoAim) {

                aimAt(
                        player,
                        predictedPosition
                );
            }

            if (isInMaceWindow(
                    player,
                    predictedPosition
            )) {

                showMaceCue(client);
            }
        }
    }

    /*
     * --------------------------------------------
     * START
     * --------------------------------------------
     */

    private static void start(
            ClientPlayerEntity player
    ) {

        active = true;

        releaseReady = false;

        fireworkTimer = 0;
        cueTimer = 0;

        targetLostTicks = 0;

        currentTarget = null;
        predictedPosition = null;
    }

    /*
     * --------------------------------------------
     * STOP
     * --------------------------------------------
     */

    private static void stop(
            MinecraftClient client
    ) {

        active = false;

        releaseReady = false;

        fireworkTimer = 0;
        cueTimer = 0;

        currentTarget = null;
        predictedPosition = null;

        if (client.player != null) {

            client.player.sendMessage(
                    Text.literal(
                            "§7Elytra Mace Assist: §cOFF"
                    ),
                    true
            );
        }
    }

    /*
     * --------------------------------------------
     * PLAYER TARGET SEARCH
     * --------------------------------------------
     */

    private static PlayerEntity findPlayerTarget(
            MinecraftClient client,
            ClientPlayerEntity player
    ) {

        double range =
                Math.max(
                        4.0,
                        CONFIG.targetRange
                );

        List<? extends PlayerEntity> players =
        client.world.getPlayers();
        /*
         * First preference:
         * the player currently under the
         * crosshair.
         */

        Entity crosshair =
                client.targetedEntity;

        if (crosshair instanceof PlayerEntity target) {

            if (target != player
                    && validTarget(player, target)
                    && angleTo(
                            player,
                            target
                    ) <= CONFIG.aimFov) {

                return target;
            }
        }

        /*
         * Otherwise choose the closest player
         * to the center of the camera.
         */

        return players.stream()

                .filter(target ->
                        target != player)

                .filter(Entity::isAlive)

                .filter(target ->
                        !target.isSpectator())

                .filter(target ->
                        player.distanceTo(target)
                                <= range)

                .filter(target ->
                        angleTo(
                                player,
                                target
                        ) <= CONFIG.aimFov)

                .min(
                        Comparator.comparingDouble(
                                target ->
                                        angleTo(
                                                player,
                                                target
                                        )
                        )
                )

                .orElse(null);
    }

    /*
     * --------------------------------------------
     * TARGET VALIDATION
     * --------------------------------------------
     */

    private static boolean validTarget(
            ClientPlayerEntity player,
            PlayerEntity target
    ) {

        if (target == null) {
            return false;
        }

        if (target == player) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        if (target.isSpectator()) {
            return false;
        }

        return player.distanceTo(target)
                <= CONFIG.targetRange;
    }

    /*
     * --------------------------------------------
     * TARGET PREDICTION
     * --------------------------------------------
     *
     * This intentionally uses the target's current
     * velocity.
     *
     * That helps when the target is:
     *
     * - moving sideways
     * - jumping
     * - falling
     * - knocked upward
     * - affected by Wind Burst
     */

    private static Vec3d predictTargetPosition(
            ClientPlayerEntity player,
            PlayerEntity target
    ) {

        Vec3d targetPos =
                target.getPos();

        Vec3d velocity =
                target.getVelocity();

        double distance =
                player.distanceTo(target);

        /*
         * Estimate time to impact.
         *
         * Faster movement means a shorter
         * prediction interval.
         */

        double playerSpeed =
                player.getVelocity().length();

        double estimatedSpeed =
                Math.max(
                        0.35,
                        playerSpeed
                                + velocity.length()
                );

        double time =
                distance
                        / estimatedSpeed;

        /*
         * Clamp prediction so it doesn't
         * wildly overshoot after knockback.
         */

        time =
                MathHelper.clamp(
                        time,
                        0.05,
                        Math.max(
                                0.1,
                                CONFIG.leadTicks
                        )
                );

        /*
         * Predict target movement.
         */

        Vec3d predicted =
                targetPos.add(
                        velocity.multiply(time)
                );

        /*
         * Aim around the player's upper body.
         */

        predicted =
                predicted.add(
                        0.0,
                        target.getHeight()
                                * 0.55,
                        0.0
                );

        return predicted;
    }

    /*
     * --------------------------------------------
     * AIM
     * --------------------------------------------
     */

    private static void aimAt(
            ClientPlayerEntity player,
            Vec3d target
    ) {

        Vec3d eye =
                player.getEyePos();

        double dx =
                target.x - eye.x;

        double dy =
                target.y - eye.y;

        double dz =
                target.z - eye.z;

        double horizontal =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        float wantedYaw =
                (float) (
                        Math.toDegrees(
                                Math.atan2(
                                        dz,
                                        dx
                                )
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

        float yawDifference =
                MathHelper.wrapDegrees(
                        wantedYaw
                                - player.getYaw()
                );

        float yaw =
                player.getYaw()
                        + yawDifference * speed;

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

    /*
     * --------------------------------------------
     * RELEASE DETECTION
     * --------------------------------------------
     */

    private static boolean shouldReleaseElytra(
            ClientPlayerEntity player,
            PlayerEntity target,
            Vec3d predicted
    ) {

        if (predicted == null) {
            return false;
        }

        double distance =
                player.getPos()
                        .distanceTo(predicted);

        /*
         * Don't release too early.
         */

        if (distance > 10.0) {
            return false;
        }

        /*
         * Require downward movement.
         */

        if (player.getVelocity().y >= 0.0) {
            return false;
        }

        /*
         * Check whether the target is roughly
         * in front of us.
         */

        return angleTo(
                player,
                target
        ) <= Math.max(
                25.0,
                CONFIG.aimFov
        );
    }

    /*
     * --------------------------------------------
     * MACE WINDOW
     * --------------------------------------------
     */

    private static boolean isInMaceWindow(
            ClientPlayerEntity player,
            Vec3d predicted
    ) {

        if (predicted == null) {
            return false;
        }

        double distance =
                player.getPos()
                        .distanceTo(predicted);

        /*
         * Close enough for the manual
         * mace timing window.
         */

        return distance <= 5.5
                && player.getVelocity().y < -0.08;
    }

    /*
     * --------------------------------------------
     * ANGLE TO TARGET
     * --------------------------------------------
     */

    private static double angleTo(
            ClientPlayerEntity player,
            Entity target
    ) {

        Vec3d targetPos =
                target.getPos()
                        .add(
                                0.0,
                                target.getHeight()
                                        * 0.55,
                                0.0
                        );

        Vec3d eye =
                player.getEyePos();

        double dx =
                targetPos.x - eye.x;

        double dy =
                targetPos.y - eye.y;

        double dz =
                targetPos.z - eye.z;

        double horizontal =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        float targetYaw =
                (float) (
                        Math.toDegrees(
                                Math.atan2(
                                        dz,
                                        dx
                                )
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
                                targetYaw
                                        - player.getYaw()
                        )
                );

        float pitchDifference =
                Math.abs(
                        targetPitch
                                - player.getPitch()
                );

        return Math.sqrt(
                yawDifference * yawDifference
                        + pitchDifference * pitchDifference
        );
    }

    /*
     * --------------------------------------------
     * FIREWORK
     * --------------------------------------------
     */

    private static boolean hasFirework(
            ClientPlayerEntity player
    ) {

        for (int slot = 0; slot < 9; slot++) {

            if (player.getInventory()
                    .getStack(slot)
                    .isOf(
                            Items.FIREWORK_ROCKET
                    )) {

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
                    .isOf(
                            Items.FIREWORK_ROCKET
                    )) {

                player.getInventory()
                        .setSelectedSlot(slot);

                if (client.interactionManager != null) {

                    client.interactionManager
                            .interactItem(
                                    player,
                                    net.minecraft.util.Hand.MAIN_HAND
                            );
                }

                player.getInventory()
                        .setSelectedSlot(oldSlot);

                return;
            }
        }
    }

    /*
     * --------------------------------------------
     * MACE CUE
     * --------------------------------------------
     */

    private static void showMaceCue(
            MinecraftClient client
    ) {

        if (cueTimer > 0) {
            return;
        }

        cueTimer = 8;

        if (client.player != null) {

            client.player.sendMessage(
                    Text.literal(
                            "§c§lMACE NOW!"
                    ),
                    true
            );
        }
    }
}
