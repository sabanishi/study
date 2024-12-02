package util;

import lombok.Data;

@Data(staticConstructor = "of")
public class Pair<A,B> {
    private final A first;
    private final B second;
}