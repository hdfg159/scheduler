package hdfg159.scheduler.trigger;

import java.time.LocalDateTime;

/**
 * 触发器 属性
 *
 * @author hdfg159
 * @date 2019/12/5 12:21
 */
public interface TriggerProperties {
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
}
