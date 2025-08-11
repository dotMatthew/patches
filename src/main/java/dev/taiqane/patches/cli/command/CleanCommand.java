package dev.taiqane.patches.cli.command;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.file.FileService;
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
@Command(name = "clean", description = "Delete all work files")
public class CleanCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"}, description = "Path to patches config file. Defaults to patches.properties")
    private File configFile = new File("patches.properties");

    @Override
    public Integer call() throws Exception {
        // Only delete _workdir if patches config file is found
        if (!this.getConfigFile().exists()) {
            log.error("No patches config file found! Will not clean work directory");
            return 42;
        }

        PatchesConfiguration.load(this.getConfigFile()).ifPresent(patchesConfiguration -> configuration = patchesConfiguration);

        log.info("Deleting {} directory", this.getConfiguration().getGitRepoDirectory());

        FileService fileService = new FileService(this.getConfiguration());
        return fileService.cleanWorkDir().getCodeValue();
    }
}
