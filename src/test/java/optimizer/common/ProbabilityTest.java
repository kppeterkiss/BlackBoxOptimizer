package optimizer.common;

import org.junit.Test;

import javax.xml.bind.Element;
import java.util.Collections;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class ProbabilityTest {
    @Test
    public void test_compare() {
        LinkedList<Probability> probs = new LinkedList<>();
        probs.add(new Probability(0, 0.0));
        probs.add(new Probability(1, 0.3));
        probs.add(new Probability(2, 0.4));
        probs.add(new Probability(3, 0.2));
        probs.add(new Probability(4, 0.1));

        Collections.sort(probs);

        assertTrue(probs.get(0).id == 0);
        assertTrue(probs.get(1).id == 4);
        assertTrue(probs.get(2).id == 3);
        assertTrue(probs.get(3).id == 1);
        assertTrue(probs.get(4).id == 2);

        probs.forEach(e -> System.out.println(e.toString()));
    }

}