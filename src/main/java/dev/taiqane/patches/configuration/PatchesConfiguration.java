package dev.taiqane.patches.configuration;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import dev.taiqane.patches.internal.TempStorage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.management.StandardEmitterMBean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchesConfiguration {
    public transient static final String EXAMPLE_URL = "no://op";

    private String baseRepoUrl;
    private String baseRepoRef;
    private String patchesDirectoryPath;

    private static Optional<PatchesConfiguration> load(File file) {
        try {
            PatchesConfiguration configuration = new Toml().read(file).to(PatchesConfiguration.class);
            return Optional.of(configuration);
        } catch (Exception ex) {
            System.err.println("Failed to load patches configuration: " + ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<PatchesConfiguration> createExampleConfiguration(TempStorage storage) {
        PatchesConfiguration configuration = new PatchesConfiguration("no://op", "main", "patches");
        TomlWriter writer = new TomlWriter();
        try {
            writer.write(configuration, new File(".patches.toml"));
            storage.setFileInThisRunCreated(true);
            return Optional.of(configuration);
        } catch (IOException e) {
            System.err.println("Failed to write patches.toml: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<PatchesConfiguration> existAndLoadOrCreateAndLoad(File file, TempStorage storage) {
        if (Files.exists(file.toPath())) {
            return load(file);
        } else {
            return createExampleConfiguration(storage);
        }
    }
}
