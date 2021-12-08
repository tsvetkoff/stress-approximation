package ru.samgtu.pm.stress_approximation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Graph {

    public double eps_z;
    public final double[] sigma_res_theta;
    public final double[] sigma_res_r;
    public final double[] sigma_res_z;
    public final double[] q_theta;
    public final double[] q_r;
    public final double[] q_z;

    @JsonIgnore
    public final int N;

    @JsonIgnore
    public final double[] temp;

    @JsonIgnore
    public final double[] integral;

    public double sigma_zv;
    public double h0;
    public double h_zv;
    public double sigma0;
    public double sigma1;
    public double b;
    public double alpha;
    public double delta_theta;
    public double delta_z;
    public double delta;

    public Graph(int N) {
        this.N = N;
        this.sigma_res_theta = new double[N];
        this.sigma_res_r = new double[N];
        this.sigma_res_z = new double[N];
        this.q_theta = new double[N];
        this.q_r = new double[N];
        this.q_z = new double[N];
        this.temp = new double[N];
        this.integral = new double[N];
    }

    public static void copy(Graph source, Graph target) {
        target.sigma_zv = source.sigma_zv;
        target.h0 = source.h0;
        target.h_zv = source.h_zv;
        target.sigma0 = source.sigma0;
        target.sigma1 = source.sigma1;
        target.b = source.b;
        target.alpha = source.alpha;
        target.eps_z = source.eps_z;
        System.arraycopy(source.sigma_res_theta, 0, target.sigma_res_theta, 0, source.N);
        System.arraycopy(source.sigma_res_r, 0, target.sigma_res_r, 0, source.N);
        System.arraycopy(source.sigma_res_z, 0, target.sigma_res_z, 0, source.N);
        System.arraycopy(source.q_theta, 0, target.q_theta, 0, source.N);
        System.arraycopy(source.q_r, 0, target.q_r, 0, source.N);
        System.arraycopy(source.q_z, 0, target.q_z, 0, source.N);
        target.delta_theta = source.delta_theta;
        target.delta_z = source.delta_z;
        target.delta = source.delta;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Graph{");
        sb.append("sigma_zv=").append(sigma_zv);
        sb.append(", h0=").append(h0);
        sb.append(", h_zv=").append(h_zv);
        sb.append(", sigma0=").append(sigma0);
        sb.append(", sigma1=").append(sigma1);
        sb.append(", b=").append(b);
        sb.append(", alpha=").append(alpha);
        sb.append(", delta_theta=").append(delta_theta);
        sb.append(", delta_z=").append(delta_z);
        sb.append(", delta=").append(delta);
        sb.append('}');
        return sb.toString();
    }

}
