package dev.taiqane.patches.internal.git;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchesDiff {
    private String diffText;
    private String subject;
    private String body;
    private String authorName;
    private String authorEmail;
    private ZonedDateTime authorDate;

    public static enum DiffSections {
        HEADERS, BODY, DIFF;
    }
}
