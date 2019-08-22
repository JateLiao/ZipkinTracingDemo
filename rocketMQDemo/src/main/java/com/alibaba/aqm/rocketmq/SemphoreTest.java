package com.alibaba.aqm.rocketmq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liaoshijie
 * @description: ${TODO}
 * @Createtime 2019/8/22 16:32
 */
public class SemphoreTest {
    static Semaphore semaphore = new Semaphore(1);
    static volatile int count = 0;
    static List<String> list = new ArrayList<>(100);
    static AtomicInteger atomicInteger = new AtomicInteger(0);
    
    /**
     * main method.
     **/
    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new Thread(new Task(1));
        Thread thread2 = new Thread(new Task(2));
        Thread thread3 = new Thread(new Task(3));
    
    
        thread1.start();
        thread2.start();
        thread3.start();
        //ExecutorService service = Executors.newFixedThreadPool(3);
        //for (int i = 0; i < 100; i++) {
        //    service.execute(new Task(1));
        //}
        
        list.stream().forEach(s -> System.out.println(s));
    }
    
    static class Task implements Runnable {
        private int id;
        
        public Task(int id) {
            this.id = id;
        }
        
        @Override
        public void run() {
            try {
                while (atomicInteger.get() < 100) {
                    semaphore.acquire();
                    if (atomicInteger.get() >= 100) {
                        return;
                    }
    
                    if (atomicInteger.get() % 3  != (id - 1)) {
                        semaphore.release();
                        continue;
                    }
                    semaphore.release();
                    list.add(id + ": " + atomicInteger.incrementAndGet());
                    //System.out.println(id + ": " + count);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }
}
