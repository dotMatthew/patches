package dev.taiqane.patches.internal.file;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.error.ExitCodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@Getter
@RequiredArgsConstructor
public class FileService {
    private final PatchesConfiguration configuration;

    private ExitCodes deleteDirectoryRecursively(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        this.deleteDirectoryRecursively(file);
                    } else {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            log.error("Unable to delete file {}", file.getName());
                        }
                    }
                }
            }

            boolean isDirDeleted = dir.delete();
            if (!isDirDeleted) {
                log.error("Unable to delete workdir directory: {}", dir.getName());
            }
            return ExitCodes.SUCCESSFUL;
        }
        log.error("{} does not exist", dir.getName());
        return ExitCodes.OPERATING_SYSTEM_ERROR;
    }

    public ExitCodes cleanWorkDir() {
        File file = new File(this.getConfiguration().getGitRepoDirectory());
        if (file.exists() && file.isDirectory()) {
            return this.deleteDirectoryRecursively(file);
        } else {
            log.error("Git repo dir specified in config is not a directory or does not exist?");
            return ExitCodes.OPERATING_SYSTEM_ERROR;
        }
    }

    public ExitCodes renamePatchesDirectory(File oldDirectory, File newDirectory) {
        if (!oldDirectory.exists() || newDirectory.exists()) {
            log.error("Either the current patches directory does not exist or the new name of the directory is already in use! Aborting!");
            return ExitCodes.USAGE_ERROR;
        }

        try {
            if (oldDirectory.renameTo(newDirectory)) {
                return ExitCodes.SUCCESSFUL;
            }
            return ExitCodes.INTERNAL_ERROR;
        } catch (Exception e) {
            log.error("An error occurred at renaming the patches directory");
            return ExitCodes.INTERNAL_ERROR;
        }
    }
}
