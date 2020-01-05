package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;

public class CustomerException extends RuntimeException {
    private ResultCode resultCode;

    public CustomerException(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode(){
        return resultCode;
    }
}
