package hdfg159.scheduler;

import hdfg159.scheduler.trigger.Trigger;
import hdfg159.scheduler.util.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 定时调度 组件
 *
 * @author hdfg159
 * @version 1.0
 */
public enum SchedulerManager {
	/**
	 * 实例
	 */
	INSTANCE;
	/**
	 * 是否运行定时任务调度
	 */
	private static boolean isWork = true;
	
	private final Logger log = LoggerFactory.getLogger(SchedulerManager.class);
	/**
	 * 取队列任务线程名称
	 */
	private final String THREAD_NAME_SCHEDULER_TAKE_TASK = "scheduler-take-task";
	/**
	 * 延迟任务队列
	 */
	private final DelayQueue<Trigger> taskQueue = new DelayQueue<>();
	/**
	 * 正在等待运行的任务
	 */
	private final Map<String, Trigger> waitingJob = new ConcurrentHashMap<>();
	/**
	 * 任务运行线程池
	 */
	private final ExecutorService taskService;
	/**
	 * 取任务线程
	 */
	private final Thread takeTaskThread;
	
	/**
	 * 中断取任务线程监听
	 */
	private Consumer<DelayQueue<Trigger>> takeQueueInterruptListener;
	
	/**
	 * 默认构造器
	 */
	SchedulerManager() {
		taskService = taskThreadPoolExecutor();
		
		takeTaskThread = new Thread(new TakeQueueTask());
		takeTaskThread.setName(THREAD_NAME_SCHEDULER_TAKE_TASK);
		// 设置为非守护进程
		takeTaskThread.setDaemon(false);
		takeTaskThread.start();
	}
	
	/**
	 * 获取任务执行线程池
	 *
	 * @return ThreadPoolExecutor
	 */
	private ThreadPoolExecutor taskThreadPoolExecutor() {
		// 线程池线程命名
		final String poolNameFormat = "task-executor-%d";
		// 线程池队列拒绝策略
		final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
		// 核心线程数，队列未满，默认最多创建线程数量
		final int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
		// 最大线程数，队列满了，允许新增到最大线程数量
		final int maximumPoolSize = Runtime.getRuntime().availableProcessors() + 1;
		// 线程存活时间，当线程池数量超过核心线程数量以后，空闲时间(idle) 时间超过这个值的线程会被终止
		final int keepAliveTime = 120;
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
	
	public Consumer<DelayQueue<Trigger>> getTakeQueueInterruptListener() {
		return takeQueueInterruptListener;
	}
	
	/**
	 * 设置中断取任务线程监听
	 *
	 * @param takeQueueInterruptListener
	 * 		监听消费
	 *
	 * @return SchedulerManager
	 */
	public SchedulerManager setTakeQueueInterruptListener(Consumer<DelayQueue<Trigger>> takeQueueInterruptListener) {
		this.takeQueueInterruptListener = takeQueueInterruptListener;
		return this;
	}
	
	/**
	 * 是否正在运行
	 *
	 * @return boolean
	 */
	public boolean isWork() {
		return isWork;
	}
	
	/**
	 * 设置执行状态
	 *
	 * @param work
	 * 		true:执行,false:不执行
	 */
	public void setWork(boolean work) {
		isWork = work;
	}
	
	/**
	 * 获取延迟任务队列
	 *
	 * @return {@code DelayQueue<Trigger>}
	 */
	public DelayQueue<Trigger> getTaskQueue() {
		return taskQueue;
	}
	
	/**
	 * 获取等待调度运行的触发器
	 *
	 * @return {@code Map<String, Trigger>}
	 */
	public Map<String, Trigger> getWaitingJob() {
		return waitingJob;
	}
	
	/**
	 * 关闭任务调度
	 */
	public void shutdown() {
		takeTaskThread.interrupt();
		
		taskService.shutdown();
	}
	
	/**
	 * 加入调度任务
	 *
	 * @param trigger
	 * 		触发器
	 *
	 * @return boolean
	 */
	public boolean schedule(Trigger trigger) {
		if (!isWork) {
			return false;
		}
		
		if (trigger == null) {
			throw new IllegalArgumentException("trigger not allow null");
		}
		
		String triggerName = trigger.getName();
		if (triggerName == null) {
			throw new IllegalArgumentException("trigger must have a name");
		}
		
		Trigger putVal = waitingJob.put(triggerName, trigger);
		if (putVal != null) {
			throw new RuntimeException("exist trigger name:[" + triggerName + "]");
		}
		
		boolean isAddSuccess = taskQueue.add(trigger);
		if (!isAddSuccess) {
			waitingJob.remove(triggerName);
			return false;
		}
		
		log.info("schedule trigger:[{}][{}],execute time:[{}]", triggerName, trigger.getId(), trigger.getExecuteTime());
		return true;
	}
	
	/**
	 * 取消等待执行的任务调度
	 *
	 * @param triggerName
	 * 		名称
	 *
	 * @return boolean
	 */
	public boolean cancel(String triggerName) {
		Optional<Trigger> triggerOptional = getWaitingJob(triggerName);
		if (!triggerOptional.isPresent()) {
			return false;
		}
		
		Trigger trigger = triggerOptional.get();
		trigger.cancel(true);
		
		waitingJob.remove(triggerName);
		return true;
	}
	
	/**
	 * 获取等待调度运行的触发器
	 *
	 * @param triggerName
	 * 		触发器名称
	 *
	 * @return {@code Optional<Trigger>}
	 */
	public Optional<Trigger> getWaitingJob(String triggerName) {
		return Optional.ofNullable(waitingJob.get(triggerName));
	}
	
	/**
	 * 取延迟队列任务
	 */
	private class TakeQueueTask implements Runnable {
		@Override
		public void run() {
			while (SchedulerManager.INSTANCE.isWork()) {
				try {
					Trigger trigger = taskQueue.take();
					
					String triggerName = trigger.getName();
					waitingJob.remove(triggerName);
					
					trigger.nextTrigger().ifPresent(t -> {
						log.debug("next trigger effect:[{}]", t.getName());
						schedule(trigger);
					});
					
					taskService.execute(new TaskRunner(trigger));
				} catch (InterruptedException e) {
					log.error("take queue task thread interrupted,task termination,queue size:[{}]", taskQueue.size());
					// 恢复中断状态
					Thread.currentThread().interrupt();
					interruptListener();
					break;
				} catch (Exception e) {
					log.error("take queue error", e);
				}
			}
		}
		
		private void interruptListener() {
			log.info("execute take queue interrupt listener");
			try {
				if (takeQueueInterruptListener != null) {
					// 中断时候监听操作
					takeQueueInterruptListener.accept(taskQueue);
				}
			} catch (Exception e) {
				log.error("take queue interrupt listener error", e);
			}
		}
	}
}
