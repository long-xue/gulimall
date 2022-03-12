package com.atguigu.gulimall.search.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @longxue
 *
 * 
 */
public class ThreadTest {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		
//		System.out.println("main...start");
//		FutureTask<Integer> futureTask = new FutureTask<Integer>(new Thread02());
//		new Thread(futureTask).run();
//		//阻塞等待整个线程执行完成，获取返回结果
//		Integer integer = futureTask.get();
//		System.out.println("main..end"+integer);
		//创建线程池的方式
//		CompletableFuture.supplyAsync(supplier, executor)
		
		
	}

	/**
	 * 线程池
	 * int corePoolSize, 
       int maximumPoolSize,
       long keepAliveTime,	比核心线程数多的那一部分的线程的等待任务时间
       TimeUnit unit,
       BlockingQueue<Runnable> workQueue, 阻塞队列，存储多的线程
       ThreadFactory threadFactory 创建线程的工厂对象
	 */
	public void threadPool() {
//		new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler)
		//Excuters几种常见的线程池
//		Executors.newCachedThreadPool()  core为0，创建一个线程池，该线程池根据需要创建新线程，但是
//	      当可用时，将重用先前构造的线程。
//		Executors.newFixedThreadPool(nThreads)创建一个重用固定数量线程的线程池 
//		Executors.newScheduledThreadPool(corePoolSize) Creates a thread pool that can schedule 
//		commands to run after a given delay, or to execute periodically(定期地).
//		Executors.newSingleThreadExecutor() 创建一个执行程序，该执行程序使用在不受限制的队列上操作的单个工作线程。 
	}
	
	
	public static class Thread01 extends Thread{
		public void run() {
			System.out.println("当前线程"+currentThread().getId());
			int i = 10/2;
			System.out.println("运行结果"+i);
		}
	}
	
	public static class Thread03 implements Runnable{
		public void run() {
			System.out.println("当前线程"+Thread.currentThread().getId());
			int i = 10/2;
			System.out.println("运行结果"+i);
		}
	}
	public static class Thread02 implements Callable<Integer>{

		@Override
		public Integer call() throws Exception {
			// TODO Auto-generated method stub
			System.out.println("当前线程"+Thread.currentThread().getId());
			int i = 10/2;
			System.out.println("运行结果"+i);
			return i;
		}
			
		
	}
	
}
