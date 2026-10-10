package net.lucab.shops.config;

import com.electronwill.nightconfig.core.file.FileConfig;

import net.lucab.shops.screen.ToggleButtonID;

import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

public class ConfigHandler {
    private static final File CONFIG_PATH = prepareConfigPath();
    public static FileConfig config;


    private static final EnumMap<ToggleButtonID, Boolean> toggleSettingsStates = getDefaultToggleSettingStates();

    private static File prepareConfigPath() {
        Path directory = FMLPaths.CONFIGDIR.get();
        Path destination = directory.resolve("spuds_shop_reforged.toml");
        Path legacy = directory.resolve("spudaciousshops.toml");
        try {
            Files.createDirectories(directory);
            // Preserve existing settings on upgrade without overwriting a new config.
            if (!Files.exists(destination) && Files.isRegularFile(legacy)) {
                Files.copy(legacy, destination);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not prepare Spuds Shop Reforged config", exception);
        }
        return destination.toFile();
    }


    private static EnumMap<ToggleButtonID, Boolean> getDefaultToggleSettingStates() {
        EnumMap<ToggleButtonID, Boolean> tss = new EnumMap<>(ToggleButtonID.class);
        config = FileConfig.of(CONFIG_PATH);
        config.load();

        String TogEffectID = "ToggleEffectsDefault";
        boolean effectsEnabled = true;
        if (!config.contains(TogEffectID)) {
            config.set(TogEffectID, true);
            config.save();
        } else {
            effectsEnabled = config.getOrElse(TogEffectID, true);
        }

        //ugly but ensures all values are covered
        for (ToggleButtonID value : ToggleButtonID.values()) {
            tss.put(value,
                    switch (value){
                        case CreativeToggle -> false;
                        case ShopStyleToggle -> false;
                        case IgnoreNBTToggle -> false;
                        case EffectsToggle -> effectsEnabled;
                    }
                    );
        }
        return tss;
    }
    public static Boolean getDefaultToggleSetting(ToggleButtonID ID) {
        return toggleSettingsStates.getOrDefault(ID,false);
    }
    public static void initialise(){}
}
