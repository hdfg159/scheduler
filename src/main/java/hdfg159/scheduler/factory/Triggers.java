package hdfg159.scheduler.factory;

import hdfg159.scheduler.trigger.Trigger;
import hdfg159.scheduler.trigger.impl.DayTrigger;
import hdfg159.scheduler.trigger.impl.SimpleTrigger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.function.Consumer;

/**
 * 调度构造工具
 *
 * @author hdfg159
 * @version 1.0
 */
public abstract class Triggers {
	/**
	 * 创建不带结束生效时间的简单定时器
	 *
	 * @param name
	 * 		名称
	 * @param times
	 * 		执行次数
	 * @param interval
	 * 		执行间隔
	 * @param intervalUnit
	 * 		执行间隔时间单位
	 * @param startTime
	 * 		开始生效时间
	 * @param job
	 * 		任务接口
	 *
	 * @return SimpleTrigger
	 */
	public static SimpleTrigger times(String name, long times, long interval, TemporalUnit intervalUnit, LocalDateTime startTime, Consumer<Trigger> job) {
		return new SimpleTrigger(name, times, interval, intervalUnit, startTime, null, job);
	}
	
	/**
	 * 创建在生效时间和结束时间之间固定间隔时间生效的简单定时器
	 *
	 * @param name
	 * 		名称
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
	 *
	 * @return SimpleTrigger
	 */
	public static SimpleTrigger foreverWithEndTime(String name, long interval, TemporalUnit intervalUnit, LocalDateTime startTime, LocalDateTime endTime, Consumer<Trigger> job) {
		return new SimpleTrigger(name, 0, interval, intervalUnit, startTime, endTime, job);
	}
	
	/**
	 * 创建没有结束时间而且固定间隔时间生效的简单定时器
	 *
	 * @param name
	 * 		名称
	 * @param interval
	 * 		执行间隔
	 * @param intervalUnit
	 * 		执行间隔时间单位
	 * @param startTime
	 * 		开始生效时间
	 * @param job
	 * 		任务接口
	 *
	 * @return SimpleTrigger
	 */
	public static SimpleTrigger forever(String name, long interval, TemporalUnit intervalUnit, LocalDateTime startTime, Consumer<Trigger> job) {
		return new SimpleTrigger(name, 0, interval, intervalUnit, startTime, null, job);
	}
	
	/**
	 * 创建一次性执行而且没有结束生效时间的简单定时器
	 *
	 * @param name
	 * 		名称
	 * @param time
	 * 		开始生效时间点
	 * @param job
	 * 		任务接口
	 *
	 * @return SimpleTrigger
	 */
	public static SimpleTrigger daily(String name, LocalTime time, Consumer<Trigger> job) {
		LocalDateTime todayTime = LocalDateTime.of(LocalDate.now(), time);
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		LocalDateTime startTime = todayTime.isBefore(LocalDateTime.now()) ? LocalDateTime.of(tomorrow, time) : todayTime;
		return new SimpleTrigger(name, 0, 1, ChronoUnit.DAYS, startTime, null, job);
	}
	
	/**
	 * 创建无限次执行而且没有结束生效时间的每日定时器
	 *
	 * @param name
	 * 		名称
	 * @param startTime
	 * 		开始生效时间
	 * @param job
	 * 		任务接口
	 *
	 * @return SimpleTrigger
	 */
	public static SimpleTrigger once(String name, LocalDateTime startTime, Consumer<Trigger> job) {
		return new SimpleTrigger(name, 1, 0, ChronoUnit.MILLIS, startTime, null, job);
	}
	
	/**
	 * 创建日调度定时器
	 *
	 * @param name
	 * 		名称
	 * @param days
	 * 		周几
	 * @param time
	 * 		时间点
	 * @param job
	 * 		任务
	 *
	 * @return DayTrigger
	 */
	public static DayTrigger dayTime(String name, int[] days, LocalTime time, Consumer<Trigger> job) {
		return new DayTrigger(name, days, time, job);
	}
}
