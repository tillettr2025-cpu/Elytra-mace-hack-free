package com.ryxn.elytramace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public boolean enabled = true;
    public boolean requireCrosshairEntity = true;
    public boolean autoStartGliding = true;
    public boolean autoFirework = false;
    public int attackDelayTicks = 2;
    public int launchDelayTicks = 8;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("elytra-mace.json");

    public static Config load() {
        try {
            if (Files.exists(FILE)) return GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception ignored) {}
        Config c = new Config(); c.save(); return c;
    }

    public void save() {
        try { Files.writeString(FILE, GSON.toJson(this)); } catch (IOException ignored) {}
    }
}
