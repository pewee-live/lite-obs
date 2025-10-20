package com.pewee.util;
/**
 * 通用重入锁
 * @author pewee
 *
 */
public interface ILock {
	
	/**
	 * 锁
	 * @param lockKey 锁头
	 * @param requestId 本次请求的锁id
	 * @param expireTime 请求锁的持续时间
	 * @param failfast 是否请求锁快速失败,当快速失败时expireTime无效,请求一次失败就会返回false而不会去重复请求锁
	 * @return
	 */
	public  boolean tryLock(String lockKey, String requestId, int expireTime,boolean failfast);
	
	public  boolean releaseLock( String lockKey, String requestId, int expireTime);
	
}
