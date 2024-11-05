package model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName="of")
public class Statement {
    String raw;
    Range range;

    @Override
    public String toString(){
        return raw;
    }
}
