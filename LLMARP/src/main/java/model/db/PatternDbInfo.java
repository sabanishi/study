package model.db;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
public class PatternDbInfo {
    String hash;
    String oldTreeHash;
    String newTreeHash;
    boolean isCandidate;
    boolean isNormalized;
    boolean isUseful;

    //NOTE:なぜか@GetterだとDaoのinsertPatternでエラーが出るので明示的にgetterを作成
    public boolean getIsCandidate(){
        return isCandidate;
    }
    public boolean getIsNormalized(){
        return isNormalized;
    }
    public boolean getIsUseful(){
        return isUseful;
    }
}
