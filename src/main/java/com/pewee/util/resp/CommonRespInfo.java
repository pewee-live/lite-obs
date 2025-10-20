package com.pewee.util.resp;


/**
 * 返回
 * @author pewee
 *
 */
public enum CommonRespInfo implements IResponse {
	SYS_ERROR("-100000","系统错误"),
	TOKEN_UN_AUTH("-100001","业务系统未携带token或token无效"),
	NOT_LEGAL_PARAM("-100002","参数错误"),
	ILLEGAL_TOKEN("-100003","凭证无效"),
	USER_NOT_EXITS("-100004","用户不存在"),
	EXPIRE_TOKEN("-100005","登录凭证过期"),
	USER_BANNED("-100006","用户已被禁用"),
	NEED_CAPTCHA("-100007","登录需要验证码"),
	CAPTCHA_EXPIRE("-100008","验证码失效"),
	CAPTCHA_ERROR("-100009","验证码错误次数:"),
	MAX_CAPTCHA_ERROR_TIME("-100010","验证码错误次数超限,请等待%s秒后再尝试!"),
	MAX_IP_ERROR_TIME("-100011","该ip登录错误次数超限,请等待%s秒后再尝试!"),
	USER_NOT_ENABLE("-100012","该用户已禁用"),
	PASSWORD_ERROR("-100013","密码错误次数:"),
	MAX_PASSWORD_ERROR_TIME("-100014","密码错误次数超限,请等待%s秒后再尝试!"),
	SIGN_ERROR("-100015","签名错误"),
	USER_UN_AUTH("-100016","用户未授权此操作"),
	TIME_OUT("-100017","时间戳已过期"),
	ANTI_REPLAY("-100018","该请求为重放攻击,已拒绝"),
	HOST_NOT_ALLOWED("-100019","服务器禁止访问"),
	REQ_LIMITED("-100020","请求被限流"),
	REMOTE_ACCESS_FAIL("-100021","远程访问失败"),
	SYS_CODE_TOKEN_EMPTY("-100022","系统编码或者token未携带"),
	SYS_CODE_ERROR("-100023","系统编码错误"),
	SYS_CODE_KEY_INVALID("-100024","系统编码或者系统秘钥无效"),
	SYS_CODE_TOKEN_INVALID("-100025","系统编码或者token无效"),
	RESOLVE_MESSAGE_ERROR("-100026","错误的远程调用结果解析"),
	LOCK_REQUEST_TIMEOUT("-100027","竞争锁超时,请稍后再试"),

	
	UNKNOWN_ENGINE("-100050","未知的存储类型"),
	ERROR_ENGINE("-100051","存储引擎错误"),
	REPEAT_PART("-100052","重复的文件请求"),
	ERROR_DYNAMIC_CODE("-100053","该链接已经过期失效,请重新查询下载链接!!"),
	TOO_MANY_DYNAMIC_CODE("-100054","该链接下载次数过多,已被禁止,请重新查询下载链接!!"),
	
	UNKNOWN_NAS_ENGINE_PREFERCONFIG("-100100","Nas引擎未配置默认Server,请检查Nas配置后再重启"),
	
	SUCCESS("000000","成功");


	CommonRespInfo(String code,String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	private String code;
	
	private String msg;
	
	
	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMsg() {
		return msg;
	}
	
	
	
}
