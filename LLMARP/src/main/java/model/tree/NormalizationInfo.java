package model.tree;

import lombok.Value;
import model.Hash;

@Value
public class NormalizationInfo {
    NormalizationType type;
    int targetId;
    int order;
    Hash hash;

    public static NormalizationInfo of(NormalizationType type, int targetId, int order,Hash hash){
        return new NormalizationInfo(type, targetId,order,hash);
    }

    public static NormalizationInfo of(NormalizationType type,int targetId,int order){
        Hash hash = Hash.of(type.toString() + targetId);
        return new NormalizationInfo(type, targetId,order,hash);
    }

    @Override
    public String toString(){
        return type + ":" + targetId;
    }
}
