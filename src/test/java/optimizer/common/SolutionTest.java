package optimizer.common;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolutionTest {

    int dimension = 2;
    float[] lowerBounds = {1, 2};
    float[] upperBounds = {5, 10};
    Random randMock = mock(Random.class);

    @Test
    public void position_is_zero() {
        when(randMock.nextFloat()).thenReturn(0f);
        Solution sol = new Solution(dimension, lowerBounds, upperBounds, randMock);

        float[] expected = { 0, 0 };
        assertArrayEquals(expected, sol.position, 0);
    }

    @Test
    public void new_positions_are_at_the_bounds() {
        float min = 0f;
        when(randMock.nextFloat()).thenReturn(min);

        Solution sol1 = new Solution(dimension, lowerBounds, upperBounds, randMock);
        assertArrayEquals(lowerBounds, sol1.newPosition, 0);

        float max = 1f;
        when(randMock.nextFloat()).thenReturn(max);

        Solution sol2 = new Solution(dimension, lowerBounds, upperBounds, randMock);
        assertArrayEquals(upperBounds, sol2.newPosition, 0);
    }

    @Test
    public void new_positions_are_between_bounds() {
        when(randMock.nextFloat()).thenReturn(0.5f);
        Solution sol1 = new Solution(dimension, lowerBounds, upperBounds, randMock);

        float[] expected = { 3, 6 };
        assertArrayEquals(expected, sol1.newPosition, 0);

        sol1.newPosition[0] = 0;
        sol1.newPosition[1] = -2;

        sol1.checkBoundsForNewPosition(dimension, lowerBounds, upperBounds);
        assertArrayEquals(lowerBounds, sol1.newPosition, 0);

        sol1.newPosition[0] = 7;
        sol1.newPosition[1] = 11;

        sol1.checkBoundsForNewPosition(dimension, lowerBounds, upperBounds);
        assertArrayEquals(upperBounds, sol1.newPosition, 0);
    }
}