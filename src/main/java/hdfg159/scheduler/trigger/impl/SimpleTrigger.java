package hdfg159.scheduler.trigger.impl;

import hdfg159.scheduler.trigger.AbstractTrigger;
import hdfg159.scheduler.trigger.Trigger;
import hdfg159.scheduler.util.Sequence;

import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * 简单任务调度定时器
 *
 * @author hdfg159
 * @version 1.0
 */
public class SimpleTrigger extends AbstractTrigger<SimpleTrigger> {
	private static final long EXECUTE_TIME_FOREVER = -1L;
	
	private static final long serialVersionUID = -5139037867976863476L;
	
	/**
	 * 当前剩余执行次数
	 */
	private long executeTimes;
	/**
	 * 执行间隔
	 */
	private long interval;
	/**
	 * 执行间隔单位
	 */
	private TemporalUnit intervalUnit;
	/**
	 * 定时器生效时间
	 */
	private LocalDateTime startTime;
	/**
	 * 定时器结束生效时间
	 */
	private LocalDateTime endTime;
	
	/**
	 * 创建简单定时器
	 *
	 * @param name
	 * 		名称
	 * @param times
	 * 		执行次数:0 为无限次
	 * @param interval
	 * 		执行间隔
	 * @param intervalUnit
	 * 		执行间隔时间单位
	 * @param startTime
	 * 		开始生效时间
	 * @param endTime
	 * 		结束生效时间
	 * @param job
	 * 		任务接口
	 */
	public SimpleTrigger(String name, long times, long interval, TemporalUnit intervalUnit, LocalDateTime startTime, LocalDateTime endTime, Consumer<Trigger> job) {
		if (name == null) {
			throw new IllegalArgumentException("trigger must have a name");
		}
		name(name);
		
		long executeTimes = times - 1;
		if (executeTimes < EXECUTE_TIME_FOREVER) {
			throw new IllegalArgumentException("trigger times must be >= 0");
		}
		this.executeTimes = executeTimes;
		
		if (interval < 0) {
			throw new IllegalArgumentException("trigger interval must be >= 0");
		}
		this.interval = interval;
		if (intervalUnit == null) {
			throw new IllegalArgumentException("trigger interval unit required not null");
		}
		this.intervalUnit = intervalUnit;
		
		if (startTime == null) {
			throw new IllegalArgumentException("trigger start time required not null");
		}
		this.startTime = startTime;
		executeTime(startTime);
		
		this.endTime = endTime;
		
		if (job == null) {
			throw new IllegalArgumentException("trigger job required not null");
		}
		job(job);
	}
	
	public long getExecuteTimes() {
		return executeTimes;
	}
	
	public long getInterval() {
		return interval;
	}
	
	public TemporalUnit getIntervalUnit() {
		return intervalUnit;
	}
	
	public LocalDateTime getStartTime() {
		return startTime;
	}
	
	public LocalDateTime getEndTime() {
		return endTime;
	}
	
	@Override
	public Optional<Trigger> nextTrigger() {
		if (isCancel()) {
			return Optional.empty();
		}
		if (executeTimes == 0) {
			return Optional.empty();
		}
		
		LocalDateTime now = LocalDateTime.now();
		if (endTime != null && endTime.isBefore(now)) {
			return Optional.empty();
		}
		
		if (executeTimes != EXECUTE_TIME_FOREVER) {
			// 不是无限次执行,才执行次数-1
			executeTimes -= 1;
		}
		// 设置上次执行时间
		setPreviousTime(now);
		// 设置下次执行时间
		executeTime(now.plus(interval, intervalUnit));
		// 更新有序 ID
		id(Sequence.SEQUENCE.nextId());
		// 更新当前任务ID对应的错误重试次数
		initRetryTimes();
		return Optional.of(this);
	}
	
	@Override
	public String toString() {
		return new StringJoiner(", ", SimpleTrigger.class.getSimpleName() + "[", "]")
				.add("executeTimes=" + executeTimes)
				.add("interval=" + interval)
				.add("intervalUnit=" + intervalUnit)
				.add("startTime=" + startTime)
				.add("endTime=" + endTime)
				.add(super.toString())
				.toString();
	}
}
