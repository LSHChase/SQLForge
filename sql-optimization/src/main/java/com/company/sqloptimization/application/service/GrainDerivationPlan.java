package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GrainDerivationPlan {

    final List<String> grain;
    final boolean hasTimeRollup;

    GrainDerivationPlan(List<String> grain, boolean hasTimeRollup) {
        this.grain = Collections.unmodifiableList(new ArrayList<String>(grain));
        this.hasTimeRollup = hasTimeRollup;
    }
}
