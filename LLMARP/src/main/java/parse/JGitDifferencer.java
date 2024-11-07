package parse;

import lombok.Getter;
import org.eclipse.jgit.diff.*;

import java.util.List;

public class JGitDifferencer<T> implements IDifferencer<T> {
    private final DiffAlgorithm algorithm;
    private final ListSequenceComparator<T> comparator = new ListSequenceComparator<>();

    public JGitDifferencer(final DiffAlgorithm.SupportedAlgorithm alg) {
        this.algorithm = DiffAlgorithm.getAlgorithm(alg);
    }

    public JGitDifferencer(){
        this(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM);
    }

    @Override
    public EditList compute(List<T> left, List<T> right) {
        EditList result = algorithm.diff(comparator, new ListSequence<>(left), new ListSequence<>(right));
        mergeAdjacentEdits(result);
        return result;
    }

    /**
     * 隣接するEditをマージする<br />
     * 引数のEditListの内容を書き換えるので注意
     */
    private static void mergeAdjacentEdits(final EditList edits){
        int i = 0;
        while(i < edits.size()-1){
            Edit e1 = edits.get(i);
            Edit e2 = edits.get(i+1);
            if(e1.getEndA() == e2.getBeginA() && e1.getEndB() == e2.getBeginB()) {
                //e1、e2を削除し、両者を統合した新しいEditを追加
                edits.remove(i);
                edits.remove(i);
                edits.add(i, new Edit(e1.getBeginA(), e2.getEndA(), e1.getBeginB(), e2.getEndB()));
            }else{
                i++;
            }
        }
    }

    @Getter
    public static class ListSequence<T> extends Sequence {
        private final List<T> elements;

        public ListSequence(final List<T> elements) {
            this.elements = elements;
        }

        public T get(final int index) {
            return elements.get(index);
        }

        @Override
        public int size() {
            return elements.size();
        }
    }

    public static class ListSequenceComparator<T> extends SequenceComparator<ListSequence<T>> {
        @Override
        public boolean equals(final ListSequence<T> a, final int ai, final ListSequence<T> b, final int bi) {
            return a.get(ai).equals(b.get(bi));
        }

        @Override
        public int hash(final ListSequence<T> seq, int ptr) {
            return seq.get(ptr).hashCode();
        }
    }
}
