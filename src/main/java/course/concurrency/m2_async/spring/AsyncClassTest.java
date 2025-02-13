package course.concurrency.m2_async.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.beans.Transient;
import java.util.concurrent.Executor;

@Component
public class AsyncClassTest {

    @Autowired
    @Lazy
    private AsyncClassTest test;

    @Autowired
    public ApplicationContext context;

    @Autowired
    /*@Qualifier("applicationTaskExecutor")*/
    @Qualifier("taskExecutor")
    private Executor executor;

   /* @Autowired
    *//*@Qualifier("applicationTaskExecutor")*//*
    @Qualifier("secondTaskExecutor")
    private Executor second_executor;*/

    @Async("taskExecutor")
    public void runAsyncTask() {
        System.out.println("runAsyncTask: " + Thread.currentThread().getName());
        test.internalTask();
    }

    @Async("taskExecutor")
    public void internalTask() {
        System.out.println("internalTask: " + Thread.currentThread().getName());
    }
}
