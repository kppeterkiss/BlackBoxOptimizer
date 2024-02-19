package optimizer.common;

import optimizer.param.Param;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class InternalStateBaseTest {
    List<Param> paramerters = new LinkedList<>();
    Param p1 = new Param(2f,3f,1f,"p1");
    Param p2 = new Param(5f,10f,4f,"p2");

    @Test
    public void constructor() {
        InternalStateBase<Solution> state = new InternalStateBase<>();
        assertNotNull(state.swarm);
        assertNull(state.swarmBestKnownPosition);
        assertNull(state.swarmBestFitness);
        assertTrue(state.init);
        assertNull(state.upperBounds);
        assertNull(state.lowerBounds);
        assertEquals(-1, state.dimension);
    }

    @Test
    public void searchSpace_is_set() {
        paramerters.add(p1);
        paramerters.add(p2);

        InternalStateBase<Solution> state = new InternalStateBase<>();
        state.initSearchSpace(paramerters);

        assertEquals(state.dimension, paramerters.size());

        for (int i = 0; i < paramerters.size(); ++i) {
            assertEquals(state.lowerBounds[i], paramerters.get(i).getLowerBound());
            assertEquals(state.upperBounds[i], paramerters.get(i).getUpperBound());
        }
    }

}