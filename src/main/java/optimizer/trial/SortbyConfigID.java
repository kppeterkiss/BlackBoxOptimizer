package optimizer.trial;

import java.util.Comparator;

public class SortbyConfigID implements Comparator<IterationResult>{
    @Override
    public int compare(IterationResult iterationResult, IterationResult t1) {
        return iterationResult.getConfiguration().get(0).getId()
                - t1.getConfiguration().get(0).getId();
    }
}
