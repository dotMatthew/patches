package dev.taiqane.patches.cli;

import dev.taiqane.patches.cli.command.*;
import dev.taiqane.patches.internal.error.ExitCodes;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Slf4j
@NoArgsConstructor
@Command(name = "patches",
        mixinStandardHelpOptions = true,
        version = "patches 1.0.0",
        description = "A command line tool for managing Git patches",
        subcommands = {
                InitCommand.class,
                ApplyCommand.class,
                CleanCommand.class,
                CreatePatchCommand.class,
                ConfigCommand.class,
                ResetCommand.class,
                ConvertPatchesCommand.class
        }
)
public class PatchesCLI implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        log.error("No command specified. Use 'patches --help' for usage information.");
        return ExitCodes.SUCCESSFUL.getCodeValue();
    }
}
