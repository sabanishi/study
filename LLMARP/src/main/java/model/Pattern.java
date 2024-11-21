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
        normalizeMethod(result);

        return result;
    }

    private void normalizeMethod(Set<Pattern> result){
        for(HalNode oldNode: getOldTreeRoot().preOrder()){
            if(oldNode instanceof HalTreeNode oldTargetNode){
                if(oldTargetNode.getType().equals("MethodInvocation")){
                    //子要素が存在しない、またはHalNormalizeInvocationNodeの時、正規化を行う
                    NormalizeMethodInfo info = canNormalizeMethod(oldTargetNode);

                    if(info.canNormalize){
                        HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                        HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                        List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());
                        HalTreeNode normalizedNode = doNormalizeMethod(copyOldRoot, oldTargetNode, info);

                        //newTreeに同じIDのノードがあれば置換する
                        HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                        if(newTargetNode instanceof HalTreeNode){
                            //子要素が全て正規化されている時、正規化を行う
                            NormalizeMethodInfo newInfo = canNormalizeMethod(newTargetNode);
                            if(newInfo.canNormalize){
                                doNormalizeMethod(copyNewRoot, (HalTreeNode)newTargetNode, newInfo);
                            }
                        }

                        //正規化情報を追加
                        copyInfoList.add(NormalizationInfo.of(NormalizationType.Method,normalizedNode.getId(),copyInfoList.size()));
                        Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                        result.add(copy);
                    }
                }
            }
        }
    }

    private HalTreeNode doNormalizeMethod(HalNode copyRoot, HalTreeNode targetNode,NormalizeMethodInfo info){
        String methodName = info.methodName;
        String receiverName = info.receiverName;
        String newName = receiverName.isEmpty() ? methodName : receiverName + "." + methodName;
        HalTreeNode newNode = HalTreeNode.of(targetNode);
        newNode.setType("SimpleName");
        newNode.setLabel(newName);

        copyRoot.replace(targetNode, newNode);
        return newNode;
    }

    private NormalizeMethodInfo canNormalizeMethod(HalNode targetNode){
        boolean canNormalize = true;
        String methodName = "";
        String receiverName = "";
        for(HalNode child:targetNode.getChildren()){
            if(child instanceof HalTreeNode childNode){
                //子要素が引数ノードだったら正規化しない
                if(childNode.getType().equals("METHOD_INVOCATION_ARGUMENTS")){
                    canNormalize = false;
                    break;
                }
                //子要素がRecevierであり、かつそれが変数でない時、正規化しない
                if(childNode.getType().equals("METHOD_INVOCATION_RECEIVER")){
                    if(!childNode.getChildren().isEmpty()){
                        if(childNode.getChildren().get(0) instanceof HalTreeNode receiverNode){
                            if(!receiverNode.getType().equals("SimpleName")){
                                canNormalize = false;
                                break;
                            }else{
                                receiverName = receiverNode.getLabel();
                            }
                        }
                    }
                }

                if(childNode.getType().equals("SimpleName")){
                    methodName = childNode.getLabel();
                }
            }
        }
        return new NormalizeMethodInfo(canNormalize, methodName, receiverName);
    }

    public record NormalizeMethodInfo(boolean canNormalize, String methodName, String receiverName){}

    private void normalizeArg(Set<Pattern> result){
        for(HalNode oldNode: getOldTreeRoot().preOrder()){
            if(oldNode instanceof HalTreeNode oldTargetNode){
                if(oldTargetNode.getType().equals("METHOD_INVOCATION_ARGUMENTS")){
                    //子要素が全て正規化されている時、正規化を行う
                    if(canNormalizeArg(oldTargetNode)){
                        HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                        HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                        List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());

                        HalNormalizeInvocationNode normalizedNode = doNormalizeArg(copyOldRoot, oldTargetNode);

                        //newTreeに同じIDのノードがあれば置換する
                        HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                        if(newTargetNode instanceof HalTreeNode){
                            //子要素が全て正規化されている時、正規化を行う
                            if(canNormalizeArg(newTargetNode)){
                                doNormalizeArg(copyNewRoot, (HalTreeNode)newTargetNode);
                            }
                        }

                        //正規化情報を追加
                        copyInfoList.add(NormalizationInfo.of(NormalizationType.Argument,normalizedNode.getId(),copyInfoList.size()));
                        Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                        result.add(copy);
                    }
                }
            }
        }
    }

    private HalNormalizeInvocationNode doNormalizeArg(HalNode copyRoot, HalTreeNode targetNode){
        HalNormalizeInvocationNode normalizedNode = HalNormalizeInvocationNode.of(targetNode);
        normalizedNode.getChildren().clear();
        copyRoot.replace(targetNode, normalizedNode);
        return normalizedNode;
    }

    private boolean canNormalizeArg(HalNode targetNode){
        boolean canNormalize = true;
        for(HalNode child : targetNode.getChildren()){
            if(!(child instanceof HalNormalizeNode)){
                canNormalize = false;
                break;
            }
        }
        return canNormalize;
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
                        copyInfoList.add(NormalizationInfo.of(NormalizationType.Name,normalizedNode.getId(),copyInfoList.size()));
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
