package hdfg159.scheduler;

import hdfg159.scheduler.trigger.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
		log.info("job run success:[{}] [{}ms]", triggerName, start.until(end, ChronoUnit.MILLIS));
	}
}
