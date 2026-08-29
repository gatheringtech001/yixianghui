package com.ruoyi.system.domain.talent;

public class TalentCenterApiException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    private final int httpStatus;

    public TalentCenterApiException(int httpStatus, String message)
    {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus()
    {
        return httpStatus;
    }
}
