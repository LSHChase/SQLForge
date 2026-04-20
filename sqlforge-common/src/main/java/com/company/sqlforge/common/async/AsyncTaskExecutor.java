package com.company.sqlforge.common.async;

import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.audit.AuditEvent;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestContext.ContextValue;
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
                final ContextValue requestContext = RequestContext.snapshot();
                final AuditEvent auditEvent = AuditContext.snapshot();
                return new Runnable() {
                    @Override
                    public void run() {
                        RequestContext.restore(requestContext);
                        AuditContext.restore(auditEvent);
                        try {
                            runnable.run();
                        } finally {
                            AuditContext.clear();
                            RequestContext.clear();
                        }
                    }
                };
            }
        };
    }
}
