package db;

public class PatternConnectionDbInfo {
    public long id;
    public String parentHash;
    public String childHash;

    public long getId() {
        return id;
    }
    public String getParentHash() {
        return parentHash;
    }
    public String getChildHash() {
        return childHash;
    }
}
