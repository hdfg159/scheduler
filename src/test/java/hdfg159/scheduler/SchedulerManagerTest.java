package hdfg159.scheduler;

import hdfg159.scheduler.factory.Triggers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Project:scheduler
 * Package:hdfg159.scheduler
 * Created by hdfg159 on 2019/11/16 19:08.
 */
public class SchedulerManagerTest {
	private static final Logger log = LoggerFactory.getLogger(SchedulerManagerTest.class);
	
	@Test
	public void test() throws InterruptedException {
		SchedulerManager instance = SchedulerManager.INSTANCE
				.setTakeQueueInterruptListener(triggers -> {
					log.info("interrupt!queue size:[{}]", triggers.size());
					triggers.forEach(trigger -> log.info("{}", trigger));
				});
		
		Triggers.forever("forever", 1, ChronoUnit.SECONDS, LocalDateTime.now(),
				trigger -> {
					throw new RuntimeException("===========");
				})
				.afterExceptionCaught((trigger, throwable) -> log.error("单独实现异常捕获，异常信息:{}", throwable.getMessage(), throwable))
				.schedule();
		
		Thread.sleep(5_000);
		instance.shutdown();
		Thread.sleep(2_000);
	}
}