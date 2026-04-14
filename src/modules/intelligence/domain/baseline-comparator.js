export class BaselineComparator {
  compare(previousBaseline, currentSummary) {
    if (!previousBaseline?.summary) {
      return {
        hasBaseline: false,
        driftRatio: 0,
        status: 'new-baseline',
        message: '暂无历史基线，当前结果将作为新基线'
      };
    }

    const previousP99 = Number(previousBaseline.summary.p99Ms ?? 0);
    const currentP99 = Number(currentSummary.p99Ms ?? 0);
    const driftRatio = previousP99 === 0 ? 0 : (currentP99 - previousP99) / previousP99;

    return {
      hasBaseline: true,
      previousP99Ms: previousP99,
      currentP99Ms: currentP99,
      driftRatio: Number(driftRatio.toFixed(4)),
      status: driftRatio > 0.2 ? 'regressed' : driftRatio < -0.1 ? 'improved' : 'stable',
      message:
        driftRatio > 0.2
          ? '当前 P99 相比历史基线明显劣化'
          : driftRatio < -0.1
            ? '当前 P99 相比历史基线明显改善'
            : '当前结果与历史基线基本一致'
    };
  }
}
