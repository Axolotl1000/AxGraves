package com.artillexstudios.axgraves;

import com.artillexstudios.axapi.AxPlugin;
import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.dvs.versioning.BasicVersioning;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.utils.MessageUtils;
import com.artillexstudios.axapi.utils.featureflags.FeatureFlags;
import com.artillexstudios.axgraves.commands.Commands;
import com.artillexstudios.axgraves.grave.Grave;
import com.artillexstudios.axgraves.grave.SpawnedGraves;
import com.artillexstudios.axgraves.listeners.DeathListener;
import com.artillexstudios.axgraves.listeners.PlayerInteractListener;
import com.artillexstudios.axgraves.schedulers.SaveGraves;
import com.artillexstudios.axgraves.schedulers.TickGraves;
import com.artillexstudios.axgraves.utils.UpdateNotifier;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class AxGraves extends AxPlugin {
    private static AxPlugin instance;
    public static Config CONFIG;
    public static Config MESSAGES;
    public static MessageUtils MESSAGEUTILS;
    public static ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    public static final Set<UUID> bypassedPlayers = new HashSet<>();

    public static AxPlugin getInstance() {
        return instance;
    }

    public void enable() {
        instance = this;

        CONFIG = new Config(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        MESSAGES = new Config(new File(getDataFolder(), "messages.yml"), getResource("messages.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        Path bypassPath = getDataFolder().toPath().resolve("bypass");
        try {
            if (Files.exists(bypassPath)) {
                Files.readAllLines(bypassPath, StandardCharsets.UTF_8).forEach(uuid -> {
                    if (uuid.isEmpty()) return;
                    try {
                        bypassedPlayers.add(UUID.fromString(uuid));
                    } catch (IllegalArgumentException ignored) {}
                });
            } else {
                Files.createFile(bypassPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        MESSAGEUTILS = new MessageUtils(MESSAGES.getBackingDocument(), "prefix", CONFIG.getBackingDocument());

        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);

        final BukkitCommandHandler handler = BukkitCommandHandler.create(instance);
        handler.register(new Commands());

        if (CONFIG.getBoolean("save-graves.enabled", true))
            SpawnedGraves.loadFromFile();

        TickGraves.start();
        SaveGraves.start();

        if (CONFIG.getBoolean("update-notifier.enabled", true)) new UpdateNotifier(this, 5076);
    }

    public void disable() {
        TickGraves.stop();
        SaveGraves.stop();

        try {
            Path bypassPath = getDataFolder().toPath().resolve("bypass");
            Files.delete(bypassPath);
            StringBuilder sb = new StringBuilder();
            bypassedPlayers.forEach(uuid -> sb.append(uuid).append("\n"));
            Files.writeString(bypassPath, sb.toString());
        } catch (IOException e) {
            getLogger().severe("Could not save bypassed players.");
            e.printStackTrace(System.err);
        }

        for (Grave grave : SpawnedGraves.getGraves()) {
            if (!CONFIG.getBoolean("save-graves.enabled", true))
                grave.remove();

            if (grave.getEntity() != null)
                grave.getEntity().remove();
            if (grave.getHologram() != null)
                grave.getHologram().remove();
        }

        if (CONFIG.getBoolean("save-graves.enabled", true))
            SpawnedGraves.saveToFile();

        EXECUTOR.shutdown();
    }

    public void updateFlags(FeatureFlags flags) {
        flags.USE_LEGACY_HEX_FORMATTER.set(true);
        flags.PACKET_ENTITY_TRACKER_ENABLED.set(true);
        flags.HOLOGRAM_UPDATE_TICKS.set(5L);
    }
}
