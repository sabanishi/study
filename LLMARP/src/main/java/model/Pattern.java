package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import model.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor(staticName = "of")
public class Pattern {
    @Getter
    private final HalNode oldTreeRoot;
    @Getter
    private final HalNode newTreeRoot;
    @Getter
    private final List<NormalizationInfo> appliedNormalizations;

    private boolean isNormalized = false;

    @Override
    public boolean equals(Object obj){
        if(obj instanceof Pattern pattern){
            return oldTreeRoot.equals(pattern.oldTreeRoot) && newTreeRoot.equals(pattern.newTreeRoot);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return oldTreeRoot.hashCode() + newTreeRoot.hashCode();
    }

    /**
     * 自身に対して正規化を行い、その結果を引数のSetに追加する
     */
    public void normalize(Set<Pattern> result){
        Set<Pattern> normalized = normalizeInternal();

        //resultに既に含まれているものは除外する
        normalized.removeIf(result::contains);
        result.addAll(normalized);
        this.isNormalized = true;

        for(Pattern pattern : normalized){
            if(pattern.isNormalized)continue;
            pattern.normalize(result);
        }
    }

    public Pattern deepCopy(){
        return Pattern.of(oldTreeRoot.deepCopy(), newTreeRoot.deepCopy(), new ArrayList<NormalizationInfo>(appliedNormalizations));
    }

    private Set<Pattern> normalizeInternal(){
        return normalizeName();
    }

    /**
     * 自身に対して、変数名正規化を一箇所だけ適用したSetを返す
     */
    private Set<Pattern> normalizeName(){
        Set<Pattern> result = new HashSet<Pattern>();

        for(HalNode oldNode :getOldTreeRoot().preOrder()){
            if(oldNode instanceof HalTreeNode oldTargetNode && !(oldNode instanceof HalNormalizeNode)){
                if(oldTargetNode.getType().equals("SimpleName") || oldTargetNode.getType().equals("StringLiteral")){
                    HalNormalizeNode normalizedNode = HalNormalizeNode.of(oldTargetNode);
                    Pattern copy = deepCopy();
                    //oldTreeの該当箇所を置換する
                    copy.getOldTreeRoot().replace(oldTargetNode, normalizedNode);

                    //newTreeに同じIDのノードがあれば置換する
                    HalNode newTargetNode = copy.getNewTreeRoot().searchById(normalizedNode.getId());
                    if(newTargetNode instanceof HalTreeNode){
                        HalNormalizeNode normalizeNode2 = HalNormalizeNode.of((HalTreeNode)newTargetNode);
                        copy.getNewTreeRoot().replace(newTargetNode, normalizeNode2);
                    }

                    //正規化情報を追加
                    copy.appliedNormalizations.add(NormalizationInfo.of(NormalizationType.Name,normalizedNode.getId()));
                    result.add(copy);
                }
            }
        }

        return result;
    }
}
