package com.company.sqloptimization.application.service;

final class PrejoinColumnMapping {

    final String sourceColumn;
    final String outputColumn;
    final String qualifier;
    final boolean ambiguousName;

    PrejoinColumnMapping(String sourceColumn, String outputColumn, String qualifier, boolean ambiguousName) {
        this.sourceColumn = sourceColumn;
        this.outputColumn = outputColumn;
        this.qualifier = qualifier;
        this.ambiguousName = ambiguousName;
    }
}
