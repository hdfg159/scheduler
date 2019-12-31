package io.github.hdfg159.scheduler.thread;

import java.util.concurrent.ExecutorService;

/**
 * 定时任务组件 线程池 接口
 *
 * @author hdfg159
 * @version 1.0
 */
public interface ThreadPool {
	/**
	 * 初始化
	 */
	void initialize();
	
	/**
	 * 关闭
	 */
	void shutdown();
	
	/**
	 * 获取当前线程池
	 *
	 * @return ExecutorService
	 */
	ExecutorService threadPool();
}
