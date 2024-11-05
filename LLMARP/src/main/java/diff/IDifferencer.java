package diff;

import org.eclipse.jgit.diff.EditList;

import java.util.List;

/**
 * 2つの文の差分を計算するインターフェース
 */
public interface IDifferencer<T>{
    EditList compute(final List<T> left, final List<T> right);
}
