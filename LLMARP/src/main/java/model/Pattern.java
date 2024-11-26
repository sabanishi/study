package model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
    @Getter
    private final Set<Pattern> parents = new HashSet<>();

    private boolean isNormalized = false;

    public static Pattern of(HalNode oldTreeRoot, HalNode newTreeRoot, List<NormalizationInfo> appliedNormalizations) {
        Hash hash = Hash.of(Stream.of(oldTreeRoot.getHash(), newTreeRoot.getHash()));
        return new Pattern(oldTreeRoot, newTreeRoot, appliedNormalizations, hash);
    }

    public static Pattern of(HalNode oldTreeRoot, HalNode newTreeRoot, List<NormalizationInfo> appliedNormalizations, Hash hash) {
        return new Pattern(oldTreeRoot, newTreeRoot, appliedNormalizations, hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pattern pattern) {
            return oldTreeRoot.equals(pattern.oldTreeRoot) && newTreeRoot.equals(pattern.newTreeRoot);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return oldTreeRoot.hashCode() + newTreeRoot.hashCode();
    }

    /**
     * 自身に対して正規化を行い、その結果を引数のSetに追加する
     */
    public void normalize(Set<Pattern> result) {
        if (this.isNormalized) return;
        this.isNormalized = true;

        normalizeInternal(result);
        List<HashSet<Pattern>> parentsList = new ArrayList<>();
        for (Pattern pattern : result) {
            HashSet<Pattern> parents = new HashSet<>(result);
            parentsList.add(parents);
            pattern.normalize(parents);
        }
        for (HashSet<Pattern> parents : parentsList) {
            result.addAll(parents);
        }
    }

    private void normalizeInternal(Set<Pattern> result) {
        normalizeName(result);
        normalizeVariable(result);
        normalizeArg(result);
        normalizeMethod(result);
    }

    private void normalizeVariable(Set<Pattern> result) {
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                NormalizeNameInfo info = canNormalizeVariable(oldTargetNode);
                if (info.canNormalize) {
                    oldTargetNode = info.targetNode;
                    HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                    HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                    List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());
                    HalTreeNode normalizedNode = doNormalizeMethod(copyOldRoot, oldTargetNode);

                    //newTreeに同じIDのノードがあれば置換する
                    HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                    if (newTargetNode instanceof HalTreeNode) {
                        //子要素が全て正規化されている時、正規化を行う
                        NormalizeNameInfo newInfo = canNormalizeVariable(newTargetNode);
                        if (newInfo.canNormalize) {
                            doNormalizeMethod(copyNewRoot, (HalTreeNode) newTargetNode);
                        }
                    }

                    //正規化情報を追加
                    copyInfoList.add(NormalizationInfo.of(NormalizationType.Type, normalizedNode.getId(), copyInfoList.size()));
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                    addPatternToResultSet(copy, result);
                }
            }
        }
    }

    private NormalizeNameInfo canNormalizeVariable(HalNode targetNode) {
        if (!(targetNode instanceof HalTreeNode targetTreeNode)
                || targetNode instanceof HalNormalizeInvocationNode
                || targetNode instanceof HalEmptyNode) {
            return new NormalizeNameInfo(false, null);
        }

        if (targetNode instanceof HalNormalizeNode) return new NormalizeNameInfo(true, targetTreeNode);

        for (HalNode child : targetNode.getChildren()) {
            NormalizeNameInfo childInfo = canNormalizeVariable(child);
            if (childInfo.canNormalize) {
                return childInfo;
            }
        }
        return new NormalizeNameInfo(false, null);
    }

    private void normalizeMethod(Set<Pattern> result) {
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                //子要素が存在しない、またはHalNormalizeInvocationNodeの時、正規化を行う
                NormalizeMethodInfo info = canNormalizeMethod(oldTargetNode);

                if (info.canNormalize) {
                    HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                    HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                    List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());
                    HalTreeNode normalizedNode = doNormalizeMethod(copyOldRoot, oldTargetNode);

                    //newTreeに同じIDのノードがあれば置換する
                    HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                    if (newTargetNode instanceof HalTreeNode) {
                        //子要素が全て正規化されている時、正規化を行う
                        NormalizeMethodInfo newInfo = canNormalizeMethod(newTargetNode);
                        if (newInfo.canNormalize && newInfo.methodName.equals(info.methodName)) {
                            doNormalizeMethod(copyNewRoot, (HalTreeNode) newTargetNode);
                        }
                    }

                    //正規化情報を追加
                    copyInfoList.add(NormalizationInfo.of(NormalizationType.Method, normalizedNode.getId(), copyInfoList.size()));
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                    addPatternToResultSet(copy, result);
                }
            }
        }
    }

    private HalTreeNode doNormalizeMethod(HalNode copyRoot, HalTreeNode targetNode) {
        HalEmptyNode newNode = HalEmptyNode.of(targetNode);
        newNode.getChildren().clear();

        copyRoot.replace(targetNode, newNode);
        return newNode;
    }

    private NormalizeMethodInfo canNormalizeMethod(HalNode targetNode) {
        if (!(targetNode instanceof HalTreeNode targetTreeNode)
                || targetNode instanceof HalNormalizeNode
                || targetNode instanceof HalNormalizeInvocationNode
                || targetNode instanceof HalEmptyNode) {
            return new NormalizeMethodInfo(false, "", "");
        }

        if (!targetTreeNode.getType().equals("MethodInvocation")) {
            return new NormalizeMethodInfo(false, "", "");
        }

        boolean canNormalize = true;
        String methodName = "";
        String receiverName = "";
        for (HalNode child : targetNode.getChildren()) {
            if (child instanceof HalTreeNode childNode) {
                //子要素が引数ノードだったら正規化しない
                if (childNode.getType().equals("METHOD_INVOCATION_ARGUMENTS")) {
                    canNormalize = false;
                    break;
                }
                //子要素がReceiverであり、かつそれが変数でない時、正規化しない
                if (childNode.getType().equals("METHOD_INVOCATION_RECEIVER")) {
                    if (!childNode.getChildren().isEmpty()) {
                        if (childNode.getChildren().get(0) instanceof HalTreeNode receiverNode) {
                            if (!receiverNode.getType().equals("SimpleName")) {
                                canNormalize = false;
                                break;
                            } else {
                                receiverName = receiverNode.getLabel();
                            }
                        }
                    }
                }

                if (childNode.getType().equals("SimpleName")) {
                    methodName = childNode.getLabel();
                }
            }
        }
        return new NormalizeMethodInfo(canNormalize, methodName, receiverName);
    }

    private void normalizeArg(Set<Pattern> result) {
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                //子要素が全て正規化されている時、正規化を行う
                if (canNormalizeArg(oldTargetNode)) {
                    HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                    HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                    List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());

                    HalNormalizeInvocationNode normalizedNode = doNormalizeArg(copyOldRoot, oldTargetNode);

                    //newTreeに同じIDのノードがあれば置換する
                    HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                    if (oldTargetNode.equals(newTargetNode)) {
                        //newTargetNodeとoldTargetNodeが同じノードの時
                        if (canNormalizeArg(newTargetNode)) {
                            //newTargetNodeとoldTargetNodeが同じ時
                            doNormalizeArg(copyNewRoot, (HalTreeNode) newTargetNode);
                        }
                    }

                    //正規化情報を追加
                    copyInfoList.add(NormalizationInfo.of(NormalizationType.Argument, normalizedNode.getId(), copyInfoList.size()));
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                    addPatternToResultSet(copy, result);
                }
            }
        }
    }

    private HalNormalizeInvocationNode doNormalizeArg(HalNode copyRoot, HalTreeNode targetNode) {
        HalNormalizeInvocationNode normalizedNode = HalNormalizeInvocationNode.of(targetNode);
        normalizedNode.getChildren().clear();
        copyRoot.replace(targetNode, normalizedNode);
        return normalizedNode;
    }

    private boolean canNormalizeArg(HalNode targetNode) {
        if (!(targetNode instanceof HalTreeNode targetTreeNode)
                || targetNode instanceof HalNormalizeNode
                || targetNode instanceof HalNormalizeInvocationNode
                || targetNode instanceof HalEmptyNode) {
            return false;
        }

        if (!targetTreeNode.getType().equals("METHOD_INVOCATION_ARGUMENTS")) {
            return false;
        }

        for (HalNode child : targetNode.getChildren()) {
            if (canNormalizeName(child).canNormalize
                    || canNormalizeArg(child)
                    || canNormalizeMethod(child).canNormalize
                    || canNormalizeVariable(child).canNormalize) {
                return false;
            }
        }
        return true;
    }

    private void normalizeName(Set<Pattern> result) {
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                NormalizeNameInfo info = canNormalizeName(oldTargetNode);
                if (info.canNormalize) {
                    oldTargetNode = info.targetNode;
                    HalNormalizeNode normalizedNode = HalNormalizeNode.of(oldTargetNode);
                    HalNode copyOldRoot = getOldTreeRoot().deepCopy();
                    HalNode copyNewRoot = getNewTreeRoot().deepCopy();
                    List<NormalizationInfo> copyInfoList = new ArrayList<>(getAppliedNormalizations());
                    //oldTreeの該当箇所を置換する
                    copyOldRoot.replace(oldTargetNode, normalizedNode);

                    //newTreeに同じIDのノードがあれば置換する
                    HalNode newTargetNode = copyNewRoot.searchById(normalizedNode.getId());
                    if (newTargetNode instanceof HalTreeNode) {
                        HalNormalizeNode normalizeNode2 = HalNormalizeNode.of((HalTreeNode) newTargetNode);
                        copyNewRoot.replace(newTargetNode, normalizeNode2);
                    }

                    //正規化情報を追加
                    copyInfoList.add(NormalizationInfo.of(NormalizationType.Label, normalizedNode.getId(), copyInfoList.size()));
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList);
                    addPatternToResultSet(copy, result);
                }
            }
        }
    }

    private NormalizeNameInfo canNormalizeName(HalNode targetNode) {
        if (targetNode instanceof HalTreeNode targetTreeNode
                && !(targetNode instanceof HalNormalizeNode)
                && !(targetNode instanceof HalNormalizeInvocationNode)
                && !(targetNode instanceof HalEmptyNode)) {
            switch (targetTreeNode.getType()) {
                case "SimpleName":
                    if (targetNode.getParent() != null) {
                        String parentType = targetNode.getParent().getType();
                        //親がメソッドかクラスの時、正規化を行わない
                        if (parentType.equals("MethodInvocation") || parentType.equals("TypeDeclaration")) {
                            return new NormalizeNameInfo(false, null);
                        }
                    }
                case "StringLiteral":
                case "CharacterLiteral":
                case "NumberLiteral":
                case "BooleanLiteral":
                    return new NormalizeNameInfo(true, targetTreeNode);
                default:
                    for (HalNode child : targetNode.getChildren()) {
                        NormalizeNameInfo childInfo = canNormalizeName(child);
                        if (childInfo.canNormalize) {
                            return childInfo;
                        }
                    }
            }
        }
        return new NormalizeNameInfo(false, null);
    }

    private void addPatternToResultSet(Pattern target, Set<Pattern> result) {
        if (!result.contains(target)) {
            target.getParents().add(this);
            result.add(target);
        } else {
            for (Pattern pattern : result) {
                if (pattern.hashCode() == target.hashCode()) {
                    pattern.getParents().add(this);
                    break;
                }
            }
        }
    }

    public record NormalizeNameInfo(boolean canNormalize, HalTreeNode targetNode) {
    }

    public record NormalizeMethodInfo(boolean canNormalize, String methodName, String receiverName) {
    }
}
