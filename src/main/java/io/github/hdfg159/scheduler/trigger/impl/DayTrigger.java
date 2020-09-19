package io.github.hdfg159.scheduler.trigger.impl;

import io.github.hdfg159.scheduler.function.Consumer;
import io.github.hdfg159.scheduler.trigger.AbstractTrigger;
import io.github.hdfg159.scheduler.trigger.Trigger;
import io.github.hdfg159.scheduler.util.Sequence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.stream.Collectors.toList;

/**
 * 日调度触发器
 *
 * @author hdfg159
 * @version 1.0
 */
public class DayTrigger extends AbstractTrigger<DayTrigger> {
	private static final long serialVersionUID = 6539676699114311448L;
	private List<DayOfWeek> days;
	private LocalTime time;

	/**
	 * 创建日调度触发器
	 *
	 * @param name
	 * 		名称
	 * @param days
	 * 		生效星期数组,ex:1是星期一,7是星期天
	 * @param time
	 * 		时间点
	 * @param job
	 * 		任务接口
	 */
	public DayTrigger(String name, int[] days, LocalTime time, Consumer<Trigger> job) {
		if (name == null) {
			throw new IllegalArgumentException("trigger must have a name");
		}
		name(name);

		if (time == null) {
			throw new IllegalArgumentException("trigger time required not null");
		}
		this.time = time;

		if (job == null) {
			throw new IllegalArgumentException("trigger job required not null");
		}
		job(job);

		if (days == null || days.length == 0) {
			throw new IllegalArgumentException("trigger day required not null or length > 0");
		}
		this.days = Arrays.stream(days).sorted().mapToObj(DayOfWeek::of).collect(toList());

		executeTime(getNextExecuteTime());
	}

	/**
	 * 获取下次执行时间
	 *
	 * @return LocalDateTime
	 */
	private LocalDateTime getNextExecuteTime() {
		return days.stream()
				.map(dw -> LocalDate.now().with(dw))
				.map(date -> LocalDateTime.of(date, time))
				.filter(dateTime -> !dateTime.isBefore(LocalDateTime.now()))
				.findFirst()
				.orElseGet(() -> {
					DayOfWeek adjuster = days.get(0);
					LocalDate date = LocalDate.now().plusWeeks(1).with(adjuster);
					return LocalDateTime.of(date, time);
				});
	}

	public List<DayOfWeek> getDays() {
		return days;
	}

	public LocalTime getTime() {
		return time;
	}

	@Override
	public Optional<Trigger> nextTrigger() {
		if (isCancel()) {
			return Optional.empty();
		}

		// 设置上次执行时间
		setPreviousTime(LocalDateTime.now());
		// 设置下次执行时间
		executeTime(getNextExecuteTime());
		// 更新有序 ID
		id(Sequence.SEQUENCE.nextId());
		// 更新当前任务ID对应的错误重试次数
		initRetryTimes();
		return Optional.of(this);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", DayTrigger.class.getSimpleName() + "[", "]")
				.add("days=" + days)
				.add("time=" + time)
				.add(super.toString())
				.toString();
	}
}
