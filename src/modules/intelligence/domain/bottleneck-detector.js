export class BottleneckDetector {
  detect(resourceBreakdown = {}) {
    const scan = Number(resourceBreakdown.scanRatio ?? 0);
    const shuffle = Number(resourceBreakdown.shuffleRatio ?? 0);
    const compute = Number(resourceBreakdown.computeRatio ?? 0);
    const network = Number(resourceBreakdown.networkRatio ?? 0);
    const waiting = Number(resourceBreakdown.waitingRatio ?? 0);

    if (scan > 0.6) {
      return {
        type: 'io-bottleneck',
        reason: 'Scan 占比过高，优先检查分区裁剪、列裁剪和小文件问题',
        suggestions: ['优化分区键', '合并小文件', '开启列式索引或数据跳过能力']
      };
    }

    if (shuffle > 0.3) {
      return {
        type: 'distribution-bottleneck',
        reason: 'Shuffle 占比过高，优先检查 Join Key 倾斜和分桶策略',
        suggestions: ['增加分桶', '调整 Join Distribution Type', '引入热点 Key 专项优化']
      };
    }

    if (compute > 0.5) {
      return {
        type: 'cpu-bottleneck',
        reason: 'Compute 占比过高，优先检查复杂表达式、窗口函数和排序',
        suggestions: ['预计算', '物化视图', '移除非必要排序']
      };
    }

    if (network > 0.2) {
      return {
        type: 'network-bottleneck',
        reason: '网络传输占比较高，需控制结果集规模并检查跨区域访问',
        suggestions: ['分页查询', '聚合下推', '结果压缩']
      };
    }

    if (waiting > 0.2) {
      return {
        type: 'scheduling-bottleneck',
        reason: '等待时间偏高，说明已接近队列或资源池上限',
        suggestions: ['扩容 Worker', '优化并行度', '调整租户队列策略']
      };
    }

    return {
      type: 'balanced',
      reason: '未检测到单一主导瓶颈',
      suggestions: ['继续扩大并发压测范围', '观察计划稳定性']
    };
  }
}
