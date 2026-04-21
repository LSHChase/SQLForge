package com.company.benchmarkengine.application.controller.vo;

import java.util.List;

public class BenchmarkTrendChartVO {

    private final String chartType;
    private final String title;
    private final String xAxisLabel;
    private final String yAxisLabel;
    private final List<BenchmarkTrendSeriesVO> series;

    public BenchmarkTrendChartVO(String chartType,
                                 String title,
                                 String xAxisLabel,
                                 String yAxisLabel,
                                 List<BenchmarkTrendSeriesVO> series) {
        this.chartType = chartType;
        this.title = title;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
        this.series = series;
    }

    public String getChartType() {
        return chartType;
    }

    public String getTitle() {
        return title;
    }

    public String getXAxisLabel() {
        return xAxisLabel;
    }

    public String getYAxisLabel() {
        return yAxisLabel;
    }

    public List<BenchmarkTrendSeriesVO> getSeries() {
        return series;
    }
}
