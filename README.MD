<p align="center">Scheduler - 轻量级任务调度工具</p>

# 简介

轻量级任务调度工具

# 优点

- 简易使用：简易创建方式和链式调用 API

- 无依赖：不依赖其他任何组件

- 动态：随时取消任务

- 事件触发：目前支持按照每日时间点调度、周几时间点调度、简单调度(次数、频率、时间范围控制)、支持同一任务顺序执行和非顺序执行

- 拓展性：可以继承 `AbstractTrigger` 默认继承类 或者 实现 `Trigger` 接口 实现自己的自定义触发器

- 调度线程：支持自定义的线程池实现,按照任务执行消耗时间自动调度到不同线程池

- 异常处理：支持针对每个定时器执行错误异常捕获后的自定义处理

- 错误重试：支持针对每个定时任务出错后设置反复重试次数

# 应用实例

## 创建任务

> 创建从现在开始，1秒触发1次，一共执行10次的任务

```
Triggers.times("自定义触发器名称", 10, 1, ChronoUnit.SECONDS, LocalDateTime.now(), trigger -> {
        // 业务处理
        System.out.println("hello world");
}).schedule();
```

更多创建方式可以查看 `hdfg159.scheduler.factory.Triggers` 工厂方法

## 取消任务

> 通过 Trigger 的 `scheduleCancel()` 方法 直接取消

```
Trigger trigger = ... ;
trigger.scheduleCancel();
```

> 通过 `ScheduleManager` 的 `cancel(String triggerName)` 根据触发器自定义名称进行取消

```
SchedulerManager.INSTANCE.cancel("自定义触发器名称");
```

## 定制任务异常处理

```
Triggers.times("自定义触发器名称", 10, 1, ChronoUnit.SECONDS, LocalDateTime.now(),
        trigger -> {
            // 业务处理
            System.out.println("hello world");
        })
        .afterExceptionCaught((trigger, throwable) -> {
            // 异常处理
        })
        .schedule();
```

待补充...