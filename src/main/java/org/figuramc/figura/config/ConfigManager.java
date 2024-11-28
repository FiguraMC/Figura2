package org.figuramc.figura.config;

import com.google.gson.*;
import org.figuramc.figura.FiguraMod;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve(FiguraMod.MOD_ID + ".json");
    public static final List<ConfigOption<?>> ALL_CONFIGS = new ArrayList<>();

    // The mod directory path, as a string
    public static final ConfigOption<@Nullable String> MOD_DIRECTORY = new ConfigOption<>("mod_directory_path", null,
            ele -> ele.isJsonNull() ? null : ele.getAsString(), // Reader
            val -> val == null ? JsonNull.INSTANCE : new JsonPrimitive(val) // Writer
    );
    // Whether to force rendering in compatible mode
    public static final ConfigOption<Boolean> FORCE_COMPATIBLE_MODE = new ConfigOption<>("force_compatible_mode", false,
            JsonElement::getAsBoolean,
            JsonPrimitive::new
    );

    // Simply load then save
    public static void init() {
        load();
        save();
    }

    // Load all configs from the config file
    public static void load() {
        // Early return if no config file exists
        if (!Files.exists(CONFIG_FILE_PATH)) return;
        // Read the json out of the config file
        JsonObject configJson;
        try {
            String fileString = Files.readString(CONFIG_FILE_PATH);
            configJson = JsonParser.parseString(fileString).getAsJsonObject();
        } catch (IOException ex) {
            FiguraMod.LOGGER.error("Failed to read config file", ex);
            return;
        } catch (JsonSyntaxException ex) {
            FiguraMod.LOGGER.error("Failed to parse config file, invalid json", ex);
            return;
        }
        // Initialize all the configs from the json object
        for (ConfigOption<?> option : ALL_CONFIGS)
            option.readFromJson(configJson);
    }

    // Save all configs to the config file
    public static void save() {
        JsonObject jsonObject = new JsonObject();
        for (ConfigOption<?> option : ALL_CONFIGS)
            option.saveToJson(jsonObject);
        try {
            Files.writeString(CONFIG_FILE_PATH, jsonObject.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            FiguraMod.LOGGER.error("Failed to save config", ex);
        }
    }

}
