package dev.taiqane.patches.cli;

import dev.taiqane.patches.cli.command.ApplyCommand;
import dev.taiqane.patches.cli.command.CleanCommand;
import dev.taiqane.patches.cli.command.InitCommand;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "patches",
        mixinStandardHelpOptions = true,
        version = "patches 1.0.0",
        description = "A command line tool for managing Git patches",
        subcommands = {
                InitCommand.class,
                ApplyCommand.class,
                CleanCommand.class
        }
)
public class PatchesCLI implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("No command specified. Use 'patches --help' for usage information.");
        return 0;
    }
}
