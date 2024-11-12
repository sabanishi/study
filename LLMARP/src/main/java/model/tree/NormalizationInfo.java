package model.tree;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "of")
public class NormalizationInfo {
    NormalizationType type;
    int targetId;
}
