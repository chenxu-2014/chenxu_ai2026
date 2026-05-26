| 同步工具               | 基于 AQS 的模式  | 典型应用场景                                          |
| ---------------------- | ---------------- | ----------------------------------------------------- |
| ReentrantLock          | 独占模式 + 重入  | 需要可中断锁、超时锁、公平锁的场景，替代 ﻿synchronized |
| ReentrantReadWriteLock | 读共享 + 写独占  | 缓存系统、配置中心、任何读多写少的数据结构            |
| Semaphore              | 共享模式         | 限流（数据库连接池、API 限流）、有界阻塞队列          |
| CountDownLatch         | 共享模式（单向） | 主线程等待多个子任务完成、并发起点统一                |
| CyclicBarrier          | 独占 + Condition | 多线程分阶段计算、数据批处理、可重用屏障              |
|                        |                  |                                                       |

## 一、AQS 是什么？

`AbstractQueuedSynchronizer`（简称 **AQS**）是 `java.util.concurrent.locks` 包中的一个抽象类，它提供了一个 **基于 FIFO 等待队列** 的同步器框架，用来实现锁和同步工具（如 `ReentrantLock`、`Semaphore`、`CountDownLatch`、`ReentrantReadWriteLock` 等）的底层机制。

> **核心思想**：
> 如果被请求的共享资源是 **空闲的**，则将当前请求线程设置为有效的工作线程，并将资源锁定；
> 如果资源 **已被占用**，则通过 CLH 队列（一种 FIFO 双向链表）将暂时获取不到锁的线程加入队列，并阻塞它。

AQS 将**状态管理**、**线程阻塞/唤醒**、**等待队列** 等复杂逻辑封装好，子类只需实现 **资源获取与释放的判断逻辑**（即 `tryAcquire` / `tryRelease` 等方法），就能快速写出一个线程安全的同步器。



## 二、AQS 核心原理

### 1. 核心成员变量

```java
// 同步状态（volatile 保证可见性）
private volatile int state;

// 等待队列的头节点（延迟初始化）
private transient volatile Node head;

// 等待队列的尾节点
private transient volatile Node tail;
```

- **state**：表示共享资源的状态。例如：
    - 独占锁：`state = 0` 表示未锁定，`state = 1` 表示已锁定。
    - 计数信号量：`state` 表示剩余许可数量。
- **head / tail**：指向 CLH 双向等待队列的头部和尾部。

### 2. CLH 等待队列

- 队列中的每个节点（`Node`）封装一个被阻塞的线程（`Thread`）。
- 节点有 `prev` / `next` 指针，形成双向链表。
- 每个节点有 **等待状态**（`waitStatus`）：
    - `CANCELLED = 1`：线程已取消等待。
    - `SIGNAL = -1`：后继节点需要被唤醒。
    - `CONDITION = -2`：线程在条件队列中等待。
    - `PROPAGATE = -3`：共享模式下传播释放。

### 3. 两种资源共享方式

- **独占模式 (Exclusive)**：只有一个线程能获取资源，如 `ReentrantLock`。
- **共享模式 (Shared)**：多个线程可同时获取资源，如 `Semaphore`、`CountDownLatch`。

AQS 中这两种模式共用同一个 `state` 和等待队列，通过不同的节点类型区分。

## 三、AQS 工作流程简述（独占模式）

1. 线程调用 `acquire()` 尝试获取资源。
2. 调用 `tryAcquire()` 尝试立即获取：
    - 成功 → 直接返回，线程继续执行。
    - 失败 → 将当前线程封装成 `Node` 加入等待队列的尾部。
3. 在队列中，前驱节点状态为 `SIGNAL` 时，当前线程调用 `LockSupport.park()` 阻塞自己。
4. 当持有锁的线程调用 `release()`：
    - 调用 `tryRelease()` 释放资源（如 `state = 0`）。
    - 如果成功，唤醒头节点的后继节点（`unparkSuccessor()`）。
5. 被唤醒的线程继续尝试获取锁（自旋），成功则设置自己为头节点，离开队列。

> **共享模式类似，只是 tryAcquireShared 返回剩余资源数（负数表示失败），释放时可能唤醒多个后继节点。**




  