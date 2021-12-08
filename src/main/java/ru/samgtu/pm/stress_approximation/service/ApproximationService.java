package ru.samgtu.pm.stress_approximation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.samgtu.pm.stress_approximation.dto.Graph;
import ru.samgtu.pm.stress_approximation.dto.Params;
import ru.samgtu.pm.stress_approximation.dto.Result;
import ru.samgtu.pm.stress_approximation.util.MathUtils;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static org.apache.commons.math3.special.Erf.erf;

@Service
public class ApproximationService {

    private final Logger log = LoggerFactory.getLogger(ApproximationService.class);

    private final AtomicReference<Graph> bestGraph = new AtomicReference<>();
    private final AtomicLong currentIterations = new AtomicLong();
    private Params params;
    private ForkJoinPool threadPool;

    @PreDestroy
    public void destroy() {
        cancelOptimization();
    }

    public Result getResult() {
        if (params == null) {
            return null;
        }
        Result result = new Result();
        result.setParams(params);
        result.setGraph(bestGraph.getAndUpdate(currentGraph -> {
            Graph graph = new Graph(currentGraph.N);
            Graph.copy(currentGraph, graph);
            return graph;
        }));
        result.setPercent((byte) (100 * currentIterations.get() / params.totalIterations));
        result.setCanceled(threadPool.isShutdown());
        return result;
    }

    /**
     * Запуск оптимизации параметров аппроксимации.
     *
     * @return Promise-объект
     */
    public Future<?> runOptimization(Params params) {
        this.params = params;
        Graph initialGraph = new Graph(params.N);
        initialGraph.delta = Double.MAX_VALUE;
        this.bestGraph.set(initialGraph);
        this.currentIterations.set(0);
        this.threadPool = new ForkJoinPool();
        log.info("ForJoinPool with parallelism={} created", threadPool.getParallelism());
        return threadPool.submit(() -> {
            long time = System.currentTimeMillis();
            log.info("Start calculation with {}", params);

            Arrays.stream(params.h0_array).parallel().forEach(h0 -> {
                Graph graph = new Graph(params.N);
                for (double h_zv : params.h_zv_array) {
                    double b = findB(params, h0, h_zv);
                    if (b == -1) {
                        continue;
                    }
                    for (double sigma_zv : params.sigma_zv_array) {
                        double sigma1 = sigma_zv / (exp(-pow((h0 - h_zv) / b, 2)) - 1);
                        double sigma0 = sigma_zv + sigma1;
                        for (double alpha : params.alpha_array) {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }

                            log.debug(String.format("h0 = %f, h_zv = %s, sigma_zv = %f, alpha = %f", h0, h_zv, sigma_zv, alpha));
                            graph.sigma_zv = sigma_zv;
                            graph.h0 = h0;
                            graph.h_zv = h_zv;
                            graph.sigma0 = sigma0;
                            graph.sigma1 = sigma1;
                            graph.b = b;
                            graph.alpha = alpha;
                            hardening(params, graph);
                            calculateDeltas(params, graph);
                            bestGraph.accumulateAndGet(graph, (currentGraph, newGraph) -> {
                                if (newGraph.delta < currentGraph.delta) {
                                    Graph.copy(newGraph, currentGraph);
                                }
                                return currentGraph;
                            });
                            currentIterations.incrementAndGet();
                        }
                    }
                }
            });
            currentIterations.set(params.totalIterations);

            log.info("Finish calculation within {} ms", System.currentTimeMillis() - time);
        });
    }

    /**
     * Отмена расчетов.
     */
    public void cancelOptimization() {
        if (threadPool != null) {
            log.info("Cancel previous calculation");
            threadPool.shutdownNow();
        }
    }

    public void hardening(Params params, Graph graph) {
        double nu = (2 + graph.alpha) / (1 + graph.alpha * params.mu);

        for (int j = 0; j < params.N; j++) {
            graph.sigma_res_theta[j] = graph.sigma0 - graph.sigma1 * exp(-pow((params.R2 - graph.h_zv - params.r[j]) / graph.b, 2));
        }

        MathUtils.varIntegral(graph.sigma_res_theta, params.dr, graph.integral);
        for (int j = 0; j < params.N; j++) {
            if (params.R1 == 0 && j == 0) {
                graph.sigma_res_r[0] = graph.sigma_res_theta[0];
            } else {
                graph.sigma_res_r[j] = graph.integral[j] / params.r[j];
            }
        }
        graph.sigma_res_r[params.N - 1] = 0;

        for (int j = 0; j < params.N; j++) {
            if (params.R1 == 0 && j == 0) {
                graph.temp[j] = 0;
            } else {
                double diff_sigma_res_theta;
                if (j == 0) {
                    diff_sigma_res_theta = (-3 * graph.sigma_res_theta[0] + 4 * graph.sigma_res_theta[1] - graph.sigma_res_theta[2]) / (2 * params.dr);
                } else if (j == params.N - 1) {
                    diff_sigma_res_theta = (3 * graph.sigma_res_theta[params.N - 1] - 4 * graph.sigma_res_theta[params.N - 2] + graph.sigma_res_theta[params.N - 3]) / (2 * params.dr);
                } else {
                    diff_sigma_res_theta = (graph.sigma_res_theta[j + 1] - graph.sigma_res_theta[j - 1]) / (2 * params.dr);
                }
                double diff_sigma_res_r = (graph.sigma_res_theta[j] - graph.sigma_res_r[j]) / params.r[j];
                graph.temp[j] = pow(params.r[j], nu - 1) * (graph.sigma_res_r[j] - graph.sigma_res_theta[j] - params.r[j] * ((1 - params.mu) * diff_sigma_res_theta - params.mu * diff_sigma_res_r));
            }
        }

        MathUtils.varIntegral(graph.temp, params.dr, graph.integral);
        for (int j = 0; j < params.N; j++) {
            if (params.R1 == 0 && j == 0) {
                graph.q_theta[0] = 0;
            } else {
                graph.q_theta[j] = (1 + params.mu) / (params.E * (1 + graph.alpha * params.mu)) * pow(params.r[j], -nu) * graph.integral[j];
            }
        }

        for (int j = 0; j < params.N; j++) {
            graph.q_z[j] = graph.alpha * graph.q_theta[j];
            graph.q_r[j] = -graph.q_theta[j] * (1 + graph.alpha);
            graph.temp[j] = params.r[j] * (graph.q_z[j] - params.mu / params.E * (graph.sigma_res_r[j] + graph.sigma_res_theta[j]));
        }

        double eps_z = 2 / (pow(params.R2, 2) - pow(params.R1, 2)) * MathUtils.defIntegral(graph.temp, params.dr);
        graph.eps_z = eps_z;
        for (int j = 0; j < params.N; j++) {
            graph.sigma_res_z[j] = params.E * (eps_z - graph.q_z[j]) + params.mu * (graph.sigma_res_r[j] + graph.sigma_res_theta[j]);
        }

//        System.out.println("r\tsigma_res_theta\tsigma_res_r\tsigma_res_z\tq_theta\tq_r\tq_z");
//        for (int j = 0; j < p.N; j++) {
//            System.out.println(r[j] + "\t" + g.sigma_res_theta[j] + "\t" + g.sigma_res_r[j] + "\t" + g.sigma_res_z[j] + "\t" + g.q_theta[j] + "\t" + g.q_r[j] + "\t" + g.q_z[j]);
//        }
    }

    private void calculateDeltas(Params params, Graph graph) {
        if (params.delta_theta_exp != 0) {
            double delta_theta = 0;
            for (int i = 0; i < params.r_theta_exp.length; i++) {
                double sigma_res_theta_exp = params.sigma_theta_exp[i];
                double sigma_res_theta_calc = graph.sigma_res_theta[params.r_theta_calc_index[i]];
                delta_theta += pow(sigma_res_theta_calc - sigma_res_theta_exp, 2);
            }
            graph.delta_theta = sqrt(delta_theta) / params.delta_theta_exp;
        }

        if (params.delta_z_exp != 0) {
            double delta_z = 0;
            for (int i = 0; i < params.r_z_exp.length; i++) {
                double sigma_res_z_exp = params.sigma_z_exp[i];
                double sigma_res_z_calc = graph.sigma_res_z[params.r_z_calc_index[i]];
                delta_z += pow(sigma_res_z_calc - sigma_res_z_exp, 2);
            }
            graph.delta_z = sqrt(delta_z) / params.delta_z_exp;
        }
        graph.delta = graph.delta_theta + graph.delta_z;
    }

    private double findB(Params params, double h0, double h_zv) {
        double b_root = -1.0;
        double delta_root = 100;
        double pi_div_2H = sqrt(PI) / (2 * params.H);
        for (double b : params.b_array) {
            double delta = exp(-pow((h0 - h_zv) / b, 2)) - b * pi_div_2H * (erf((params.H - h_zv) / b) + erf(h_zv / b));
            delta = abs(delta);
            if (delta < delta_root && delta < 0.01) {
                b_root = b;
                delta_root = delta;
            }
        }
        return b_root;
    }

}
