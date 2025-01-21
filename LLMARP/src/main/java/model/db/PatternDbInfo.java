package model.db;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value

public class PatternDbInfo {
    String hash;
    String oldTreeHash;
    String newTreeHash;
    boolean isCandidate;
    boolean isNormalized;
    boolean isUseful;
    boolean isChildUseful;

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
    public boolean getIsChildUseful(){
        return isChildUseful;
    }

    public PatternDbInfo(String hash, String oldTreeHash, String newTreeHash, boolean isCandidate, boolean isNormalized, boolean isUseful, boolean isChildUseful) {
        this.hash = hash;
        this.oldTreeHash = oldTreeHash;
        this.newTreeHash = newTreeHash;
        this.isCandidate = isCandidate;
        this.isNormalized = isNormalized;
        this.isUseful = isUseful;
        this.isChildUseful = isChildUseful;
    }

    public String toString() {
        return "PatternDbInfo(hash=" + this.getHash() + ", oldTreeHash=" + this.getOldTreeHash() + ", newTreeHash=" + this.getNewTreeHash() + ", isCandidate=" + this.getIsCandidate() + ", isNormalized=" + this.getIsNormalized() + ", isUseful=" + this.getIsUseful() + ", isChildUseful=" + this.getIsChildUseful() + ")";
    }
}
