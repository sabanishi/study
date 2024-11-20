package model.tree;

import lombok.Value;
import model.Hash;

@Value
public class NormalizationInfo {
    NormalizationType type;
    int targetId;
    Hash hash;

    public static NormalizationInfo of(NormalizationType type, int targetId, Hash hash){
        return new NormalizationInfo(type, targetId, hash);
    }

    public static NormalizationInfo of(NormalizationType type,int targetId){
        Hash hash = Hash.of(type.toString() + targetId);
        return new NormalizationInfo(type, targetId, hash);
    }

    @Override
    public String toString(){
        return type + ":" + targetId;
    }
}
