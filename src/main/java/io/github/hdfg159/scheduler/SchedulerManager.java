package io.github.hdfg159.scheduler;

import io.github.hdfg159.scheduler.thread.NormalThreadPoolExecutor;
import io.github.hdfg159.scheduler.thread.SlowThreadPoolExecutor;
import io.github.hdfg159.scheduler.thread.ThreadPool;
import io.github.hdfg159.scheduler.trigger.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
	 * 慢任务执行时间 阈值(毫秒)
	 */
	private static final long MAX_LIMIT_TIME = 100L;
	private static final String SCHEDULER_PROPERTIES = "scheduler.properties";
	private static final String CLASS_NORMAL_THREAD_POOL_EXECUTOR = "scheduler.threadPool.class";
	private static final String CLASS_SLOW_THREAD_POOL_EXECUTOR = "scheduler.slowThreadPool.class";
	private static final String PROPERTIES_SLOW_MAX_LIMIT_TIME = "scheduler.maxLimitTime";
	
	/**
	 * 取队列任务线程名称
	 */
	private static final String THREAD_NAME_SCHEDULER_TAKE_TASK = "scheduler-take-task";
	private static final Logger log = LoggerFactory.getLogger(SchedulerManager.class);
	/**
	 * 是否运行定时任务调度
	 */
	private static boolean isWork = true;
	/**
	 * 延迟任务队列
	 */
	private final DelayQueue<Trigger> taskQueue = new DelayQueue<>();
	/**
	 * 正在等待运行的任务
	 */
	private final Map<String, Trigger> waitingJob = new ConcurrentHashMap<>();
	/**
	 * 正常线程线程池实现
	 */
	private final ThreadPool taskExecutor;
	/**
	 * 慢任务线程池实现
	 */
	private final ThreadPool slowTaskExecutor;
	/**
	 * 取任务线程
	 */
	private final Thread takeTaskThread;
	/**
	 * 配置文件
	 */
	private Properties config;
	/**
	 * 中断取任务线程监听
	 */
	private Consumer<DelayQueue<Trigger>> takeQueueInterruptListener;
	
	/**
	 * 默认构造器
	 */
	SchedulerManager() {
		config = initProperties();
		String normalThreadPoolClassName = config.getProperty(CLASS_NORMAL_THREAD_POOL_EXECUTOR);
		taskExecutor = initTaskExecutor(normalThreadPoolClassName, () -> {
			NormalThreadPoolExecutor executor = new NormalThreadPoolExecutor();
			executor.initialize();
			return executor;
		});
		
		String slowThreadPoolClassName = config.getProperty(CLASS_SLOW_THREAD_POOL_EXECUTOR);
		slowTaskExecutor = initTaskExecutor(slowThreadPoolClassName, () -> {
			SlowThreadPoolExecutor executor = new SlowThreadPoolExecutor();
			executor.initialize();
			return executor;
		});
		
		takeTaskThread = new Thread(new TakeQueueTask());
		takeTaskThread.setName(THREAD_NAME_SCHEDULER_TAKE_TASK);
		// 设置为非守护进程
		takeTaskThread.setDaemon(false);
		takeTaskThread.start();
	}
	
	/**
	 * 初始化读取配置文件
	 *
	 * @return Properties
	 */
	private Properties initProperties() {
		Properties properties = new Properties();
		
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCHEDULER_PROPERTIES)) {
			if (inputStream != null) {
				properties.load(inputStream);
			}
		} catch (IOException e) {
			log.error("load properties error", e);
		}
		return properties;
	}
	
	private ThreadPool initTaskExecutor(String clazzName, Supplier<ThreadPool> threadPoolSupplier) {
		return Optional.ofNullable(clazzName)
				.map(className -> {
					try {
						Class<?> clazz = Class.forName(className);
						Object instance = clazz.getDeclaredConstructor().newInstance();
						if (instance instanceof ThreadPool) {
							ThreadPool threadPool = (ThreadPool) instance;
							threadPool.initialize();
							return threadPool;
						}
					} catch (Exception e) {
						log.error("init thread pool error,exception:[{}]", e.getClass().getName(), e);
					}
					return null;
				})
				.orElseGet(threadPoolSupplier);
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
		
		taskExecutor.shutdown();
		slowTaskExecutor.shutdown();
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
	 * 获取慢任务执行时间阈值
	 *
	 * @return long 毫秒
	 */
	private long getLimitTime() {
		return Optional.ofNullable(config.getProperty(PROPERTIES_SLOW_MAX_LIMIT_TIME))
				.map(Long::parseLong)
				.orElse(MAX_LIMIT_TIME);
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
					
					TaskRunner taskRunner = new TaskRunner(trigger);
					long limitTime = getLimitTime();
					if (trigger.getCostTime() > limitTime) {
						slowTaskExecutor.threadPool().execute(taskRunner);
					} else {
						taskExecutor.threadPool().execute(taskRunner);
					}
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
