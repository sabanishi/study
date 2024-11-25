package model;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class PatternInfo {
    String hash;
    String oldTreeHash;
    String newTreeHash;
    boolean isNormalized;
}
