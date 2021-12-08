document.addEventListener('DOMContentLoaded', function () {

    Highcharts.setOptions({
        lang: {
            resetZoom: 'Сбросить масштаб'
        }
    });

    Vue.prototype.$http = axios;
    new Vue({
        el: '#app',
        data: {
            isFormLoaded: false,
            percent: 0,
            sigma_zv: null,
            h0: null,
            h_zv: null,
            sigma0: null,
            sigma1: null,
            b: null,
            alpha: null,
            delta_theta: null,
            delta_z: null,
            delta: null,
            charts: {
                sigma_res_theta: {
                    title: '&sigma;<sub>&theta;</sub><sup>res</sup> = &sigma;<sub>&theta;</sub><sup>res</sup>(<i>r</i> )',
                    chart: null
                },
                sigma_res_r: {
                    title: '&sigma;<sub>r</sub><sup>res</sup> = &sigma;<sub>r</sub><sup>res</sup>(<i>r</i> )',
                    chart: null
                },
                sigma_res_z: {
                    title: '&sigma;<sub>z</sub><sup>res</sup> = &sigma;<sub>z</sub><sup>res</sup>(<i>r</i> )',
                    chart: null
                },
                q_theta: {
                    title: 'q<sub>&theta;</sub> = q<sub>&theta;</sub>(<i>r</i> )',
                    chart: null
                },
                q_r: {
                    title: 'q<sub>r</sub> = q<sub>r</sub>(<i>r</i> )',
                    chart: null
                },
                q_z: {
                    title: 'q<sub>z</sub> = q<sub>z</sub>(<i>r</i> )',
                    chart: null
                }
            },
            E: null,
            mu: null,
            R1: null,
            R2: null,
            dr: null,
            text_r_theta_exp: '',
            text_sigma_theta_exp: '',
            text_r_z_exp: '',
            text_sigma_z_exp: '',
            sigma_zv_start: null,
            sigma_zv_end: null,
            sigma_zv_step: null,
            h_zv_start: null,
            h_zv_end: null,
            h_zv_step: null,
            h0_start: null,
            h0_end: null,
            h0_step: null,
            b_start: null,
            b_end: null,
            b_step: null,
            alpha_start: null,
            alpha_end: null,
            alpha_step: null
        },
        computed: {
            totalIterations: function () {
                return Math.round((this.h_zv_end - this.h_zv_start) / this.h_zv_step) *
                    Math.round((this.sigma_zv_end - this.sigma_zv_start) / this.sigma_zv_step) *
                    Math.round((this.h0_end - this.h0_start) / this.h0_step) *
                    Math.round((this.alpha_end - this.alpha_start) / this.alpha_step);
            }
        },
        mounted: function () {
            this.createChart('sigma_res_theta');
            this.createChart('sigma_res_r');
            this.createChart('sigma_res_z');
            this.createChart('q_theta');
            this.createChart('q_r');
            this.createChart('q_z');
            setTimeout(() => this.loadGraph(), 500);
        },
        methods: {
            postParameters: function () {
                this.$http.post('/run', {
                    E: this.E,
                    mu: this.mu,
                    R1: this.R1,
                    R2: this.R2,
                    dr: this.dr,
                    h_zv_start: this.h_zv_start,
                    h_zv_end: this.h_zv_end,
                    h_zv_step: this.h_zv_step,
                    sigma_zv_start: this.sigma_zv_start,
                    sigma_zv_end: this.sigma_zv_end,
                    sigma_zv_step: this.sigma_zv_step,
                    h0_start: this.h0_start,
                    h0_end: this.h0_end,
                    r_theta_exp: this.str2array(this.text_r_theta_exp),
                    sigma_theta_exp: this.str2array(this.text_sigma_theta_exp),
                    r_z_exp: this.str2array(this.text_r_z_exp),
                    sigma_z_exp: this.str2array(this.text_sigma_z_exp),
                    h0_step: this.h0_step,
                    b_start: this.b_start,
                    b_end: this.b_end,
                    b_step: this.b_step,
                    alpha_start: this.alpha_start,
                    alpha_end: this.alpha_end,
                    alpha_step: this.alpha_step
                });
            },
            cancel: function () {
                this.$http.post('/cancel');
            },
            createChart: function (name) {
                var series = [{
                    name: name,
                    data: [],
                    boostThreshold: 1
                }];
                if (name === "sigma_res_theta" || name === "sigma_res_z") {
                    series.push({
                        name: "Эксперимент",
                        data: [],
                        lineWidth: 0,
                        marker: {
                            enabled: true
                        }
                    });
                }

                this.charts[name].chart = Highcharts.chart(name, {
                    chart: {
                        type: 'spline',
                        plotBorderWidth: 2,
                        zoomType: 'xy',
                        panning: {
                            enabled: true,
                            type: 'xy'
                        },
                        panKey: 'shift'
                    },
                    title: {
                        text: this.charts[name].title,
                        useHTML: true
                    },
                    tooltip: {
                        enabled: true,
                        useHTML: true,
                        followPointer: true,
                        valueDecimals: 7,
                        headerFormat: '<span><i>r</i> = {point.key}</span><br/>',
                        pointFormat: '<span><b>{point.y}</b></span>',
                        positioner: function () {
                            return {
                                x: this.chart.plotLeft + 15,
                                y: this.chart.plotHeight - 15
                            };
                        }
                    },
                    xAxis: {
                        title: {
                            text: 'r',
                            style: {
                                color: "white"
                            }
                        },
                        tickWidth: 0,
                        gridLineWidth: 1
                    },
                    yAxis: {
                        title: {
                            enabled: false
                        }
                    },
                    credits: {
                        enabled: false
                    },
                    legend: {
                        enabled: false
                    },
                    boost: {
                        useGPUTranslations: true,
                        usePreallocated: true
                    },
                    series: series,
                    exporting: {
                        allowHTML: true,
                        filename: name,
                        tableCaption: false,
                        buttons: {
                            contextButton: {
                                menuItems: ["viewFullscreen", "printChart", "separator", "downloadPNG", "downloadSVG", "downloadPDF", 'downloadCSV']
                            }
                        }
                    }
                });
            },
            loadGraph: function () {
                this.$http
                    .get('/graph')
                    .then(response => {
                        var params = response.data.params;
                        var graph = response.data.graph;
                        this.percent = response.data.percent;

                        if (graph.delta === this.delta) {
                            return;
                        }

                        if (!this.isFormLoaded) {
                            this.isFormLoaded = true;
                            this.E = params.E;
                            this.mu = params.mu;
                            this.R1 = params.R1;
                            this.R2 = params.R2;
                            this.dr = params.dr;
                            this.text_r_theta_exp = params.r_theta_exp.join('\n');
                            this.text_sigma_theta_exp = params.sigma_theta_exp.join('\n');
                            this.text_r_z_exp = params.r_z_exp.join('\n');
                            this.text_sigma_z_exp = params.sigma_z_exp.join('\n');
                            this.sigma_zv_start = params.sigma_zv_start;
                            this.sigma_zv_end = params.sigma_zv_end;
                            this.sigma_zv_step = params.sigma_zv_step;
                            this.h_zv_start = params.h_zv_start;
                            this.h_zv_end = params.h_zv_end;
                            this.h_zv_step = params.h_zv_step;
                            this.h0_start = params.h0_start;
                            this.h0_end = params.h0_end;
                            this.h0_step = params.h0_step;
                            this.b_start = params.b_start;
                            this.b_end = params.b_end;
                            this.b_step = params.b_step;
                            this.alpha_start = params.alpha_start;
                            this.alpha_end = params.alpha_end;
                            this.alpha_step = params.alpha_step;
                        }

                        this.sigma_zv = graph.sigma_zv;
                        this.h0 = graph.h0;
                        this.h_zv = graph.h_zv;
                        this.sigma0 = graph.sigma0;
                        this.sigma1 = graph.sigma1;
                        this.b = graph.b;
                        this.alpha = graph.alpha;
                        this.delta_theta = graph.delta_theta > 0 ? graph.delta_theta : null;
                        this.delta_z = graph.delta_z > 0 ? graph.delta_z : null;
                        this.delta = graph.delta;

                        this.updateChart(this.charts.sigma_res_theta.chart, params.r, graph.sigma_res_theta, params.r_theta_exp, params.sigma_theta_exp);
                        this.updateChart(this.charts.sigma_res_r.chart, params.r, graph.sigma_res_r, null, null);
                        this.updateChart(this.charts.sigma_res_z.chart, params.r, graph.sigma_res_z, params.r_z_exp, params.sigma_z_exp);
                        this.updateChart(this.charts.q_theta.chart, params.r, graph.q_theta, null, null);
                        this.updateChart(this.charts.q_r.chart, params.r, graph.q_r, null, null);
                        this.updateChart(this.charts.q_z.chart, params.r, graph.q_z, null, null);
                    })
                    .catch(error => {
                        this.percent = 0;
                        if (error.response && error.response.status === 404 && !this.isFormLoaded) {
                            this.isFormLoaded = true;
                            this.E = 200000.0;
                            this.mu = 0.3;
                            this.R1 = 0;
                            this.R2 = 12.5;
                            this.dr = 0.001;
                            this.text_r_theta_exp = [11.426, 11.468, 11.558, 11.691, 11.804, 11.891, 11.964, 12.045, 12.114, 12.157, 12.200, 12.229, 12.253, 12.283, 12.313, 12.357, 12.398, 12.449, 12.5].join('\n');
                            this.text_sigma_theta_exp = [19.64, 0, -48.08, -110.38, -167.27, -207.9, -249.21, -294.58, -333.86, -356.21, -371.78, -373.81, -370.43, -362.3, -348.76, -322.35, -289.16, -245.15, -200.45].join('\n');
                            this.text_r_z_exp = [11.439, 11.462, 11.541, 11.664, 11.845, 12.089, 12.199, 12.293, 12.334, 12.369, 12.414, 12.5].join('\n');
                            this.text_sigma_z_exp = [17.63, 0, -53.56, -135.59, -269.83, -454.92, -539.66, -605.42, -612.88, -606.78, -585.76, -518.64].join('\n');
                            this.sigma_zv_start = -350;
                            this.sigma_zv_end = -340;
                            this.sigma_zv_step = 0.1;
                            this.h_zv_start = 0.20;
                            this.h_zv_end = 0.22;
                            this.h_zv_step = 0.001;
                            this.h0_start = 1.14;
                            this.h0_end = 1.16;
                            this.h0_step = 0.001;
                            this.b_start = 0.5;
                            this.b_end = 0.6;
                            this.b_step = 0.000001;
                            this.alpha_start = 3;
                            this.alpha_end = 4;
                            this.alpha_step = 0.1;
                            this.charts.sigma_res_theta.chart.showLoading('Расчёт ещё не запускался');
                            this.charts.sigma_res_r.chart.showLoading('Расчёт ещё не запускался');
                            this.charts.sigma_res_z.chart.showLoading('Расчёт ещё не запускался');
                            this.charts.q_theta.chart.showLoading('Расчёт ещё не запускался');
                            this.charts.q_r.chart.showLoading('Расчёт ещё не запускался');
                            this.charts.q_z.chart.showLoading('Расчёт ещё не запускался');
                        }
                    })
                    .finally(() => setTimeout(() => this.loadGraph(), 5000));
            },
            str2array: function (str) {
                if (str === '') {
                    return [];
                }
                return str.split('\n').map(x => parseFloat(x));
            },
            textareaFormatter: function (value) {
                return value.replace(/[^0-9.,\n-]/g, '');
            },
            updateChart: function (chart, r_calc, values_calc, r_exp, values_exp) {
                chart.series[0].setData(r_calc.map((r, i) => [r, values_calc[i]]), false, false);
                if (r_exp != null && values_exp != null) {
                    chart.series[1].setData(r_exp.map((r, i) => [r, values_exp[i]]), false, false);
                }
                chart.xAxis[0].update({min: r_calc[0], max: r_calc[r_calc.length - 1]}, false);
                chart.hideLoading();
                chart.redraw();
            }
        }
    });

});
