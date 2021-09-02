package com.example.ibeacondemo.api;

import com.hjq.http.config.IRequestApi;

/**
 *    desc   : 获取验证码
 */
public final class GetCodeApi implements IRequestApi {

    @Override
    public String getApi() {
        return "User/SendUserVerifyCode";
    }

    /** 手机号 */
    private String MobileNo;

    public GetCodeApi setMobileNo(String mobileNo) {
        this.MobileNo = mobileNo;
        return this;
    }
}