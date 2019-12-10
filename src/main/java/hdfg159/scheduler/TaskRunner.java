package hdfg159.scheduler;

import hdfg159.scheduler.trigger.Trigger;
import hdfg159.scheduler.trigger.TriggerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
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
		long triggerId = trigger.getId();
		
		beforeJobRun();
		
		try {
			boolean isThrowException = jobRun();
			if (isThrowException) {
				retry(triggerId, trigger);
			}
		} finally {
			afterJobRun();
		}
	}
	
	/**
	 * 任务执行前
	 */
	private void beforeJobRun() {
		nextTriggerEffect(t -> !t.isSequence());
	}
	
	/**
	 * 任务执行
	 *
	 * @return boolean true:任务执行出现异常返回 ,false:任务正常执行无错误
	 */
	private boolean jobRun() {
		if (trigger.isCancel()) {
			log.warn("[{}] job cancel", trigger.getName());
			return false;
		}
		
		boolean isThrowException = false;
		String triggerName = trigger.getName();
		log.info("trigger job:[{}]", triggerName);
		LocalDateTime startTime = LocalDateTime.now();
		
		try {
			trigger.getJob().accept(trigger);
		} catch (Throwable e) {
			isThrowException = true;
			try {
				trigger.exceptionCaught(e);
			} catch (Throwable e1) {
				log.error("trigger job exception caught error", e1);
			}
		}
		
		// 设置任务执行时间
		long until = startTime.until(LocalDateTime.now(), ChronoUnit.MILLIS);
		trigger.costTime(until);
		
		log.info("job run success:[{}] [{}ms]", triggerName, until);
		
		return isThrowException;
	}
	
	/**
	 * 重试
	 *
	 * @param triggerId
	 * 		触发器原 ID
	 * @param trigger
	 * 		触发器
	 */
	private void retry(long triggerId, Trigger trigger) {
		Map<Long, Long> retryCountMap = trigger.getRetryCountMap();
		Long retryTimes = retryCountMap.getOrDefault(triggerId, 0L);
		
		String triggerName = trigger.getName();
		
		// 重复尝试
		for (long i = retryTimes; i > 0; i--) {
			log.info("[{}] job remain retry times:[{}/{}]", triggerName, i, retryTimes);
			try {
				trigger.getJob().accept(trigger);
			} catch (Throwable e) {
				// 防止 afterExceptionCaught 方法处理再次出现异常
				try {
					trigger.exceptionCaught(e);
				} catch (Throwable e1) {
					log.error("retry trigger job exception caught error", e1);
				}
				continue;
			}
			break;
		}
		
		// 移除
		retryCountMap.remove(triggerId);
	}
	
	/**
	 * 任务执行后
	 */
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
		Optional.ofNullable(trigger)
				.filter(predicate)
				.flatMap(Trigger::nextTrigger)
				.ifPresent(t -> {
					boolean schedule = trigger.schedule();
					log.debug("next trigger effect:[{}],task cost time:[{}ms],result:[{}]", t.getName(), t.getCostTime(), schedule);
				});
	}
}
