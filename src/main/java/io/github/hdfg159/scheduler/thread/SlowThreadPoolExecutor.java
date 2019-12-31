package io.github.hdfg159.scheduler.thread;

import io.github.hdfg159.scheduler.util.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 慢线程池 实现
 *
 * @author hdfg159
 * @version 1.0
 */
public class SlowThreadPoolExecutor implements ThreadPool {
	private static final Logger log = LoggerFactory.getLogger(SlowThreadPoolExecutor.class);
	/**
	 * 慢任务运行线程池
	 */
	private static ExecutorService slowTaskService;
	
	@Override
	public void initialize() {
		slowTaskService = slowTaskThreadPoolExecutor();
	}
	
	@Override
	public void shutdown() {
		slowTaskService.shutdown();
	}
	
	@Override
	public ExecutorService threadPool() {
		return slowTaskService;
	}
	
	/**
	 * 慢任务执行线程池
	 *
	 * @return ExecutorService
	 */
	private ExecutorService slowTaskThreadPoolExecutor() {
		final String poolNameFormat = "slow-task-%d";
		final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
		final int corePoolSize = Runtime.getRuntime().availableProcessors() * 5;
		final int maximumPoolSize = Runtime.getRuntime().availableProcessors() * 5;
		final int keepAliveTime = 60;
		final TimeUnit timeUnit = TimeUnit.SECONDS;
		final int queueCapacity = Integer.MAX_VALUE;
		final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat(poolNameFormat)
				.setDaemon(false)
				.setUncaughtExceptionHandler((t, e) -> log.error("thread run error:[{}]", t.getName(), e))
				.build();
		
		return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, workQueue, threadFactory, rejectedExecutionHandler) {
			@Override
			public void shutdown() {
				int queueSize = getQueue().size();
				int threadActiveCount = getActiveCount();
				log.info("shutdown slow task thread pool,working thread count:[{}],queue size:[{}],wait for exist tasks count:[{}]", threadActiveCount, queueSize, queueSize + threadActiveCount);
				super.shutdown();
				log.info("shutdown slow task thread pool finish");
			}
		};
	}
}
