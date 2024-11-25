package model;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class ChunkInfo {
    String fileName;
    int oldBegin;
    int oldEnd;
    int newBegin;
    int newEnd;
    String oldStatement;
    String newStatement;
}
