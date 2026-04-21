package com.company.benchmarkengine.application.controller.vo;

import java.util.List;

public class BenchmarkTrendSeriesVO {

    private final String seriesName;
    private final List<BenchmarkTrendPointVO> points;

    public BenchmarkTrendSeriesVO(String seriesName, List<BenchmarkTrendPointVO> points) {
        this.seriesName = seriesName;
        this.points = points;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public List<BenchmarkTrendPointVO> getPoints() {
        return points;
    }
}
