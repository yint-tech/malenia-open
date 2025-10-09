package cn.iinti.malenia2.service.proxy.core.mitm;

import cn.iinti.malenia2.BuildConfig;
import cn.iinti.malenia2.service.base.config.Configs;
import cn.iinti.malenia2.service.base.config.Settings;
import cn.iinti.malenia2.service.base.metric.monitor.Monitor;
import io.micrometer.core.instrument.Tags;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 不信赖扩展脚本会按照规则办事儿，所以扩展脚本的执行发生在单独线程池中。避免脚本线程影响Netty线程，
 * 当线程池满了之后，我们需要拒绝脚本的执行。<br/>
 * 考虑多用户相互影响的可能，我设计了两层线程池结构,所有任务在统一的公共线程池中执行，
 * 当某个账户业务量特别大影响公共线程池之后，任务提交将会失败，然后我们给每个账户单独开辟一个小型线程池
 */
public class MitmExecutor {

    private static final ThreadPoolExecutor threadPool = createThreadPool();
    private static final Map<String, ThreadPoolExecutor> childPool = new ConcurrentHashMap<>();

    static {
        Configs.addKeyMonitor(Settings.mitmThreadPoolSize.key, MitmExecutor::updateThreadPoolConfig);
    }

    /**
     * 专门给脚本提交任务使用，在任务满的时候可能提交失败
     *
     * @param user     脚本所属用户
     * @param runnable mitm对应的脚本代码
     * @return 是否提交成功
     */
    public static boolean submit(String user, Runnable runnable) {
        runnable = Monitor.timer("mitm.executor.run",
                Tags.of("user", user)
        ).wrap(runnable);
        boolean success = false;
        try {
            threadPool.submit(runnable);
            success = true;
        } catch (RejectedExecutionException ignore) {
        }
        if (success) {
            return true;
        }

        ThreadPoolExecutor childExecutor = createOrGetChildExecutor(user);
        try {
            childExecutor.submit(runnable);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }


    private static ThreadPoolExecutor createOrGetChildExecutor(String user) {
        ThreadPoolExecutor threadPoolExecutor = childPool.get(user);
        if (threadPoolExecutor != null) {
            return threadPoolExecutor;
        }

        synchronized (MitmExecutor.class) {
            threadPoolExecutor = childPool.get(user);
            if (threadPoolExecutor != null) {
                return threadPoolExecutor;
            }
            threadPoolExecutor = createChildExecutor(user);
            childPool.put(user, threadPoolExecutor);
        }
        return threadPoolExecutor;
    }

    private static ThreadPoolExecutor createChildExecutor(String user) {
        return new ThreadPoolExecutor(3, 3,
                1L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(1),
                new ThreadFactory() {
                    private final AtomicInteger sequence = new AtomicInteger(1);

                    @Override
                    public Thread newThread(@Nonnull Runnable r) {
                        Thread thread = new Thread(r);
                        int seq = sequence.getAndIncrement();
                        thread.setName("mitm-childExecotor-" + user + "-" + (seq > 1 ? "-" + seq : ""));
                        thread.setDaemon(true);
                        return thread;
                    }
                }, new ThreadPoolExecutor.AbortPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                Monitor.counter("mitm.childExecotor.reject", Tags.of("user", user)).increment();
                super.rejectedExecution(r, e);
            }
        });
    }


    private static void updateThreadPoolConfig() {
        int threadNumInt = Settings.mitmThreadPoolSize.value;

        int corePoolSize = threadPool.getCorePoolSize();
        if (corePoolSize < threadNumInt) {
            // 线程池扩容
            threadPool.setMaximumPoolSize(threadNumInt);
            threadPool.setCorePoolSize(threadNumInt);
        } else if (corePoolSize > threadNumInt) {
            threadPool.setCorePoolSize(threadNumInt);
            threadPool.setMaximumPoolSize(threadNumInt);
        }
    }

    private static ThreadPoolExecutor createThreadPool() {
        int threadNumInt = Settings.mitmThreadPoolSize.value;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadNumInt, threadNumInt,
                1L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(5),
                new ThreadFactory() {
                    private final AtomicInteger sequence = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        int seq = sequence.getAndIncrement();
                        thread.setName("mitm-execotor-" + (seq > 1 ? "-" + seq : ""));
                        thread.setDaemon(true);
                        return thread;
                    }
                }, new ThreadPoolExecutor.AbortPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                Monitor.counter(BuildConfig.appName + ".mitm.execotor.reject").increment();
                super.rejectedExecution(r, e);
            }
        });

        // 监控线程池
        Monitor.gauge(BuildConfig.appName + ".mitm.executor.queueSize", threadPoolExecutor, value -> value.getQueue().size());
        Monitor.gauge(BuildConfig.appName + ".mitm.executor.coreSize", threadPoolExecutor, ThreadPoolExecutor::getCorePoolSize);
        Monitor.gauge(BuildConfig.appName + ".mitm.executor.active", threadPoolExecutor, ThreadPoolExecutor::getActiveCount);
        return threadPoolExecutor;
    }
}
