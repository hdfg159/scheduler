package io.github.hdfg159.scheduler;

import io.github.hdfg159.scheduler.factory.Triggers;
import io.github.hdfg159.scheduler.trigger.impl.SimpleTrigger;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * Project:scheduler
 * Package:hdfg159.scheduler
 * Created by hdfg159 on 2019/11/16 19:08.
 */
public class SchedulerManagerTest {
	private static final Logger log = LoggerFactory.getLogger(SchedulerManagerTest.class);

	@Test
	public void simple() throws InterruptedException {
		SchedulerManager instance = SchedulerManager.INSTANCE
				.setTakeQueueInterruptListener(triggers -> {
					log.info("interrupt!queue size:[{}]", triggers.size());
					triggers.forEach(trigger -> log.info("{}", trigger));
				});

		SimpleTrigger forever = Triggers.forever("forever",
				1,
				ChronoUnit.SECONDS,
				LocalDateTime.now(),
				trigger -> Files.lines(Paths.get(UUID.randomUUID().toString())))
				.afterExceptionCaught((trigger, throwable) -> log.error("单独实现异常捕获，异常信息:{}", throwable.getMessage(), throwable));
		forever.schedule();

		Thread.sleep(5_000);

		forever.scheduleCancel();

		Thread.sleep(5_000);
		instance.shutdown();
		Thread.sleep(2_000);
	}

	@Test
	public void sequence() throws InterruptedException {
		Triggers.times("sequence",
				5,
				1,
				ChronoUnit.SECONDS,
				LocalDateTime.now(),
				trigger -> {
					log.info("start sequence");
					// 随机延迟时间
					int second = ThreadLocalRandom.current().nextInt(0, 5) * 1_000;
					try {
						Thread.sleep(second);
					} catch (InterruptedException e) {
						// ignore
					}
				})
				.sequence(true)
				.schedule();

		Thread.sleep(30_000);
	}

	@Test
	public void dispatch() throws InterruptedException {
		Triggers.times("triggers",
				10,
				1,
				ChronoUnit.SECONDS,
				LocalDateTime.now(),
				trigger -> {
					// 随机延迟时间测试自动调度
					int second = ThreadLocalRandom.current().nextInt(0, 5) * 1_000;
					try {
						Thread.sleep(second);
						throw new RuntimeException("出现异常");
					} catch (InterruptedException e) {
						// ignore
					}
					log.info("[{}]", ((SimpleTrigger) trigger).getExecuteTimes());
				})
				.afterExceptionCaught((trigger, throwable) -> {
					throw new RuntimeException("测试");
				})
				// 准确自动调度需要顺序任务
				.sequence(true)
				.schedule();

		Thread.sleep(60_000);
	}

	@Test
	public void retry() throws InterruptedException {
		final LongAdder adder = new LongAdder();
		int times = 1;
		int retryTimes = 50;
		Triggers.times("retry",
				times,
				1,
				ChronoUnit.SECONDS,
				LocalDateTime.now(),
				trigger -> {
					adder.increment();
					int i = ThreadLocalRandom.current().nextInt(0, 10);
					log.debug("retry-random:[{}]", i);
					if (i > 0 || adder.longValue() == 1L) {
						throw new RuntimeException("出错了啊");
					}
				})
				.afterExceptionCaught((trigger, throwable) -> {
					throw new RuntimeException("捕获异常继续抛出错误");
				})
				.retry(retryTimes)
				// .sequence(true)
				.schedule();

		Thread.sleep(10_000);

		log.debug("execute time:[{}]", adder.sum());
	}

	@Test
	public void repeatTask() throws InterruptedException {
		boolean result1 = Triggers.once("test", LocalDateTime.now().plusMinutes(3), trigger -> {}).schedule();
		Assert.assertTrue(result1);

		Thread.sleep(1000);

		SimpleTrigger test = Triggers.once("test", LocalDateTime.now().plusMinutes(3), trigger -> {});
		log.info(test.toString());
		boolean result2 = test.schedule();
		Assert.assertFalse(result2);
	}
}