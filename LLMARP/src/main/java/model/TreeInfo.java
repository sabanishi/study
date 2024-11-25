package model;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class TreeInfo {
    String hash;
    String structure;
    String text;
}
