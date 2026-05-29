package com.company.sqlforge.common.utils;

final class RewriteCandidate {
    final String rewrittenText;
    final int nextIndex;

    RewriteCandidate(String rewrittenText, int nextIndex) {
        this.rewrittenText = rewrittenText;
        this.nextIndex = nextIndex;
    }
}
