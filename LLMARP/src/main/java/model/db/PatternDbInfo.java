package model.db;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class PatternDbInfo {
    String hash;
    String oldTreeHash;
    String newTreeHash;
    boolean isNormalized;
    boolean isCandidate;
    boolean isUseful;
}
