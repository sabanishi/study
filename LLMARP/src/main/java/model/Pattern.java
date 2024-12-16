package model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import model.tree.*;
import util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Pattern {
    private final int MAX_LITERAL = 5;

    @Getter
    private final HalNode oldTreeRoot;
    @Getter
    private final HalNode newTreeRoot;
    @Getter
    private final List<NormalizationInfo> appliedNormalizations;
    @Getter
    private final Hash hash;
    private final boolean isCandidate;
    @Getter
    private final Set<Pattern> parents = new HashSet<>();

    //NOTE:なぜか@GetterだとDaoのinsertPatternでエラーが出るので明示的にgetterを作成
    public boolean getIsCandidate(){
        return isCandidate;
    }

    private boolean isNormalized = false;

    public static Pattern of(HalNode oldTreeRoot, HalNode newTreeRoot, List<NormalizationInfo> appliedNormalizations,boolean isCandidate) {
        Hash hash = Hash.of(Stream.of(oldTreeRoot.getHash(), newTreeRoot.getHash()));
        return new Pattern(oldTreeRoot, newTreeRoot, appliedNormalizations, hash,isCandidate);
    }

    public static Pattern of(HalNode oldTreeRoot, HalNode newTreeRoot, List<NormalizationInfo> appliedNormalizations, Hash hash,boolean isCandidate) {
        return new Pattern(oldTreeRoot, newTreeRoot, appliedNormalizations, hash,isCandidate);
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
        return 31*oldTreeRoot.hashCode() + newTreeRoot.hashCode();
    }

    /**
     * 自身に対して正規化を行い、その結果を引数のSetに追加する
     */
    public void normalize(Set<Pattern> result) {
        //変数名だけ先に全て正規化する
        Pattern variablePattern = normalizeVariables();
        result.add(variablePattern);

        //変更パターン候補数の爆発を抑えるため、リテラルの数が一定数以下の時のみ、正規化を行う
        if(countUpLiteral()> MAX_LITERAL) return;

        //正規化パターン候補のSetを生成する
        variablePattern.normalizeInternal(result);
    }

    /**
     * 正規化対象となるリテラルの数を数える
     */
    private int countUpLiteral(){
        int count = 0;
        for(HalNode oldNode : getOldTreeRoot().preOrder()){
            if(canNormalizeLiteral(oldNode).canNormalize){
                count++;
            }
        }
        return count;
    }

    /**
     *
     * @param allResult 自分の親、祖父も含めた全ての子供
     */
    private void normalizeInternal(Set<Pattern> allResult){
        if (this.isNormalized) return;
        this.isNormalized = true;

        Set<Pattern> result = new HashSet<>();
        doNormalize(result);
        for (Pattern pattern : result) {
            allResult.add(pattern);
            pattern.normalizeInternal(allResult);
        }
    }

    /**
     * 正規化を行える箇所、1箇所に対してのみ正規化を施す
     */
    private void doNormalize(Set<Pattern> result) {
        normalizeLiteral(result);
    }

    private void normalizeLiteral(Set<Pattern> result){
        //リテラルの正規化を行う
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                NormalizeNameInfo info = canNormalizeLiteral(oldTargetNode);
                if(info.canNormalize){
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
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList,true);
                    addPatternToResultSet(copy, result);
                }
            }
        }
    }

    private NormalizeNameInfo canNormalizeLiteral(HalNode targetNode){
        if (targetNode instanceof HalTreeNode targetTreeNode
                && !(targetNode instanceof HalNormalizeNode)
                && !(targetNode instanceof HalNormalizeInvocationNode)
                && !(targetNode instanceof HalEmptyNode)) {

            //一番外側のリテラルは正規化しない
            if(targetTreeNode.getId()==0)return new NormalizeNameInfo(false, null);

            switch (targetTreeNode.getType()) {
                case "StringLiteral":
                case "CharacterLiteral":
                case "NumberLiteral":
                case "BooleanLiteral":
                    return new NormalizeNameInfo(true, targetTreeNode);
            }
        }
        return new NormalizeNameInfo(false, null);
    }

    /**
     * 変数ノードのLabelを全て正規化する
     */
    private Pattern normalizeVariables(){
        Set<Pair<HalNode,HalNode>> replaceOldSet = new HashSet<>();
        Set<Pair<HalNode,HalNode>> replaceNewSet = new HashSet<>();

        List<NormalizationInfo> copyInfoList = new ArrayList<>();
        HalNode copyOld = getOldTreeRoot().deepCopy();
        HalNode copyNew = getNewTreeRoot().deepCopy();
        Pattern copy = Pattern.of(copyOld,copyNew, copyInfoList,false);

        for(HalNode oldNode : copyOld.preOrder()){
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                NormalizeNameInfo info = canNormalizeVariable(oldTargetNode);
                if (info.canNormalize) {
                    oldTargetNode = info.targetNode;
                    HalNormalizeNode normalizedNode = HalNormalizeNode.of(oldTargetNode);
                    //oldTreeの該当箇所を置換する
                    replaceOldSet.add(Pair.of(oldTargetNode,normalizedNode));

                    //newTreeに同じIDのノードがあれば置換する
                    HalNode newTargetNode = copyNew.searchById(normalizedNode.getId());
                    if (newTargetNode instanceof HalTreeNode) {
                        HalNormalizeNode normalizeNode2 = HalNormalizeNode.of((HalTreeNode) newTargetNode);
                        replaceNewSet.add(Pair.of(newTargetNode,normalizeNode2));
                    }

                    //正規化情報を追加
                    copyInfoList.add(NormalizationInfo.of(NormalizationType.Label, normalizedNode.getId(), copyInfoList.size()));
                }
            }
        }

        //Old、Newの置換を行う
        for(Pair<HalNode,HalNode> pair : replaceOldSet){
            copy.getOldTreeRoot().replace(pair.getFirst(),pair.getSecond());
        }
        for(Pair<HalNode,HalNode> pair : replaceNewSet){
            copy.getNewTreeRoot().replace(pair.getFirst(),pair.getSecond());
        }

        return copy;
    }

    private NormalizeNameInfo canNormalizeVariable(HalNode targetNode) {
        if (targetNode instanceof HalTreeNode targetTreeNode
                && !(targetNode instanceof HalNormalizeNode)
                && !(targetNode instanceof HalNormalizeInvocationNode)
                && !(targetNode instanceof HalEmptyNode)) {

            //一番外側の変数は正規化しない
            if(targetTreeNode.getId()==0)return new NormalizeNameInfo(false, null);

            if((targetTreeNode.getType().equals("SimpleName"))){
                if (targetNode.getParent() != null) {
                    String parentType = targetNode.getParent().getType();
                    //メソッドの引数または変数宣言の右辺の時のみ、正規化を行う
                    switch(parentType){
                        case "METHOD_INVOCATION_RECEIVER":
                        case "METHOD_INVOCATION_ARGUMENTS":
                        case "SingleVariableDeclaration":
                        case "VariableDeclarationFragment":
                        case "ReturnStatement":
                            return new NormalizeNameInfo(true, targetTreeNode);
                        default:
                            break;
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

    /**
     * 正規化則4
     */
    private void normalizeEmptyNode(Set<Pattern> result) {
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                NormalizeNameInfo info = canNormalizeEmptyNode(oldTargetNode);
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
                        NormalizeNameInfo newInfo = canNormalizeEmptyNode(newTargetNode);
                        if (newInfo.canNormalize) {
                            doNormalizeMethod(copyNewRoot, (HalTreeNode) newTargetNode);
                        }
                    }

                    //正規化情報を追加
                    copyInfoList.add(NormalizationInfo.of(NormalizationType.Type, normalizedNode.getId(), copyInfoList.size()));
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList,true);
                    addPatternToResultSet(copy, result);
                }
            }
        }
    }

    private NormalizeNameInfo canNormalizeEmptyNode(HalNode targetNode) {
        if (!(targetNode instanceof HalTreeNode targetTreeNode)
                || targetNode instanceof HalNormalizeInvocationNode
                || targetNode instanceof HalEmptyNode) {
            return new NormalizeNameInfo(false, null);
        }

        if (targetNode instanceof HalNormalizeNode) return new NormalizeNameInfo(true, targetTreeNode);

        for (HalNode child : targetNode.getChildren()) {
            NormalizeNameInfo childInfo = canNormalizeEmptyNode(child);
            if (childInfo.canNormalize) {
                return childInfo;
            }
        }
        return new NormalizeNameInfo(false, null);
    }

    /**
     * 正規化則3
     */
    private void normalizeMethod(Set<Pattern> result) {
        for (HalNode oldNode : getOldTreeRoot().preOrder()) {
            //一番外側のメソッドは正規化しない
            if(oldNode.getId()==0)continue;

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
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList,true);
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

    /**
     * 正規化則2
     */
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
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList,true);
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
                    || canNormalizeEmptyNode(child).canNormalize) {
                return false;
            }
        }
        return true;
    }

    /**
     * 正規化則1
     */
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
                    Pattern copy = Pattern.of(copyOldRoot, copyNewRoot, copyInfoList,true);
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

    public record NormalizeNameInfo(boolean canNormalize, HalTreeNode targetNode) {
    }

    public record NormalizeMethodInfo(boolean canNormalize, String methodName, String receiverName) {
    }
}
