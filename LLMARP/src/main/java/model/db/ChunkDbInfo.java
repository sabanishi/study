package model.db;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class ChunkDbInfo {
    String fileName;
    int oldBegin;
    int oldEnd;
    int newBegin;
    int newEnd;
    String oldStatement;
    String newStatement;

    @Override
    public String toString(){
        return """
                ChunkDbInfo(fileName=%s,
                oldBegin=%d,oldEnd=%d,
                newBegin=%d,newEnd=%d,
                oldStatement=
                %s,
                newStatement=
                %s)
                """.formatted(fileName,oldBegin,oldEnd,newBegin,newEnd,oldStatement,newStatement);
    }
}
