package com.pewee.engine.local;

import com.pewee.engine.FileContext;
import com.pewee.engine.EngineInfoEnum;
import com.pewee.engine.meta.EngineDefinition;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
/**
 * 本地存储
 * @author pewee
 *
 */
@Component
@Slf4j
@RestController
public class LocalEngine extends EngineDefinition{
	
	
	private static final String DIR = "obs/tmp";

	@Override
	public String save(FileContext context) {
		File file = new File(DIR + "/" + getFileToken(context));
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
		} catch (FileNotFoundException e1) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e1);
		}
		try {
			IOUtils.write(context.getContent(), outputStream);
		} catch (IOException e) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		} finally {
			try {
				IOUtils.close(outputStream);
			} catch (IOException e) {
				throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
			}
		}
		return getFileToken(context);
	}
	
	@Override
	public int deleteFile(com.pewee.bean.File flie) {
		File file = new File(DIR + "/" + flie.getToken());
		if (file.exists()) {
			file.delete();
			return 1;
		}
		return 0;
	}

	@Override
	public int deleteFiles(List<com.pewee.bean.File> files) {
		int i = 0;
		for (int x = 0; x < files.size() ; x++) {
			i += deleteFile(files.get(x));
		}
		return i;
	}

	@Override
	public void load() {
		File file = new File(DIR);
		if (! file.exists()) {
			file.mkdirs();
		}
		log.info("本地存储-已加载资源");
	}

	@Override
	protected void loginAndAutoRefreshSession() {
		log.info("本地存储-已登录");
	}

	@Override
	public EngineInfoEnum getDefinition() {
		return EngineInfoEnum.LOCAL;
	}

	@Override
	public List<InputStream> doGetStream(List<com.pewee.bean.File> fs) {
		List<InputStream> list = new ArrayList<>();
		for (com.pewee.bean.File f : fs) {
			File file = new File(DIR + "/" + f.getToken());
			FileInputStream inputStream;
			try {
				inputStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				log.error("localengine获取多文件流- 失败 - 需要关闭前面的流!!共需关闭:" + list.size() + "个流",e);
				if (!list.isEmpty()) {
					for (InputStream i : list) {
						try {
							IOUtils.closeQuietly(i);
						} catch (Exception e1) {
							log.error("localengine获取多文件流- 失败 - 关闭前面的流时失败!!",e1);
						}
					}
				}
				throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
			} finally {
			}
			list.add(inputStream);
		}
		return list;
	}

}
