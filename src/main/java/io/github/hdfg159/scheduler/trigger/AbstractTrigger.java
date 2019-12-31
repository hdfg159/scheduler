package io.github.hdfg159.scheduler.trigger;

import io.github.hdfg159.scheduler.SchedulerManager;
import io.github.hdfg159.scheduler.util.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 抽象任务触发器接口
 *
 * @author hdfg159
 * @version 1.0
 */
public abstract class AbstractTrigger<T extends AbstractTrigger<T>> implements Trigger {
	private static final long serialVersionUID = -8924096205973321207L;
	
	private static final Logger log = LoggerFactory.getLogger(AbstractTrigger.class);
	
	private long id = Sequence.SEQUENCE.nextId();
	private Consumer<Trigger> job;
	private long retry = 0L;
	private Map<Long, Long> retryCount = new ConcurrentHashMap<>();
	private String name;
	private boolean cancel = false;
	private long costTime;
	private boolean sequence = false;
	private LocalDateTime previousTime;
	private LocalDateTime executeTime;
	private LocalDateTime createTime = LocalDateTime.now();
	private BiConsumer<Trigger, Throwable> exceptionCaughtConsumer = (trigger, cause) -> log.error("[{}] job run error", getName(), cause);
	
	/**
	 * 获取上次执行时间
	 *
	 * @return LocalDateTime
	 */
	public LocalDateTime getPreviousTime() {
		return previousTime;
	}
	
	public T setPreviousTime(LocalDateTime previousTime) {
		this.previousTime = previousTime;
		return self();
	}
	
	/**
	 * this 强转 T
	 *
	 * @return T
	 */
	private T self() {
		return (T) this;
	}
	
	@Override
	public int compareTo(Delayed o) {
		if (this == o) {
			return 0;
		}
		
		if (o instanceof Trigger) {
			Trigger trigger = (Trigger) o;
			
			if (getExecuteTime() == trigger.getExecuteTime()) {
				// 执行时间相同,按照放入有序的id比较
				return Long.compare(getId(), trigger.getId());
			}
		}
		
		return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
	}
	
	@Override
	public long getDelay(TimeUnit unit) {
		LocalDateTime now = LocalDateTime.now();
		long duration = now.until(getExecuteTime(), ChronoUnit.MILLIS);
		return unit.convert(duration, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public T id(long id) {
		this.id = id;
		return self();
	}
	
	@Override
	public LocalDateTime getCreateTime() {
		return createTime;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public T name(String name) {
		this.name = name;
		return self();
	}
	
	@Override
	public boolean isCancel() {
		return cancel;
	}
	
	@Override
	public T cancel(boolean cancel) {
		this.cancel = cancel;
		return self();
	}
	
	@Override
	public T costTime(long time) {
		costTime = time;
		return self();
	}
	
	@Override
	public long getCostTime() {
		return costTime;
	}
	
	@Override
	public boolean isSequence() {
		return sequence;
	}
	
	@Override
	public T sequence(boolean sequence) {
		this.sequence = sequence;
		return self();
	}
	
	@Override
	public T retry(long times) {
		retry = times;
		initRetryTimes();
		return self();
	}
	
	@Override
	public Map<Long, Long> getRetryCountMap() {
		return retryCount;
	}
	
	@Override
	public T retryCountMap(Map<Long, Long> retryCountMap) {
		// 不支持设置
		return self();
	}
	
	@Override
	public long getId() {
		return id;
	}
	
	@Override
	public long getRetry() {
		return retry;
	}
	
	@Override
	public Consumer<Trigger> getJob() {
		return job;
	}
	
	@Override
	public T job(Consumer<Trigger> job) {
		this.job = job;
		return self();
	}
	
	@Override
	public LocalDateTime getExecuteTime() {
		return executeTime;
	}
	
	@Override
	public T executeTime(LocalDateTime executeTime) {
		this.executeTime = executeTime;
		return self();
	}
	
	@Override
	public boolean schedule() {
		return SchedulerManager.INSTANCE.schedule(this);
	}
	
	@Override
	public boolean scheduleCancel() {
		return SchedulerManager.INSTANCE.cancel(getName());
	}
	
	@Override
	public BiConsumer<Trigger, Throwable> getAfterExceptionCaught() {
		return exceptionCaughtConsumer;
	}
	
	@Override
	public T afterExceptionCaught(BiConsumer<Trigger, Throwable> consumer) {
		exceptionCaughtConsumer = consumer;
		return self();
	}
	
	@Override
	public String toString() {
		return new StringJoiner(", ", AbstractTrigger.class.getSimpleName() + "[", "]")
				.add("id=" + id)
				.add("job=" + job)
				.add("retry=" + retry)
				.add("retryCount=" + retryCount)
				.add("name='" + name + "'")
				.add("cancel=" + cancel)
				.add("costTime=" + costTime)
				.add("sequence=" + sequence)
				.add("previousTime=" + previousTime)
				.add("executeTime=" + executeTime)
				.add("createTime=" + createTime)
				.toString();
	}
}
