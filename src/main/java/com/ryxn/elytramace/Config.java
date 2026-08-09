package com.ryxn.elytramace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    /*
     * Main settings
     */

    public boolean enabled = true;

    public boolean autoAim = true;

    public boolean autoTarget = true;

    public boolean requireTarget = true;

    public boolean autoStartGliding = true;

    public boolean autoFirework = false;

    public boolean attackOnlyWhileDescending = true;

    public boolean restoreSlot = true;


    /*
     * Target / aiming settings
     */

    public double targetRange = 32.0;

    public double aimFov = 90.0;

    public double aimSpeed = 1.0;

    public double leadTicks = 2.0;


    /*
     * Timing settings
     */

    public int attackDelayTicks = 2;

    public int launchDelayTicks = 4;

    public int fireworkDelayTicks = 6;


    /*
     * Config file
     */

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("elytra-mace.json");


    /*
     * Load configuration.
     */

    public static Config load() {

        try {

            if (Files.exists(FILE)) {

                Config loaded =
                        GSON.fromJson(
                                Files.readString(FILE),
                                Config.class
                        );

                if (loaded != null) {
                    return loaded;
                }
            }

        } catch (Exception ignored) {
        }


        /*
         * If no config exists,
         * create one using defaults.
         */

        Config config = new Config();

        config.save();

        return config;
    }


    /*
     * Save configuration.
     */

    public void save() {

        try {

            Files.createDirectories(
                    FILE.getParent()
            );

            Files.writeString(
                    FILE,
                    GSON.toJson(this)
            );

        } catch (IOException ignored) {
        }
    }
}
