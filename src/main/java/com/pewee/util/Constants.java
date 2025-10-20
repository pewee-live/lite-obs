package com.pewee.util;

/**
 * @Description 常量类
 */
public class Constants {
	
	/**
	 * userInfo
	 */
	public static final String USER_INFO = "userInfo";

	
	/**
	 * token
	 */
	public static final String Authorizatiuon = "Authorizatiuon";
	
	/**
	 * 时间
	 */
	public static final String TIMESTAMP = "timestamp";
	
	/**
	 * 签名
	 */
	public static final String SIGN = "sign";
	
	/**
	 * 是否跳过签名验证
	 * 0:否 1:是
	 */
	public static final String IS_SKIP_SIGN = "isSkipSign";
	
	/**
	 * DEV环境
	 */
	 public static final String DEV = "dev";
	 
	 /**
	  * SIT环境
	  */
	public static final String SIT = "sit";
	
	/**
	 * PRD
	 */
	public static final String PRD = "prd";
	
	
	
	/**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

	
	/**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 应用数据库字段：0正常，1无效
     */
    public static final String YES = "0";
    public static final String NO = "1";

    /**
     * 应用前后端字段：0表示未获取响应结果（异常），1表示有响应结果（正常），-1表示未登录或无权限, 2表示告警
     */
    public static final String SUCCESS = "1";
    public static final String FAILED = "0";
    public static final String NOT_LOGIN = "-1";
    public static final String ALARM = "2";

    public static final String ENCODING = "UTF-8";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json;charset=UTF-8";

    public static final String SEPARATOR_SIGN = ",";
    public static final String NULL = "null";

    /**
     * 下一步,默认从1开始
     */
    public static final int CURRENT_PAGE=1;
    /**
     * 分页记录行大小
     */
    public static final int PAGE_SIZE=10;

    /**
     * 时间常量
     */
    public final static String DAY = "day";
    public final static String HOUR = "hour";
    public final static String MIN = "min";
    /**
     * 日期格式化常量
     */
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public final static String DATE_FORMAT_DAY = "yyyyMMdd";
    public final static String DATE_FORMAT_HOUR = "HH";
    public final static String DATE_FORMAT_MIN = "mm";
    public final static String YYYYMMDD = DATE_FORMAT_DAY;
    public final static String YYYYMMDDHH = "yyyyMMddHH";
    public final static String YYYYMMDDHHMM = "yyyyMMddHHmm";
}
