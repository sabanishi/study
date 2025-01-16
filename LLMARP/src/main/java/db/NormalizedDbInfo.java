package db;

public class NormalizedDbInfo {
    public String hash;
    public String type;
    public long targetId;
    public long orderIndex;

    public String getHash() {
        return hash;
    }
    public String getType() {
        return type;
    }
    public long getTargetId() {
        return targetId;
    }
    public long getOrderIndex() {
        return orderIndex;
    }
}
