package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;

public class ExceptionUtil {

    public static void throwExc(ResultCode resultCode){
        throw new CustomerException(resultCode);
    }
}
