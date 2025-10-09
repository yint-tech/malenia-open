package cn.iinti.malenia2.service.base.env;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
public class ShutdownHook {
    private static final Set<Runnable> shutdownHooks = Collections.synchronizedSet(new LinkedHashSet<>());
    private static volatile boolean shutdownHookRunning = false;

    public static void registerShutdownHook(Runnable runnable) {
        if (shutdownHookRunning) {
            throw new IllegalStateException("ShutdownHook is already running");
        }
        HookWrapper hookWrapper = new HookWrapper(runnable);
        shutdownHooks.add(hookWrapper);
        Runtime.getRuntime().addShutdownHook(new Thread(hookWrapper));
    }

    private static class HookWrapper implements Runnable {
        private boolean called = false;
        private final Runnable delegate;

        public HookWrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            synchronized (this) {
                if (called) {
                    return;
                }
                called = true;
            }
            delegate.run();
        }
    }

    public static int prepareShutdown() {
        int nowTaskSize = shutdownHooks.size();
        if (shutdownHookRunning || nowTaskSize == 0) {
            return nowTaskSize;
        }
        synchronized (Environment.class) {
            if (shutdownHookRunning) {
                return nowTaskSize;
            }
            shutdownHookRunning = true;
        }
        new Thread(() -> {
            Iterator<Runnable> it = shutdownHooks.iterator();
            while (it.hasNext()) {
                Runnable next = it.next();
                try {
                    next.run();
                } catch (Throwable throwable) {
                    log.error("running shutdown hook failed", throwable);
                } finally {
                    it.remove();
                }
            }
        }).start();
        return shutdownHooks.size();
    }
}
