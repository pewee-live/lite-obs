package com.pewee.util.resp;

public class ServiceException extends RuntimeException implements IResponse{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5933752584125462129L;

	private String retutnCode;
	
	private String returnMsg; 
	
	public ServiceException(IResponse resp){	
		super(resp.getMsg(),new RuntimeException());
        this.retutnCode = resp.getCode();
        this.returnMsg = resp.getMsg();	
    }
	
	public ServiceException(String retutnCode,String returnMsg){	
		super(returnMsg,new RuntimeException());
        this.retutnCode = retutnCode;
        this.returnMsg = returnMsg;	
    }
	
	public ServiceException(IResponse resp,Exception e) {	
		super(e.getMessage(), e);
        this.retutnCode = resp.getCode();
        this.returnMsg = resp.getMsg();	   
    }


	@Override
	public String getCode() {
		return retutnCode;
	}

	@Override
	public String getMsg() {
		return returnMsg;
	}

}
