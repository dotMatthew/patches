package dev.taiqane.patches.cli.command;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.error.ExitCodes;
import dev.taiqane.patches.internal.git.GitService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Slf4j
@Getter
@NoArgsConstructor
@Command(name = "init", description = "Create a new patches file if not existing and downloads the base repository as specified in patches.properties, but does not apply any patches.")
public class InitCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"}, description = "Path to patches config file. Defaults to patches.properties")
    private File configFile = new File("patches.properties");

    @Option(names = {"-U", "--repo-url"}, description = "URL of the Base Repo")
    private String url = "no://op";

    @Override
    public Integer call() throws Exception {
        PatchesConfiguration.existAndLoadOrCreateAndLoad(this.getConfigFile(), this.getStorage(), url).ifPresent(patchesConfiguration -> configuration = patchesConfiguration);

        if (this.getConfiguration() == null) {
            log.error("No valid patches configuration found?");
            return ExitCodes.USAGE_ERROR.getCodeValue();
        }

        String baseRepoUrl = this.getConfiguration().getBaseRepoUrl();

        if (baseRepoUrl.equals(PatchesConfiguration.EXAMPLE_URL)) {
            if (this.getStorage().isFileInThisRunCreated()) {
                log.info("Create new base patches config file! Happy working :)");
                return ExitCodes.SUCCESSFUL.getCodeValue();
            } else {
                log.error("Example url {} cannot be used to download the base repository! Aborting!", PatchesConfiguration.EXAMPLE_URL);
                return ExitCodes.USAGE_ERROR.getCodeValue();
            }
        }

        Files.createDirectories(Path.of(this.getConfiguration().getPatchesDirectoryPath()));

        GitService gitService = new GitService(this.getConfiguration(), this.getStorage());
        return gitService.downloadRepository().getCodeValue();
    }
}
