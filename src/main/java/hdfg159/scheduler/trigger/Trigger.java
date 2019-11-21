package hdfg159.scheduler.trigger;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 任务触发器接口
 *
 * @author hdfg159
 * @version 1.0
 */
public interface Trigger extends Serializable, Delayed {
	/**
	 * 获取任务 ID
	 *
	 * @return long
	 */
	long getId();
	
	/**
	 * 设置任务 ID
	 *
	 * @param id
	 * 		任务 ID
	 *
	 * @return Trigger
	 */
	Trigger id(long id);
	
	/**
	 * 获取创建时间
	 *
	 * @return LocalDateTime
	 */
	LocalDateTime getCreateTime();
	
	/**
	 * 获取任务名称
	 *
	 * @return String
	 */
	String getName();
	
	/**
	 * 设置任务名称
	 *
	 * @param name
	 * 		名称
	 *
	 * @return Trigger
	 */
	Trigger name(String name);
	
	/**
	 * 获取任务取消状态
	 *
	 * @return T
	 */
	boolean isCancel();
	
	/**
	 * 设置任务取消状态
	 *
	 * @param cancel
	 * 		状态
	 *
	 * @return Trigger
	 */
	Trigger cancel(boolean cancel);
	
	/**
	 * 获取任务
	 *
	 * @return Consumer<Trigger>
	 */
	Consumer<Trigger> getJob();
	
	/**
	 * 设置任务
	 *
	 * @param job
	 * 		任务
	 *
	 * @return Trigger
	 */
	Trigger job(Consumer<Trigger> job);
	
	/**
	 * 获取执行时间
	 *
	 * @return LocalDateTime
	 */
	LocalDateTime getExecuteTime();
	
	/**
	 * 设置执行时间
	 *
	 * @param executeTime
	 * 		执行时间
	 *
	 * @return Trigger
	 */
	Trigger executeTime(LocalDateTime executeTime);
	
	/**
	 * 自定义在原来基础上生成一个新的触发器
	 *
	 * @return {@code Optional<T>}
	 */
	Optional<Trigger> nextTrigger();
	
	/**
	 * 调度动作
	 *
	 * @return boolean true:放入调度队列成功,false:放入调度队列失败
	 */
	boolean schedule();
	
	/**
	 * 抛出异常后 默认处理方法
	 *
	 * @param cause
	 * 		异常
	 */
	default void exceptionCaught(Throwable cause) {
		BiConsumer<Trigger, Throwable> consumer = getAfterExceptionCaught();
		if (consumer == null) {
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw new RuntimeException(cause);
		}
		
		consumer.accept(this, cause);
	}
	
	/**
	 * 获取 抛出异常后操作 消费
	 *
	 * @return {@code BiConsumer<Trigger, Throwable>}
	 */
	BiConsumer<Trigger, Throwable> getAfterExceptionCaught();
	
	/**
	 * 抛出异常后操作 消费
	 *
	 * @param consumer
	 * 		操作消费
	 *
	 * @return Trigger
	 */
	Trigger afterExceptionCaught(BiConsumer<Trigger, Throwable> consumer);
}
