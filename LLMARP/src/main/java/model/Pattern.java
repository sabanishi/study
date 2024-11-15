package model;

import lombok.*;
import model.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Pattern {
    @Getter
    private final HalNode oldTreeRoot;
    @Getter
    private final HalNode newTreeRoot;
    @Getter
    private final List<NormalizationInfo> appliedNormalizations;
    @Getter
    private final Hash hash;

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

    private Set<Pattern> normalizeInternal(){
        Set<Pattern> result = new HashSet<>();
        normalizeName(result);
        normalizeArg(result);

        return result;
    }

    private void normalizeArg(Set<Pattern> result){
        for(HalNode oldNode: getOldTreeRoot().preOrder()){
            if(oldNode instanceof HalTreeNode oldTargetNode){
                if(oldTargetNode.getType().equals("METHOD_INVOCATION_ARGUMENTS")){
                    //子要素が全て正規化されている時、正規化を行う
                    boolean canNormalize = true;
                    for(HalNode child : oldTargetNode.getChildren()){
                        if(!(child instanceof HalNormalizeNode)){
                            canNormalize = false;
                            break;
                        }
                    }

                    if(canNormalize){
                        HalNormalizeInvocationNode normalizedNode = HalNormalizeInvocationNode.of(oldTargetNode);
                        HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                        HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                        List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());
                        //oldTreeの該当箇所を置換する
                        copyOldRoot.replace(oldTargetNode, normalizedNode);

                        //newTreeに同じIDのノードがあれば置換する
                        HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                        if(newTargetNode instanceof HalTreeNode){
                            //子要素が全て正規化されている時、正規化を行う
                            boolean canNormalize2 = true;
                            for(HalNode child : newTargetNode.getChildren()){
                                if(!(child instanceof HalNormalizeNode)){
                                    canNormalize2 = false;
                                    break;
                                }
                            }
                            if(canNormalize2){
                                HalNormalizeInvocationNode normalizeNode2 = HalNormalizeInvocationNode.of((HalTreeNode)newTargetNode);
                                copyNewRoot.replace(newTargetNode, normalizeNode2);
                            }
                        }

                        //正規化情報を追加
                        copyInfoList.add(NormalizationInfo.of(NormalizationType.Argument,normalizedNode.getId()));
                        Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                        result.add(copy);
                    }
                }
            }
        }
    }

    /**
     * 自身に対して、変数名正規化を一箇所だけ適用したSetを返す
     */
    private void normalizeName(Set<Pattern> result){
        for(HalNode oldNode :getOldTreeRoot().preOrder()){
            if(oldNode instanceof HalTreeNode oldTargetNode && !(oldNode instanceof HalNormalizeNode)){
                switch(oldTargetNode.getType()){
                    case "SimpleName":
                        if(oldNode.getParent()!=null){
                            String parentType = oldNode.getParent().getType();
                            //親がメソッドかクラスの時、正規化を行わない
                            if(parentType.equals("MethodInvocation") || parentType.equals("TypeDeclaration")){
                                break;
                            }
                        }
                    case "StringLiteral":
                    case "CharacterLiteral":
                    case "NumberLiteral":
                    case "BooleanLiteral":
                        HalNormalizeNode normalizedNode = HalNormalizeNode.of(oldTargetNode);
                        HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                        HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                        List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());
                        //oldTreeの該当箇所を置換する
                        copyOldRoot.replace(oldTargetNode, normalizedNode);

                        //newTreeに同じIDのノードがあれば置換する
                        HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                        if(newTargetNode instanceof HalTreeNode){
                            HalNormalizeNode normalizeNode2 = HalNormalizeNode.of((HalTreeNode)newTargetNode);
                            copyNewRoot.replace(newTargetNode, normalizeNode2);
                        }

                        //正規化情報を追加
                        copyInfoList.add(NormalizationInfo.of(NormalizationType.Name,normalizedNode.getId()));
                        Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                        result.add(copy);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static Pattern of(HalNode oldTreeRoot, HalNode newTreeRoot, List<NormalizationInfo> appliedNormalizations){
        Hash hash = Hash.of(Stream.concat(
                Stream.of(oldTreeRoot.getHash(), newTreeRoot.getHash()),
                appliedNormalizations.stream().map(NormalizationInfo::getHash)));
        return new Pattern(oldTreeRoot, newTreeRoot, appliedNormalizations,hash);
    }

    public static Pattern of(HalNode oldTreeRoot, HalNode newTreeRoot, List<NormalizationInfo> appliedNormalizations, Hash hash){
        return new Pattern(oldTreeRoot, newTreeRoot, appliedNormalizations, hash);
    }
}
