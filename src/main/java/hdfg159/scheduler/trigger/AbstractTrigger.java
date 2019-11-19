package hdfg159.scheduler.trigger;

import hdfg159.scheduler.SchedulerManager;
import hdfg159.scheduler.util.Sequence;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.StringJoiner;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 抽象任务触发器接口
 *
 * @author hdfg159
 * @version 1.0
 */
public abstract class AbstractTrigger<T extends AbstractTrigger<T>> implements Trigger {
	private static final long serialVersionUID = -8924096205973321207L;
	
	private long id = Sequence.SEQUENCE.nextId();
	private Consumer<Trigger> job;
	private String name;
	private boolean cancel = false;
	private LocalDateTime previousTime;
	private LocalDateTime executeTime;
	private LocalDateTime createTime = LocalDateTime.now();
	
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
	public long getId() {
		return id;
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
	public long getDelay(TimeUnit unit) {
		LocalDateTime now = LocalDateTime.now();
		long duration = now.until(getExecuteTime(), ChronoUnit.MILLIS);
		return unit.convert(duration, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public String toString() {
		return new StringJoiner(", ", AbstractTrigger.class.getSimpleName() + "[", "]")
				.add("id=" + id)
				.add("name='" + name + "'")
				.add("cancel=" + cancel)
				.add("previousTime=" + previousTime)
				.add("executeTime=" + executeTime)
				.add("createTime=" + createTime)
				.toString();
	}
}
