package com.company.common.async;

import com.company.common.audit.AuditContext;
import com.company.common.audit.AuditContext.AuditContextHolder;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncTaskExecutor {

    @Bean(name = "tenantAwareTaskExecutor")
    public Executor tenantAwareTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("sqlforge-async-");
        executor.setTaskDecorator(tenantAwareTaskDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator tenantAwareTaskDecorator() {
        return new TaskDecorator() {
            @Override
            public Runnable decorate(Runnable runnable) {
                final AuditContextHolder snapshot = AuditContext.snapshot();
                return new Runnable() {
                    @Override
                    public void run() {
                        AuditContext.restore(snapshot);
                        try {
                            runnable.run();
                        } finally {
                            AuditContext.clear();
                        }
                    }
                };
            }
        };
    }
}
