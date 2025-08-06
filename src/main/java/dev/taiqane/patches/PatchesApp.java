package dev.taiqane.patches;

import dev.taiqane.patches.cli.PatchesCLI;
import picocli.CommandLine;

public class PatchesApp {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PatchesCLI()).execute(args);
        System.exit(exitCode);
    }
}
