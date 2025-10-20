package com.pewee.util;

import org.hibernate.service.spi.ServiceException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 通用任务
 * 擦除泛型后无法返回类型,需要自己维护返回类型
 * @author pewee
 *
 */
public abstract class CommonTask<T,R extends Serializable> implements Runnable{
	
	public static final ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 50, 60, TimeUnit.MINUTES, new ArrayBlockingQueue<>(5000));	
	
	public static final ScheduledExecutorService executor1 =  Executors.newScheduledThreadPool(5);
	
	private volatile boolean hasDepend = false;
	
	private volatile boolean sync = true;
	
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	private List<CommonTask> dependOn;
	
	private Long timeout = 5000L;
	
	/**
	 * 0:初始 1:进行中  2:成功 3:失败 
	 */
	private volatile int status = 0;
	/**
	 * 异常,所有的依赖异常应该被传递到顶层!!
	 */
	private Exception ex;
	
	private volatile boolean isDependDone;
	
	private T params;
	
	private R result;
	
	private CountDownLatch cdl;
	
	private CountDownLatch myLatch = new CountDownLatch(1);
	
	public CommonTask(T params) {
		if(null != params ) {
			this.params = params;
		}
	}
	
	public static <A> Future<A>  submitSingle(Callable<A>  ca) {
		Future<A> submit = executor.submit(ca);
		return submit;
	}
	
	public CommonTask(T params, List<CommonTask> dependOn) {
		if(null != params ) {
			this.params = params;
		}
		if(null != dependOn && !dependOn.isEmpty()) {
			this.hasDepend = true;
			this.dependOn = new ArrayList<CommonTask>();
			this.dependOn.addAll(dependOn);
			isDependDone = false;
			cdl = new CountDownLatch(dependOn.size());
			
			for (CommonTask commonTask : dependOn) {
				commonTask.getClass().getGenericSuperclass();
			}
		}
	}
	
	public CommonTask(T params, List<CommonTask> dependOn, Long timeout) {
		if(null != params ) {
			this.params = params;
		}
		if(null != dependOn && !dependOn.isEmpty()) {
			this.hasDepend = true;
			this.dependOn = new ArrayList<CommonTask>();
			dependOn.addAll(dependOn);
			isDependDone = false;
			cdl = new CountDownLatch(dependOn.size());
		}
		if(null != timeout && timeout > 5000L) {
			this.timeout = timeout;
		}
	}
	
	public void applyLatch(CountDownLatch cdl) {
		this.cdl = cdl;
	}
	
	private void doDependsWork() {
		if(hasDepend) {
			for (CommonTask<?, ?> commonTask : dependOn) {
				commonTask.applyLatch(cdl);
				executor.submit(commonTask);
			}
			try {
				boolean await = cdl.await(timeout, TimeUnit.MILLISECONDS);
				if(!await) {
					this.status = 3;
					this.ex = new ServiceException("依赖任务超时,class=" + this.getClass().getName());
					this.isDependDone = false;
					thisTaskFinish();
				} else {
					List<CommonTask> err =  dependOn.stream().filter(dp -> dp.getStatus() == 3).collect(Collectors.toList());
					if(null != err && !err.isEmpty()) {
						CommonTask task = err.get(0);
						ex =  task.getEx();
						this.status = 3;
						this.isDependDone = false;
					} else {
						this.status = 1;
						this.isDependDone = true;
					}
				}
			} catch (InterruptedException e) {
				this.status = 3;
				this.ex = new ServiceException(e.getMessage(),e);
				this.isDependDone = false;
				thisTaskFinish();
			}
		} 
	}
	
	private void doWork() {
		Future<R> future = executor.submit( new Callable<R>() {
			private CommonTask<T,R> task;
			public Callable<R> applyFunctionParams(CommonTask<T,R> task){
				this.task = task;
				return this;
			}
			@Override
			public R call() throws Exception {
				R apply = task.doMyWork();
				return apply;
			}
		}.applyFunctionParams(this));
		try {
			R r = future.get(timeout, TimeUnit.MILLISECONDS);
			this.status = 2;
			this.result = r;
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			this.status = 3;
			this.ex = new ServiceException(e1.getMessage(),e1);
		} finally {
			if(null != cdl) {
				cdl.countDown();
			}
			thisTaskFinish();
		}
	}
	
	private void thisTaskFinish() {
		this.myLatch.countDown();
	}

	@Override
	public void run() {
		this.status = 1;
		doDependsWork();
		if(this.status == 1 ) {
			doWork();
		} else {
			thisTaskFinish();
		}
	}
	
	/**
	 * 任务开始入口
	 */
	public void doSubmit() {
		executor.submit(this);
		if(this.sync) {
			try {
				boolean await = myLatch.await(timeout, TimeUnit.MILLISECONDS);
				if(!await) {
					this.status = 3;
					this.ex = new ServiceException("任务超时");
				} 
			} catch (InterruptedException e) {
				this.status = 3;
				this.ex = new ServiceException("等待任务中断");
			}
		}
	}
	
	public abstract R doMyWork();
	
	public int getStatus() {
		return status;
	}

	public Exception getEx() {
		return ex;
	}

	public T getParams() {
		return params;
	}

	public R getResult() {
		return result;
	}

	public List<CommonTask> getDependOn() {
		return dependOn;
	}
	
	/**
	 * 使用示例
	 * 任务1--
	 *        |--任务3--结束 
	 * 任务2--
	 * @param args
	 */
	public static void main(String[] args) {
		CommonTask<Integer, Integer> commonTask1 = new CommonTask<Integer,Integer>(1000){
			@Override
			public Integer doMyWork() {
				return getParams() + 1;
			}
		};
		
		CommonTask<Integer, String> commonTask2 = new CommonTask<Integer,String>(2){
			@Override
			public String doMyWork() {
				return getParams() + 1 + "";
			}
		};
		ArrayList<CommonTask> list = new ArrayList<CommonTask>();
		list.add(commonTask1);
		list.add(commonTask2);
		CommonTask<String,String> commonTask3 = new CommonTask<String,String>("AAA",list){
			@Override
			public String doMyWork() {
				//泛型擦除后需要自己维护类型
				List collect = getDependOn().stream().map(CommonTask::getResult).collect(Collectors.toList());
				Object r1 = collect.get(0);
				Integer i = (Integer)r1;
				Object r2 = collect.get(1);
				String s = (String)r2;
				
				///**
				try {
					System.out.println("休息4秒钟模拟一些计算逻辑或者调用逻辑!!");
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//**/
				return getParams() + 1 + ",第一个任务结果:" + r1 + ",第二个任务结果:" + s;
			}
		};
		commonTask3.setSync(true);
		commonTask3.doSubmit();
		System.out.println("任务状态:" + commonTask3.getStatus());
		if(2 == commonTask3.getStatus()) {
			System.out.println(commonTask3.getResult());
		}
		//spring项目中千万不要关掉threadpool,main方法中需要关掉释放内存
		commonTask3.executor.shutdown();
	}
	
}
