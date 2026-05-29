package com.company.sqloptimization.application.service;

final class PrejoinToken {

    final String value;
    final int endIndex;

    PrejoinToken(String value, int endIndex) {
        this.value = value;
        this.endIndex = endIndex;
    }
}
