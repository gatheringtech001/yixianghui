package com.ruoyi.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 老年教育商品扩展对象 app_goods_education_ext
 */
public class AppGoodsEducationExt extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long extId;

    private Long goodsId;

    private String courseTime;

    private String coursePlace;

    private String teacherName;

    private Integer lessonCount;

    private Integer classSizeMax;

    private Integer classSizeMin;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String signupStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String signupEnd;

    private String materialNote;

    private String consultPhone;

    public Long getExtId()
    {
        return extId;
    }

    public void setExtId(Long extId)
    {
        this.extId = extId;
    }

    public Long getGoodsId()
    {
        return goodsId;
    }

    public void setGoodsId(Long goodsId)
    {
        this.goodsId = goodsId;
    }

    public String getCourseTime()
    {
        return courseTime;
    }

    public void setCourseTime(String courseTime)
    {
        this.courseTime = courseTime;
    }

    public String getCoursePlace()
    {
        return coursePlace;
    }

    public void setCoursePlace(String coursePlace)
    {
        this.coursePlace = coursePlace;
    }

    public String getTeacherName()
    {
        return teacherName;
    }

    public void setTeacherName(String teacherName)
    {
        this.teacherName = teacherName;
    }

    public Integer getLessonCount()
    {
        return lessonCount;
    }

    public void setLessonCount(Integer lessonCount)
    {
        this.lessonCount = lessonCount;
    }

    public Integer getClassSizeMax()
    {
        return classSizeMax;
    }

    public void setClassSizeMax(Integer classSizeMax)
    {
        this.classSizeMax = classSizeMax;
    }

    public Integer getClassSizeMin()
    {
        return classSizeMin;
    }

    public void setClassSizeMin(Integer classSizeMin)
    {
        this.classSizeMin = classSizeMin;
    }

    public String getStartDate()
    {
        return startDate;
    }

    public void setStartDate(String startDate)
    {
        this.startDate = startDate;
    }

    public String getSignupStart()
    {
        return signupStart;
    }

    public void setSignupStart(String signupStart)
    {
        this.signupStart = signupStart;
    }

    public String getSignupEnd()
    {
        return signupEnd;
    }

    public void setSignupEnd(String signupEnd)
    {
        this.signupEnd = signupEnd;
    }

    public String getMaterialNote()
    {
        return materialNote;
    }

    public void setMaterialNote(String materialNote)
    {
        this.materialNote = materialNote;
    }

    public String getConsultPhone()
    {
        return consultPhone;
    }

    public void setConsultPhone(String consultPhone)
    {
        this.consultPhone = consultPhone;
    }
}
