package ru.samgtu.pm.stress_approximation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.samgtu.pm.stress_approximation.util.MathUtils;

import java.util.Arrays;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Params {

    public double R1;
    public double R2;
    public double dr;
    public double E;
    public double mu;

    public double[] r_theta_exp;
    public double[] sigma_theta_exp;
    public double[] r_z_exp;
    public double[] sigma_z_exp;

    public double sigma_zv_start;
    public double sigma_zv_end;
    public double sigma_zv_step;
    public double h0_start;
    public double h0_end;
    public double h0_step;
    public double h_zv_start;
    public double h_zv_end;
    public double h_zv_step;
    public double b_start;
    public double b_end;
    public double b_step;
    public double alpha_start;
    public double alpha_end;
    public double alpha_step;

    public double[] r;

    @JsonIgnore
    public int N;

    @JsonIgnore
    public double H;

    /**
     * Массив индексов r_theta_exp в массиве r.
     */
    @JsonIgnore
    public int[] r_theta_calc_index;

    /**
     * Массив индексов r_z_exp в массиве r.
     */
    @JsonIgnore
    public int[] r_z_calc_index;

    @JsonIgnore
    public double delta_theta_exp;

    @JsonIgnore
    public double delta_z_exp;

    @JsonIgnore
    public double[] h0_array;

    @JsonIgnore
    public double[] h_zv_array;

    @JsonIgnore
    public double[] sigma_zv_array;

    @JsonIgnore
    public double[] b_array;

    @JsonIgnore
    public double[] alpha_array;

    public long totalIterations;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Params{");
        sb.append("R1=").append(R1);
        sb.append(", R2=").append(R2);
        sb.append(", dr=").append(dr);
        sb.append(", E=").append(E);
        sb.append(", mu=").append(mu);
        sb.append(", r_theta_exp=").append(Arrays.toString(r_theta_exp));
        sb.append(", sigma_theta_exp=").append(Arrays.toString(sigma_theta_exp));
        sb.append(", r_z_exp=").append(Arrays.toString(r_z_exp));
        sb.append(", sigma_z_exp=").append(Arrays.toString(sigma_z_exp));
        sb.append(", sigma_zv_start=").append(sigma_zv_start);
        sb.append(", sigma_zv_end=").append(sigma_zv_end);
        sb.append(", sigma_zv_step=").append(sigma_zv_step);
        sb.append(", h0_start=").append(h0_start);
        sb.append(", h0_end=").append(h0_end);
        sb.append(", h0_step=").append(h0_step);
        sb.append(", h_zv_start=").append(h_zv_start);
        sb.append(", h_zv_end=").append(h_zv_end);
        sb.append(", h_zv_step=").append(h_zv_step);
        sb.append(", b_start=").append(b_start);
        sb.append(", b_end=").append(b_end);
        sb.append(", b_step=").append(b_step);
        sb.append(", alpha_start=").append(alpha_start);
        sb.append(", alpha_end=").append(alpha_end);
        sb.append(", alpha_step=").append(alpha_step);
        sb.append(", totalIterations=").append(totalIterations);
        sb.append('}');
        return sb.toString();
    }

    public void init() {
        H = R2 - R1;
        N = (int) MathUtils.round(H / dr) + 1;
        r = new double[N];
        for (int j = 0; j < N; j++) {
            r[j] = MathUtils.round(R1 + j * dr);
        }

        if (r_theta_exp.length != sigma_theta_exp.length) {
            throw new IllegalArgumentException(
                    "Arrays r_theta_exp and sigma_theta_exp have different lengths: " + r_theta_exp.length + " and " + sigma_theta_exp.length
            );
        }
        r_theta_calc_index = new int[r_theta_exp.length];
        for (int i = 0; i < r_theta_exp.length; i++) {
            int index = Arrays.binarySearch(r, r_theta_exp[i]);
            if (index < 0) {
                throw new IllegalArgumentException(
                        "Element r_theta_exp=" + r_theta_exp[i] + " not found in r=[" + r[0] + ", " + r[1] + " .." + r[N - 2] + ", " + r[N - 1] + "]"
                );
            }
            r_theta_calc_index[i] = index;
        }

        if (r_z_exp.length != sigma_z_exp.length) {
            throw new IllegalArgumentException(
                    "Arrays r_z_exp and sigma_z_exp have different lengths: " + r_z_exp.length + " and " + sigma_z_exp.length
            );
        }
        r_z_calc_index = new int[r_z_exp.length];
        for (int i = 0; i < r_z_exp.length; i++) {
            int index = Arrays.binarySearch(r, r_z_exp[i]);
            if (index < 0) {
                throw new IllegalArgumentException(
                        "Element r_z_exp=" + r_z_exp[i] + " not found in r=[" + r[0] + ", " + r[1] + " .." + r[N - 2] + ", " + r[N - 1] + "]"
                );
            }
            r_z_calc_index[i] = index;
        }

        delta_theta_exp = sqrt(Arrays.stream(sigma_theta_exp).map(v -> pow(v, 2)).sum());
        delta_z_exp = sqrt(Arrays.stream(sigma_z_exp).map(v -> pow(v, 2)).sum());

        h0_array = fillArray(h0_start, h0_end, h0_step);
        h_zv_array = fillArray(h_zv_start, h_zv_end, h_zv_step);
        sigma_zv_array = fillArray(sigma_zv_start, sigma_zv_end, sigma_zv_step);
        b_array = fillArray(b_start, b_end, b_step);
        alpha_array = fillArray(alpha_start, alpha_end, alpha_step);

        totalIterations = (long) h0_array.length * h_zv_array.length * sigma_zv_array.length * alpha_array.length;
    }

    private double[] fillArray(double start, double end, double step) {
        if (start == end) {
            return new double[]{start};
        }

        double[] array = new double[(int) MathUtils.round((end - start) / step) + 1];
        for (int i = 0; i < array.length; i++) {
            array[i] = MathUtils.round(start + i * step);
        }
        return array;
    }

}
