package dev.taiqane.patches.cli.command;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.git.GitService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Slf4j
@Getter
@NoArgsConstructor
@Command(name = "apply", description = "Applies the patches to the base repository")
public class ApplyCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"}, description = "Path to patches config file. Defaults to patches.toml")
    private File configFile = new File("patches.toml");

    @Override
    public Integer call() throws Exception {
        try {
            if (!configFile.exists()) {
                log.error("No patches config file found! Run patches init first!");
                return 42;
            }
            PatchesConfiguration.existAndLoadOrCreateAndLoad(configFile, storage, "no://op").ifPresent(patchesConfiguration -> configuration = patchesConfiguration);
            File patchesDirectoryPath = new File(this.getConfiguration().getPatchesDirectoryPath());

            if (!patchesDirectoryPath.exists() || !patchesDirectoryPath.isDirectory()) {
                log.error("Patches directory path is not a directory!");
                return 42;
            }

            if (!(new File(this.getConfiguration().getGitRepoDirectory()).exists())) {
                log.error("No workdir found!");
                return 42;
            }

            GitService gitService = new GitService(this.getConfiguration(), this.getStorage());
            return gitService.applyPatches();
        } catch (Exception e) {
            log.error("An error occurred at applying patches", e);
            return 42;
        }
    }
}
