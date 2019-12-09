package hdfg159.scheduler.trigger;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 触发器 属性
 *
 * @author hdfg159
 * @date 2019/12/5 12:21
 */
public interface TriggerProperties {
	/**
	 * 设置任务 ID
	 *
	 * @param id
	 * 		任务 ID
	 *
	 * @return Trigger
	 */
	TriggerProperties id(long id);
	
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
	 * @return TriggerProperties
	 */
	TriggerProperties name(String name);
	
	/**
	 * 获取任务取消状态
	 *
	 * @return T
	 */
	boolean isCancel();
	
	/**
	 * 设置任务取消状态
	 * 与{@link Trigger#scheduleCancel()}不同，需要等待下次执行才可以重新加入同名定时器
	 *
	 * @param cancel
	 * 		状态
	 *
	 * @return TriggerProperties
	 */
	TriggerProperties cancel(boolean cancel);
	
	/**
	 * 设置任务花费时间
	 *
	 * @param time
	 * 		时间(毫秒)
	 *
	 * @return TriggerProperties
	 */
	TriggerProperties costTime(long time);
	
	/**
	 * 获取上次任务执行时间
	 *
	 * @return long
	 */
	long getCostTime();
	
	/**
	 * 前后任务是否顺序执行
	 *
	 * @return boolean
	 */
	boolean isSequence();
	
	/**
	 * 设置 前后任务是否顺序执行<br>
	 * {@code true} 顺序执行，等前一个任务完成时候才执行下一个，任务执行时间过长会导致后一个任务开始时间顺延   <br>
	 * {@code false} 前一个任务开始时候 直接 放入后一个任务，前后任务执行时间互不关联,但是{@link TriggerProperties#getCostTime()}获得的时间会不准确（因为根据上一个任务执行完统计得到花费时间）<br>
	 *
	 * @param sequence
	 * 		前后任务是否顺序执行
	 *
	 * @return TriggerProperties
	 */
	TriggerProperties sequence(boolean sequence);
	
	/**
	 * 设置任务错误 重试次数
	 *
	 * @param times
	 * 		重试次数
	 *
	 * @return TriggerProperties
	 */
	TriggerProperties retry(long times);
	
	/**
	 * 初始化重试次数
	 */
	default void initRetryTimes() {
		Map<Long, Long> retryCountMap = getRetryCountMap();
		if (retryCountMap == null) {
			retryCountMap(new ConcurrentHashMap<>());
		}
		
		getRetryCountMap().put(getId(), getRetry());
	}
	
	/**
	 * 获取重试剩余次数
	 *
	 * @return {@code Map<Long, Long>} [任务ID:重试剩余次数]
	 */
	Map<Long, Long> getRetryCountMap();
	
	/**
	 * 设置重试剩余次数
	 *
	 * @param retryCountMap
	 * 		设置重试次数map
	 *
	 * @return {@code TriggerProperties} [任务ID:重试剩余次数]
	 */
	TriggerProperties retryCountMap(Map<Long, Long> retryCountMap);
	
	/**
	 * 获取任务 ID
	 *
	 * @return long
	 */
	long getId();
	
	/**
	 * 获取任务错误 重试次数
	 *
	 * @return long
	 */
	long getRetry();
}
