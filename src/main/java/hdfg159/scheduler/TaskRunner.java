package hdfg159.scheduler;

import hdfg159.scheduler.trigger.Trigger;
import hdfg159.scheduler.trigger.TriggerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

/**
 * 任务运行
 *
 * @author hdfg159
 * @version 1.0
 */
public class TaskRunner implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);
	private Trigger trigger;
	
	public TaskRunner(Trigger trigger) {
		this.trigger = trigger;
	}
	
	@Override
	public void run() {
		beforeJobRun();
		
		String triggerName = trigger.getName();
		if (trigger.isCancel()) {
			log.warn("[{}] job cancel,not execute", triggerName);
			return;
		}
		
		log.info("trigger job:[{}]", triggerName);
		LocalDateTime start = LocalDateTime.now();
		
		try {
			trigger.getJob().accept(trigger);
		} catch (Throwable e) {
			trigger.exceptionCaught(e);
		}
		
		LocalDateTime end = LocalDateTime.now();
		// 设置任务执行时间
		long until = start.until(end, ChronoUnit.MILLIS);
		trigger.costTime(until);
		
		log.info("job run success:[{}] [{}ms]", triggerName, until);
		
		afterJobRun();
	}
	
	private void beforeJobRun() {
		nextTriggerEffect(t -> !t.isSequence());
	}
	
	private void afterJobRun() {
		nextTriggerEffect(TriggerProperties::isSequence);
	}
	
	/**
	 * 生效并且放入下一个定时器
	 *
	 * @param predicate
	 * 		条件
	 */
	private void nextTriggerEffect(Predicate<Trigger> predicate) {
		if (!predicate.test(trigger)) {
			return;
		}
		
		trigger.nextTrigger().ifPresent(t -> {
			boolean schedule = trigger.schedule();
			log.debug("next trigger effect:[{}],task cost time:[{}ms],result:[{}]", t.getName(), t.getCostTime(), schedule);
		});
	}
}
