package org.xyz.api.entity;

import org.xyz.api.contants.CommonConstants;

public class Response<T>
{
    /**
     * code = 0 时返回true
     * code != 0 时返回false
     */
    private boolean success;

    /**
     * 消息状态码
     * 0 表示成功
     * 其它表示失败
     */
    private String code;

    private String msg;

    /**
     * 返回数据
     */
    private T data;

    public Response()
    {
        this.code = CommonConstants.SUCCESS;
        this.success = CommonConstants.TRUE;
        this.msg = CommonConstants.REQUST_SUC;
    }

    public Response(String msg, T data)
    {
        this.code = CommonConstants.SUCCESS;
        this.success = CommonConstants.TRUE;
        this.data = data;
        this.msg = msg;
    }

    public Response(String code, String retInfo)
    {
        this.code = code;
        this.msg = retInfo;
        if (CommonConstants.SUCCESS.equals(code))
        {
            success = CommonConstants.TRUE;
        }
        else
        {
            success = CommonConstants.FALSE;
        }
    }

    public Response(String code, String msg, T data)
    {
        this.code = code;
        this.msg = msg;
        this.data = data;
        if (CommonConstants.SUCCESS.equals(code))
        {
            success = true;
        }
        else
        {
            success = false;
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    public T getData()
    {
        return data;
    }

    public void setData(T data)
    {
        this.data = data;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

}
