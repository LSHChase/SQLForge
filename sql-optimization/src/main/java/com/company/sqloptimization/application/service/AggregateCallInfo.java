package com.company.sqloptimization.application.service;

final class AggregateCallInfo {

    final String functionName;
    final String argument;
    final String expression;
    final boolean distinct;

    AggregateCallInfo(String functionName, String argument, String expression, boolean distinct) {
        this.functionName = functionName;
        this.argument = argument;
        this.expression = expression;
        this.distinct = distinct;
    }
}
