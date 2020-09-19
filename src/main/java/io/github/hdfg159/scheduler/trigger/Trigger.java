package io.github.hdfg159.scheduler.trigger;

import io.github.hdfg159.scheduler.function.Consumer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Delayed;
import java.util.function.BiConsumer;

/**
 * 任务触发器接口
 *
 * @author hdfg159
 * @version 1.0
 */
public interface Trigger extends Serializable, Delayed, TriggerProperties {
	/**
	 * 获取任务
	 *
	 * @return {@code Consumer<Trigger>}
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
	 * 调度
	 *
	 * @return boolean true:放入调度队列成功,false:放入调度队列失败
	 */
	boolean schedule();

	/**
	 * 取消任务
	 * 与{@link Trigger#cancel(boolean)}不同，可以在取消后马上加入新的同名定时器
	 *
	 * @return boolean true:取消成功,false:取消失败
	 */
	boolean scheduleCancel();

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
	 * 获取 Trigger job执行 抛出异常后操作 消费
	 *
	 * @return {@code BiConsumer<Trigger, Throwable>}
	 */
	BiConsumer<Trigger, Throwable> getAfterExceptionCaught();

	/**
	 * Trigger job执行 抛出异常后操作 消费
	 *
	 * @param consumer
	 * 		操作消费
	 *
	 * @return Trigger
	 */
	Trigger afterExceptionCaught(BiConsumer<Trigger, Throwable> consumer);
}
