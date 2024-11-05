package model;

import lombok.ToString;
import lombok.Value;
import org.eclipse.jgit.diff.Edit;

import java.util.List;

@Value
@ToString(of = {"fileName","oldRange","oldStatements","newRange","newStatements"})
public class Chunk {
    String fileName;

    Range oldRange;
    List<Statement> oldStatements;

    Range newRange;
    List<Statement> newStatements;

    public static Chunk of(final String fileName, final List<Statement> oldStatements, final List<Statement> newStatements, Edit e){
        final List<Statement> oldSlice = oldStatements.subList(e.getBeginA(), e.getEndA());
        final List<Statement> newSlice = newStatements.subList(e.getBeginB(), e.getEndB());

        int oldBegin,oldEnd,newBegin,newEnd;

        if(oldSlice.isEmpty()){
            oldBegin = e.getBeginA() == oldStatements.size()
                    ? oldStatements.get(oldStatements.size()-1).getRange().getEnd()
                    : oldStatements.get(e.getBeginA()).getRange().getBegin();
            oldEnd = oldBegin;
        }else{
            oldBegin = oldSlice.get(0).getRange().getBegin();
            oldEnd = oldSlice.get(oldSlice.size()-1).getRange().getEnd();
        }

        if(newSlice.isEmpty()) {
            newBegin = e.getBeginB() == newStatements.size()
                    ? newStatements.get(newStatements.size() - 1).getRange().getEnd()
                    : newStatements.get(e.getBeginB()).getRange().getBegin();
            newEnd = newBegin;
        }else{
            newBegin = newSlice.get(0).getRange().getBegin();
            newEnd = newSlice.get(newSlice.size()-1).getRange().getEnd();
        }

        return new Chunk(fileName, Range.of(oldBegin, oldEnd), oldSlice, Range.of(newBegin, newEnd), newSlice);
    }
}