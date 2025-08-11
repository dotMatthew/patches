package dev.taiqane.patches.internal.git;

import dev.taiqane.patches.configuration.PatchesConfiguration;
import dev.taiqane.patches.internal.TempStorage;
import dev.taiqane.patches.internal.error.ExitCodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;


/*
    I wrote this class with the help of chatgpt 5
    Some of the things are not so clear to myself. So if u find a bug in here
    it would be nice if you create a mr. And if possible also fix the bug lol
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class GitService {
    private final DiffService diffService = new DiffService();
    private final PatchesConfiguration configuration;
    private final TempStorage tempStorage;

    public ExitCodes downloadRepository() {
        try {
            log.info("Start to download the repository");
            Git.cloneRepository()
                    .setURI(configuration.getBaseRepoUrl())
                    .setBranch(configuration.getBaseRepoRef())
                    .setDirectory(new File("_workdir"))
                    .call();
            log.info("Successfully downloaded the repository");
            return ExitCodes.SUCCESSFUL;
        } catch (GitAPIException e) {
            log.error("An error occurred at downloading the repository", e);
            return ExitCodes.INTERNAL_ERROR;
        }
    }

    public ExitCodes createGitPatch(String patchFileName) {
        File repoDir = new File(this.getConfiguration().getGitRepoDirectory());

        log.info("Starting to create a patch");
        try (Repository repository = Git.open(repoDir).getRepository()) {
            try (Git git = new Git(repository)) {

                Status status = git.status().call();
                if (!status.isClean()) {
                    log.error("Your repository is not in a clean state. Cannot create a patch from an unclean state");
                    return ExitCodes.USAGE_ERROR;
                }

                ObjectId oldCommitId = repository.resolve("HEAD~1");
                ObjectId newCommitId = repository.resolve("HEAD");

                AbstractTreeIterator oldTreeIter = prepareTreeParser(repository, oldCommitId);
                AbstractTreeIterator newTreeIter = prepareTreeParser(repository, newCommitId);

                if (oldTreeIter == null || newTreeIter == null) {
                    log.error("AbstractTreeIterator is null but should not be!");
                    return ExitCodes.INTERNAL_ERROR;
                }

                List<DiffEntry> diffs = git.diff()
                        .setOldTree(oldTreeIter)
                        .setNewTree(newTreeIter)
                        .call();

                File patchFile = new File(this.getConfiguration().getPatchesDirectoryPath() + "/" + patchFileName + ".patch");
                String rawDiff = null;

                try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                     DiffFormatter formatter = new DiffFormatter(out)) {

                    formatter.setRepository(repository);
                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
                    formatter.setDetectRenames(true);

                    for (DiffEntry entry : diffs) {
                        formatter.format(entry);
                    }

                    rawDiff = out.toString();
                }

                try (RevWalk walk = new RevWalk(repository)) {
                    RevCommit newCommit = walk.parseCommit(newCommitId);
                    PersonIdent author = newCommit.getAuthorIdent();

                    String[] subjectAndBody = this.splitSubjectAndBody(newCommit.getFullMessage());
                    String subject = subjectAndBody[0];
                    String body = subjectAndBody[1];

                    String name = (author.getName() != null) ? author.getName() : "Unknown Author";
                    String email = (author.getEmailAddress() != null) ? author.getEmailAddress() : "unknown@example.com";

                    ZonedDateTime authorDate = ZonedDateTime.ofInstant(author.getWhenAsInstant(), author.getZoneId());

                    this.getDiffService().writePatchWithHeader(patchFile, rawDiff, subject, body, name, email, authorDate);
                    log.info("Patch successfully created and saved to disk! ({}.patch)", patchFileName);
                }

                return ExitCodes.SUCCESSFUL;
            }
        } catch (GitAPIException | IOException e) {
            log.error("An error occurred at creating this patch", e);
            return ExitCodes.INTERNAL_ERROR;
        }
    }

    public ExitCodes applyPatches() {
        File[] patchFiles = new File(this.getConfiguration().getPatchesDirectoryPath()).listFiles((dir, name) -> name.toLowerCase().endsWith(".patch"));

        if (patchFiles == null || patchFiles.length == 0) {
            log.error("No patches to apply found");
            return ExitCodes.SUCCESSFUL;
        }

        try (Git git = Git.open(new File(this.getConfiguration().getGitRepoDirectory()))) {

            Status status = git.status().call();
            if (!status.isClean()) {
                log.error("The repository is not in a clean state. To apply patches you need a clean repository!");
                return ExitCodes.USAGE_ERROR;
            }

            for (File file : patchFiles) {
                log.info("Applying patch {}", file.getName());

                PatchesDiff diff = this.getDiffService().readPatches(file);

                try (ByteArrayInputStream in = new ByteArrayInputStream(diff.getDiffText().getBytes(StandardCharsets.UTF_8))) {
                    git.apply()
                            .setPatch(in)
                            .call();

                    git.add().addFilepattern(".").call();
                    git.add().setUpdate(true).addFilepattern(".").call();

                    PersonIdent author = new PersonIdent(diff.getAuthorName(), diff.getAuthorEmail(), diff.getAuthorDate().toInstant(), diff.getAuthorDate().getZone());

                    String message = diff.getSubject() + "\n\n" + diff.getBody();
                    git.commit()
                            .setMessage(message)
                            .setAuthor(author)
                            .setCommitter(author)
                            .call();

                    log.info("Applied patch {} successfully as commit", file.getName());
                } catch (GitAPIException | IOException e) {
                    log.error("An error occurred at applying patch {} ", file.getName(), e);
                    return ExitCodes.INTERNAL_ERROR;
                }
            }
            return ExitCodes.SUCCESSFUL;
        } catch (Exception e) {
            log.error("An error occurred at applying patch", e);
            return ExitCodes.INTERNAL_ERROR;
        }
    }

    public ExitCodes resetRepository() {
        try (Git git = Git.open(new File(this.getConfiguration().getGitRepoDirectory()))) {

            git.clean()
                    .setCleanDirectories(true)
                    .setForce(true)
                    .call();

            git.fetch()
                    .setRemote("origin")
                    .setRefSpecs(
                            new RefSpec("+refs/heads/*:refs/remotes/origin/*"),
                            new RefSpec("+refs/tags/*:refs/tags/*")
                    )
                    .setTagOpt(TagOpt.FETCH_TAGS)
                    .call();

            ObjectId targetCommit = this.resolveToCommit(git.getRepository(), this.getConfiguration().getBaseRepoRef());
            if (targetCommit == null) {
                log.error("Could not found commit id for base ref");
                return ExitCodes.INTERNAL_ERROR;
            }

            git.reset()
                    .setMode(ResetType.HARD)
                    .setRef(targetCommit.getName())
                    .call();

            log.info("Successfully reset the repository");
            return ExitCodes.SUCCESSFUL;
        } catch (Exception e) {
            log.error("An error occurred at resetting repository", e);
            return ExitCodes.INTERNAL_ERROR;
        }
    }

    public ExitCodes convertPatchesToGitPatches() {
        File[] patchFiles = new File(this.getConfiguration().getPatchesDirectoryPath()).listFiles((dir, name) -> name.toLowerCase().endsWith(".patch"));

        if (patchFiles == null || patchFiles.length == 0) {
            log.error("No patches to convert found");
            return ExitCodes.SUCCESSFUL;
        }

        try {
            Files.createDirectories(Path.of("converted"));
        } catch (IOException e) {
            log.error("Unable to create converted directory", e);
            return ExitCodes.OPERATING_SYSTEM_ERROR;
        }

        for (File file : patchFiles) {
            PatchesDiff diff = this.getDiffService().readPatches(file);
            try (FileWriter writer = new FileWriter(new File("converted", file.getName()))) {
                writer.write(diff.getDiffText());
                log.info("Successfully converted patch {}", file.getName());
            } catch (IOException e) {
                log.error("Unable to write converted patches to disk", e);
                return ExitCodes.OPERATING_SYSTEM_ERROR;
            }
        }
        return ExitCodes.SUCCESSFUL;
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (var reader = repository.newObjectReader()) {
                treeParser.reset(reader, treeId);
            }
            walk.dispose();
            return treeParser;
        } catch (IncorrectObjectTypeException e) {
            log.error("Incorrect type given", e);
        } catch (MissingObjectException e) {
            log.error("Missing object given", e);
        } catch (IOException e) {
            log.error("An error occurred at creating this patch", e);
        }
        return null;
    }

    private String[] splitSubjectAndBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }

        int idx = rawBody.indexOf('\n');
        if (idx < 0) {
            return null;
        }

        String subject = rawBody.substring(0, idx).trim();
        String body = rawBody.substring(idx + 1).strip();

        return new String[]{subject, body};
    }

    private String getRefString(String baseRef) {
        if (baseRef.matches("v\\d+\\.\\d+\\.\\d+")) {
            return "refs/tags/" + baseRef;
        } else {
            return "refs/remotes/origin/" + baseRef;
        }
    }

    private ObjectId resolveToCommit(Repository repo, String baseRef) throws Exception {
        if (this.looksLikeFullSha1(baseRef)) {
            ObjectId id = repo.resolve(baseRef + "^{commit}");
            if (id != null) return id;
            return null;
        }

        if (baseRef.startsWith("refs/")) {
            Ref ref = repo.exactRef(baseRef);
            if (ref != null) return peelToCommit(repo, ref);
        }

        String[] candidates = new String[]{
                "refs/remotes/origin/" + baseRef,
                "refs/tags/" + baseRef,
                "refs/heads/" + baseRef
        };
        for (String cand : candidates) {
            Ref r = repo.findRef(cand);
            if (r != null) {
                ObjectId peeled = peelToCommit(repo, r);
                if (peeled != null) return peeled;
            }
        }

        Ref r = repo.findRef(baseRef);
        if (r != null) return peelToCommit(repo, r);

        return null;
    }

    private ObjectId peelToCommit(Repository repo, Ref ref) throws Exception {
        ObjectId obj = ref.getObjectId();
        if (obj == null) return null;

        try (RevWalk walk = new RevWalk(repo)) {
            var any = walk.parseAny(obj);
            if (any instanceof RevTag tag) {
                RevObject target = tag.getObject();
                RevObject parsed = walk.parseAny(target);
                ObjectId asCommit = repo.resolve(parsed.getId().name() + "^{commit}");
                return asCommit != null ? asCommit : parsed.getId();
            }
            ObjectId asCommit = repo.resolve(obj.getName() + "^{commit}");
            return asCommit != null ? asCommit : obj;
        }
    }

    private boolean looksLikeFullSha1(String value) {
        if (value == null || value.length() != 40) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') ||
                          (c >= 'a' && c <= 'f') ||
                          (c >= 'A' && c <= 'F');
            if (!hex) return false;
        }
        return true;
    }
}
