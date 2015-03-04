/******************************************************************************
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
package de.bb.benchmark;

import java.util.ArrayList;

public class Bench {

    public static int INTERVALL = 1000;
    private static ArrayList<Result> results = new ArrayList<Result>();

    public static void bench(Mark mark) {
        System.out.println("starting \"" + mark.getName() + "\"");
        // ramp up
        double ppm = 0, bestppm = 0;
        for (;;) {
            ppm = one(mark);
            if (ppm <= bestppm)
                break;
            bestppm = ppm;
        }

        double sum = ppm + bestppm;
        double sum2 = ppm * ppm + bestppm * bestppm;
        int n = 2;

        for (;;) {
            double m = sum / n;
            double s = Math.sqrt(sum * sum - sum2) / n / n;
            if (s * 20 < m) {
                Result r = new Result(sum, sum2, n);
                results.add(r);
                System.out.println(r);
                break;
            }
            ppm = one(mark);
            sum += ppm;
            sum2 += ppm * ppm;
            ++n;
        }
        System.out.println("========");
        System.out.println("Summary:");
        for (final Result r : results) {
            System.out.println(r);
        }
    }

    public static double one(Mark mark) {
        mark.action(0);
        mark.action(1);

        long start = System.currentTimeMillis();
        long end = start + INTERVALL; // run 5 seconds
        long now = start;
        int passes = 1;
        long total = 0;
        do {
            total += passes;
            for (int i = 0; i < passes; ++i) {
                mark.action(i);
            }
            now = System.currentTimeMillis();

            int rest = (int) (end - now);
            if (rest <= 0)
                break;
            if (rest > 200)
                rest = 200;

            int spent = (int) (now - start);
            if (spent == 0 || rest > 800)
                passes += passes;
            else
                passes = (int) (total * rest / spent);
        } while (now < end);
        end = now - start;
        mark.end();
        System.out.println("did   " + total + " passes in " + end + "ms");
        double ppm = (double) total / end;
        System.out.println("pass: " + (long) ppm + " passes per ms");
        return ppm;
    }

    static class Result {

        private double sum;
        private double sum2;
        private int n;

        public Result(double sum, double sum2, int n) {
            this.sum = sum;
            this.sum2 = sum2;
            this.n = n;
        }

        public String toString() {
            double s = Math.sqrt(sum * sum - sum2) / n / n;
            return "mean: " + (long) (sum / n) + " (+-" + (long) s
                    + ") passes per ms\r\n";
        }
    }
}
