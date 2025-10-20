package com.pewee.engine;

import com.pewee.engine.meta.EngineDefinition;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 引擎注册
 * @author pewee
 *
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "obs")
public class EngineRegistor implements InitializingBean,ApplicationContextAware{
	private static final Map<Integer,EngineDefinition> m = new HashMap<>();
	private ApplicationContext applicationContext;
	
	private int defaultType;
	
	public int getDefaultType() {
		return defaultType;
	}

	public void setDefaultType(int defaultType) {
		this.defaultType = defaultType;
	}
	
	public int getType(EngineAccess engine) {
		if (engine == null) {
			throw new ServiceException(CommonRespInfo.UNKNOWN_ENGINE);
		}
		for (Map.Entry<Integer, EngineDefinition> entry : m.entrySet()) {
			if (entry.getValue() != null && entry.getValue().equals(engine)) {
				return entry.getKey();
			}
		}
		throw new ServiceException(CommonRespInfo.UNKNOWN_ENGINE);
	}

	/**
	 * 注册引擎
	 */
	private void doRegist()  throws Exception{
		Map<String, EngineDefinition> map = applicationContext.getBeansOfType(EngineDefinition.class);
		log.info("查找到了" + map.size() + "个:" + map.keySet());
		Iterator<String> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			EngineDefinition v = map.get(key);
			v.init();
			m.put(v.getType(), v);
		}
	}
	
	/**
	 * 获取默认存储引擎
	 * @return
	 */
	public EngineAccess getDefault() {
		return switchTo(getDefaultType());
	}
	
	/**
	 * 切换引擎
	 * @param i
	 * @return
	 */
	public EngineAccess switchTo(int i) {
		if (m.containsKey(i)) {
			return m.get(i);
		}
		throw new ServiceException(CommonRespInfo.UNKNOWN_ENGINE);
	}
	
	

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		doRegist();
	}
	
}
