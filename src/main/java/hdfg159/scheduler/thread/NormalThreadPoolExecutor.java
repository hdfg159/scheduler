package hdfg159.scheduler.thread;

import hdfg159.scheduler.util.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 定时任务 组件:正常线程池
 *
 * @author hdfg159
 * @version 1.0
 */
public class NormalThreadPoolExecutor implements ThreadPool {
	private static final Logger log = LoggerFactory.getLogger(NormalThreadPoolExecutor.class);
	
	/**
	 * 任务运行线程池
	 */
	private static ExecutorService taskService;
	
	@Override
	public void initialize() {
		taskService = taskThreadPoolExecutor();
	}
	
	@Override
	public void shutdown() {
		taskService.shutdown();
	}
	
	@Override
	public ExecutorService threadPool() {
		return taskService;
	}
	
	/**
	 * 获取任务执行线程池
	 *
	 * @return ThreadPoolExecutor
	 */
	private ThreadPoolExecutor taskThreadPoolExecutor() {
		// 线程池线程命名
		final String poolNameFormat = "normal-task-%d";
		// 线程池队列拒绝策略
		final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
		// 核心线程数，队列未满，默认最多创建线程数量
		final int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
		// 最大线程数，队列满了，允许新增到最大线程数量
		final int maximumPoolSize = Runtime.getRuntime().availableProcessors() + 1;
		// 线程存活时间，当线程池数量超过核心线程数量以后，空闲时间(idle) 时间超过这个值的线程会被终止
		final int keepAliveTime = 60;
		// 线程池存活时间单位
		final TimeUnit timeUnit = TimeUnit.SECONDS;
		// 默认线程池最大队列是 INT 最大值（过大导致内存满），队列长度
		final int queueCapacity = Integer.MAX_VALUE;
		// 队列类型
		final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);
		// 自定义线程工厂：自定义名字/优先级/线程是否 Daemon （Daemon 是守护进程, JVM 执行完用户线程后退出，Daemon 线程也会退出，非 Daemon 线程的话会当作用户线程一直执行）
		final ThreadFactory threadFactory = new ThreadFactoryBuilder()
				.setNameFormat(poolNameFormat)
				.setDaemon(false)
				// 默认异常处理
				.setUncaughtExceptionHandler((t, e) -> log.error("thread run error:[{}]", t.getName(), e))
				.build();
		
		return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, workQueue, threadFactory, rejectedExecutionHandler) {
			@Override
			public void shutdown() {
				int queueSize = getQueue().size();
				int threadActiveCount = getActiveCount();
				log.info("shutdown task thread pool,working thread count:[{}],queue size:[{}],wait for exist tasks count:[{}]", threadActiveCount, queueSize, queueSize + threadActiveCount);
				super.shutdown();
				log.info("shutdown task thread pool finish");
			}
		};
	}
}
