package org.example.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//ThreadLocal 内存泄漏
//ThreadLocal 确实存在内存泄漏风险，这主要和它的内部结构设计有关。
//  1. 底层数据结构：
//ThreadLocal 内部维护了一个静态内部类 ThreadLocalMap。
//注意：这个 Map 是存在于 Thread（线程）对象中的，而不是 ThreadLocal 中。
//Key：是 ThreadLocal 对象的实例（弱引用）。
//Value：是我们调用 set() 存入的值（强引用）。
//  2. 为什么会内存泄漏？
//Key 的泄漏：ThreadLocalMap 中的 Key 被设计为弱引用。这意味着如果外部没有强引用指向这个 ThreadLocal 对象（比如方法结束，局部变量销毁），GC 时 Key 就会被回收，变成 null。
//Value 的泄漏（真正的坑）：虽然 Key 变成了 null，但 Value 是强引用，且 Entry 还在链表中。
//线程池的放大效应：在线程池场景下，线程是复用的，线程对象生命周期极长。如果不手动清理，这个 Key 为 null 但 Value 不为 null 的 Entry 会一直堆积在 Thread 的 Map 里，随着任务增多，导致内存溢出（OOM）。
//  3. 解决方案：
//强制 remove()：遵循 “谁使用，谁清理” 的原则。在代码中必须使用 try...finally 块，在 finally 中显式调用 threadLocal.remove()，将 Entry 从 Map 中移除，切断强引用。
public class ThreadLocalOOM {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                // 局部 ThreadLocal，方法结束后 ThreadLocal 对象不再被引用
                ThreadLocal<byte[]> localThreadLocal = new ThreadLocal<>();
                byte[] data = new byte[10 * 1024 * 1024]; // 10MB
                data[0]='c';
                data[1]='x';
                localThreadLocal.set(data);
//                在这段代码中，key 对象就是 localThreadLocal 这个 ThreadLocal 实例本身。
//                具体来说：
//                当你调用 localThreadLocal.set(data) 时，底层会获取当前线程的 ThreadLocalMap。
//                然后在这个 Map 中创建一个 Entry，key 是 localThreadLocal（即你 new 出来的那个 ThreadLocal 对象），value 是你传入的 data。
//                这个 Entry 对 key 的引用是弱引用（WeakReference<ThreadLocal<?>>），对 value 是强引用。
//                所以，之后 localThreadLocal 局部变量一旦出作用域，没有其他强引用指向这个 ThreadLocal 实例时，GC 就会回收它，此时 Map 中的 entry 的 key 就变成了 null，但 value 依然被强引用着，这就是内存泄漏的根源。

                System.out.println(Thread.currentThread().getName() + " 设置数据， localThreadLocal=" + localThreadLocal);

                // 业务逻辑...
                try { TimeUnit.MILLISECONDS.sleep(100); } catch (InterruptedException ignored) {} finally {
                    // 没有调用 remove()
                    //localThreadLocal.remove();
                }

                // localThreadLocal 现在离开作用域，下一次 GC 会被回收
            });
            TimeUnit.SECONDS.sleep(1);
        }

        // 等待任务执行完，触发 GC
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.gc();
        System.out.println("主动 GC 完成，但线程池线程的 ThreadLocalMap 中还残留着 value（byte[]）");
        // 理论上下面的代码会看到线程仍然持有大对象 (可通过 jmap 或 监控工具 验证)
        TimeUnit.SECONDS.sleep(60); // 留时间用工具观察
    }
}
