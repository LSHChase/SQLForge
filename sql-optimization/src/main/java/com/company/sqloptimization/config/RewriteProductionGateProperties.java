package com.company.sqloptimization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sql-optimization.rewrite-production")
public class RewriteProductionGateProperties {

    private boolean developmentDirectActivationEnabled = false;
    private final Adapter calciteRelNode = new Adapter();
    private final Adapter relToSql = new Adapter();
    private final Adapter calciteMetadata = new Adapter();
    private final SmtSolver smtZ3 = new SmtSolver();
    private final Adapter hetuExplainCost = new Adapter();
    private final StatisticsCost statisticsCost = new StatisticsCost();

    public boolean isDevelopmentDirectActivationEnabled() {
        return developmentDirectActivationEnabled;
    }

    public void setDevelopmentDirectActivationEnabled(boolean developmentDirectActivationEnabled) {
        this.developmentDirectActivationEnabled = developmentDirectActivationEnabled;
    }

    public Adapter getCalciteRelNode() {
        return calciteRelNode;
    }

    public Adapter getRelToSql() {
        return relToSql;
    }

    public Adapter getCalciteMetadata() {
        return calciteMetadata;
    }

    public SmtSolver getSmtZ3() {
        return smtZ3;
    }

    public Adapter getHetuExplainCost() {
        return hetuExplainCost;
    }

    public StatisticsCost getStatisticsCost() {
        return statisticsCost;
    }

    public static class Adapter {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class SmtSolver extends Adapter {

        private String command = "";

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }

    public static class StatisticsCost extends Adapter {

        private String statisticsSource = "";

        public String getStatisticsSource() {
            return statisticsSource;
        }

        public void setStatisticsSource(String statisticsSource) {
            this.statisticsSource = statisticsSource;
        }
    }
}
