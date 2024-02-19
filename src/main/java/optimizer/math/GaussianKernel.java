/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package optimizer.math;

/**
 * k(u,v) = exp(-||u-v||^2 / 2*sigma^2)
 */
public class GaussianKernel implements MercerKernel<double[]>{
    private double gamma;

    public GaussianKernel(double sigma) {
        if(sigma <= 0) {
            throw new IllegalArgumentException("sigma is not positive");
        } else {
            this.gamma = 0.5 / (sigma * sigma);
        }
    }

    public double k(double[] x, double[] y) {
        if(x.length != y.length) {
            throw new IllegalArgumentException("arrays have different length");
        } else {
            double s = 0;
            for(int i = 0; i < x.length; ++i)
                s += (x[i]-y[i]) * (x[i]-y[i]);
            return Math.exp(-this.gamma * s);
        }
    }
}
