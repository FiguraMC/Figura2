package org.figuramc.figura.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.util.functional.ThrowingFunction;

import java.util.function.Function;

/**
 * Represents one key-value pair inside the configuration json object.
 */
public class ConfigOption<T> {

    public final String key;
    public final T defaultValue;
    private final ThrowingFunction<JsonElement, T, Throwable> reader;
    private final Function<T, JsonElement> writer;

    private T value;

    public ConfigOption(String key, T defaultValue, ThrowingFunction<JsonElement, T, Throwable> reader, Function<T, JsonElement> writer) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.reader = reader;
        this.writer = writer;
        this.value = defaultValue;
        // Add this to the list of all configs
        ConfigManager.ALL_CONFIGS.add(this);
    }

    /**
     * Get the key from the json file, and apply the reader.
     * If the reader errors in any way, warn about it in the console and keep the value as it was.
     */
    public void readFromJson(JsonObject json) {
        JsonElement element = json.get(key);
        if (element == null) {
            FiguraMod.LOGGER.warn("No config for key \"{}\". Using default.", key);
        } else try {
            this.value = reader.apply(element);
        } catch (Throwable t) {
            //noinspection StringConcatenationArgumentToLogCall
            FiguraMod.LOGGER.error("Failed to parse input \"" + element + "\" for config key \"" + key + "\"! Using default.", t);
        }
    }

    /**
     * Store the current value of this option into the json
     */
    public void saveToJson(JsonObject json) {
        json.add(key, writer.apply(value));
    }

    public T getValue() { return this.value; }
    public void setValue(T value) {
        this.value = value;
        ConfigManager.save();
    }

}
