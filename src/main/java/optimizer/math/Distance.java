package optimizer.math;

public class Distance {
    public static double Minkowski(int dim, double[] a, double[] b) {
        double ret = 0;
        for (int i = 0; i < dim; ++i) {
            ret += Math.pow(Math.pow(Math.abs(a[i] - b[i]), dim), 1 / dim);
        }
        return ret;
    }

    public static float distance(int dim, float[] a, float[] b) {
        float dist = 0;
        for(int i = 0; i < dim; i++) {
            dist += Math.pow((a[i] - b[i]), 2);
        }
        return (float) Math.sqrt(dist);
    }
}
