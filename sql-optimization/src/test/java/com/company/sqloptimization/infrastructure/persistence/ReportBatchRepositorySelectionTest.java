package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchItemRepository;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchRepository;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchStatisticsRepository;
import com.company.sqloptimization.infrastructure.persistence.mapper.ReportBatchItemMapper;
import com.company.sqloptimization.infrastructure.persistence.mapper.ReportBatchMapper;
import com.company.sqloptimization.infrastructure.persistence.mapper.ReportBatchStatisticsMapper;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryReportBatchStatisticsRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReportBatchRepositorySelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(ReportBatchMapper.class, () -> mock(ReportBatchMapper.class))
        .withBean(ReportBatchItemMapper.class, () -> mock(ReportBatchItemMapper.class))
        .withBean(ReportBatchStatisticsMapper.class, () -> mock(ReportBatchStatisticsMapper.class))
        .withUserConfiguration(
            MybatisReportBatchRepository.class,
            MybatisReportBatchItemRepository.class,
            MybatisReportBatchStatisticsRepository.class,
            InMemoryReportBatchRepository.class,
            InMemoryReportBatchItemRepository.class,
            InMemoryReportBatchStatisticsRepository.class
        );

    @Test
    void shouldSelectMybatisReportBatchRepositoriesWhenConfiguredForDatabase() {
        contextRunner
            .withPropertyValues("sql-optimization.report-batch.repository=database")
            .run(context -> {
                assertTrue(context.containsBean("mybatisReportBatchRepository"));
                assertTrue(context.containsBean("mybatisReportBatchItemRepository"));
                assertTrue(context.containsBean("mybatisReportBatchStatisticsRepository"));
                assertFalse(context.containsBean("inMemoryReportBatchRepository"));
                assertFalse(context.containsBean("inMemoryReportBatchItemRepository"));
                assertFalse(context.containsBean("inMemoryReportBatchStatisticsRepository"));

                Map<String, ReportBatchRepository> batchRepositories =
                    context.getBeansOfType(ReportBatchRepository.class);
                Map<String, ReportBatchItemRepository> itemRepositories =
                    context.getBeansOfType(ReportBatchItemRepository.class);
                Map<String, ReportBatchStatisticsRepository> statisticsRepositories =
                    context.getBeansOfType(ReportBatchStatisticsRepository.class);

                assertTrue(batchRepositories.get("mybatisReportBatchRepository")
                    instanceof MybatisReportBatchRepository);
                assertTrue(itemRepositories.get("mybatisReportBatchItemRepository")
                    instanceof MybatisReportBatchItemRepository);
                assertTrue(statisticsRepositories.get("mybatisReportBatchStatisticsRepository")
                    instanceof MybatisReportBatchStatisticsRepository);
            });
    }

    @Test
    void shouldSelectInMemoryReportBatchRepositoriesOnlyForTestRepositoryMode() {
        contextRunner
            .withPropertyValues("sql-optimization.report-batch.repository=test")
            .run(context -> {
                assertFalse(context.containsBean("mybatisReportBatchRepository"));
                assertFalse(context.containsBean("mybatisReportBatchItemRepository"));
                assertFalse(context.containsBean("mybatisReportBatchStatisticsRepository"));
                assertTrue(context.containsBean("inMemoryReportBatchRepository"));
                assertTrue(context.containsBean("inMemoryReportBatchItemRepository"));
                assertTrue(context.containsBean("inMemoryReportBatchStatisticsRepository"));

                Map<String, ReportBatchRepository> batchRepositories =
                    context.getBeansOfType(ReportBatchRepository.class);
                Map<String, ReportBatchItemRepository> itemRepositories =
                    context.getBeansOfType(ReportBatchItemRepository.class);
                Map<String, ReportBatchStatisticsRepository> statisticsRepositories =
                    context.getBeansOfType(ReportBatchStatisticsRepository.class);

                assertTrue(batchRepositories.get("inMemoryReportBatchRepository")
                    instanceof InMemoryReportBatchRepository);
                assertTrue(itemRepositories.get("inMemoryReportBatchItemRepository")
                    instanceof InMemoryReportBatchItemRepository);
                assertTrue(statisticsRepositories.get("inMemoryReportBatchStatisticsRepository")
                    instanceof InMemoryReportBatchStatisticsRepository);
            });
    }
}
