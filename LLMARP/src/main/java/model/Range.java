package model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "of")
public class Range {
    int begin;
    int end;

    public boolean isEmpty() {
        return begin == end;
    }

    public boolean isSingle() {
        return begin + 1 == end;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return String.format("empty(%d)", begin);
        }

        if (isSingle()) {
            return String.valueOf(begin);
        }

        return String.format("%d-%d", begin, end - 1);
    }

    public Range joint(Range other) {
        return new Range(Math.min(this.begin, other.begin), Math.max(this.end, other.end));
    }
}
