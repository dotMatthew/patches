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
import java.util.concurrent.Callable;

@Slf4j
@Getter
@NoArgsConstructor
@Command(name = "convert-patches", description = "Convert our patch format to git patches")
public class ConvertPatchesCommand implements Callable<Integer> {
    private final TempStorage storage = new TempStorage();
    private PatchesConfiguration configuration;

    @Option(names = {"-F", "--config"}, description = "Path to patches config file. Defaults to patches.properties")
    private File configFile = new File("patches.properties");

    @Override
    public Integer call() throws Exception {

        if (!this.getConfigFile().exists()) {
            log.error("No valid patches configuration file found!");
            return ExitCodes.USAGE_ERROR.getCodeValue();
        }

        PatchesConfiguration.load(this.getConfigFile()).ifPresent(patchesConfiguration -> configuration = patchesConfiguration);

        log.info("Start to convert our patches to git patches");
        GitService gitService = new GitService(this.getConfiguration(), this.getStorage());
        return gitService.convertPatchesToGitPatches().getCodeValue();
    }
}
