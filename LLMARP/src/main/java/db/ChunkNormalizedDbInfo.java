package db;

public class ChunkNormalizedDbInfo {
    public long id;
    public long chunkPatternsId;
    public String hash;

    public long getId() {
        return id;
    }
    public long getChunkPatternsId() {
        return chunkPatternsId;
    }
    public String getHash() {
        return hash;
    }
}
