package dev.taiqane.patches.internal.git;

import dev.taiqane.patches.internal.error.ExitCodes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import dev.taiqane.patches.internal.git.PatchesDiff.DiffSections;

/*
    This class represents a git diff which includes metadata (author, commit message etc.)
 */
@Slf4j
@Getter
@NoArgsConstructor
public class DiffService {

    private static final String HDR_FROM    = "From: ";
    private static final String HDR_DATE    = "Date: ";
    private static final String HDR_SUBJECT = "Subject: ";
    private static final String DIFF_START  = "diff --git ";

    public ExitCodes writePatchWithHeader(File outFile, String diffText, String subject, String body, String authorName, String authorEmail, ZonedDateTime dateTime) {
        if (diffText == null || diffText.isBlank()) {
            log.error("No diff text found in commit!");
            return ExitCodes.INTERNAL_ERROR;
        }
        if (!diffText.startsWith("diff --git ")) {
            log.error("The specified diff is incorrect!");
            return ExitCodes.INTERNAL_ERROR;
        }

        if (authorName == null || authorName.isBlank()) {
            log.error("The name of the author is null or blank!");
            return ExitCodes.INTERNAL_ERROR;
        }

        if (authorEmail == null || authorEmail.isBlank()) {
            log.error("The email of the author is null or blank!");
            return ExitCodes.INTERNAL_ERROR;
        }

        // RFC-1123 Datum erzeugen (z. B. "Tue, 12 Aug 2025 10:15:00 +0200")
        String rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME.format(dateTime);

        try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile, false), StandardCharsets.UTF_8)) {
            // Header
            w.write("From: " + authorName + " <" + authorEmail + ">\n");
            w.write("Date: " + rfc1123 + "\n");
            w.write("Subject: " + subject.strip() + "\n");
            w.write("\n");

            // Body (optional)
            String bodyStripped = body.strip();
            if (!bodyStripped.isEmpty()) {
                w.write(bodyStripped);
                w.write("\n\n");
            }

            if (!diffText.endsWith("\n")) {
                diffText = diffText + "\n";
            }
            w.write(diffText);
            return ExitCodes.SUCCESSFUL;
        } catch (IOException e) {
            log.error("Unable to write patch file to disk", e);
            return ExitCodes.OPERATING_SYSTEM_ERROR;
        }
    }

    public PatchesDiff readPatches(File file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            PatchesDiff patchesDiff = new PatchesDiff();
            StringBuilder rawDiff = new StringBuilder();
            StringBuilder rawBody = new StringBuilder();

            DiffSections sections = DiffSections.HEADERS;

            while ((line = reader.readLine()) != null) {
                switch (sections) {
                    case HEADERS -> {
                        if (line.isBlank()) {
                            sections = DiffSections.BODY;
                            continue;
                        }

                        if (line.startsWith(DIFF_START)) {
                            sections = DiffSections.DIFF;
                            rawDiff.append(line).append("\n");
                            continue;
                        }

                        if (line.startsWith(HDR_FROM)) {
                            String fromLine = line.replace(HDR_FROM, "");
                            String[] splitFromLine = fromLine.split("<");

                            if (splitFromLine.length != 2) {
                                log.error("Unable to parse from line from patch");
                                return null;
                            }

                            String name = splitFromLine[0].trim();
                            String mail = splitFromLine[1].trim().replace("<", "").replace(">", "");

                            patchesDiff.setAuthorName(name);
                            patchesDiff.setAuthorEmail(mail);
                        }

                        if (line.startsWith(HDR_DATE)) {
                            String dateLine = line.replace(HDR_DATE, "").trim();

                            if (!this.parseAndSetAuthorDate(dateLine, patchesDiff)) {
                                log.error("Unable to parse date from line from patch");
                                return null;
                            }
                        }

                        if (line.startsWith(HDR_SUBJECT)) {
                            String subjectLine = line.replace(HDR_SUBJECT, "").trim();
                            patchesDiff.setSubject(subjectLine);
                        }
                    }
                    case BODY -> {
                        if (line.startsWith(DIFF_START)) {
                            sections = DiffSections.DIFF;
                            rawDiff.append(line).append("\n");
                            continue;
                        }

                        rawBody.append(line).append("\n");

                    }
                    case DIFF -> {
                        rawDiff.append(line).append("\n");
                    }
                }
            }

            patchesDiff.setBody(rawBody.toString());
            patchesDiff.setDiffText(rawDiff.toString());

            return patchesDiff;
        } catch (IOException e) {
            log.error("Unable to read patch files from disk", e);
        }

        return null;
    }

    private boolean parseAndSetAuthorDate(String s, PatchesDiff patchesDiff) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
            patchesDiff.setAuthorDate(zdt);
            return true;
        } catch (DateTimeParseException ignore) { return false; }
    }

}