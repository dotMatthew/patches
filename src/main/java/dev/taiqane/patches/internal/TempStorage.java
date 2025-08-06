package dev.taiqane.patches.internal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TempStorage {
    private boolean fileInThisRunCreated = false;
}
