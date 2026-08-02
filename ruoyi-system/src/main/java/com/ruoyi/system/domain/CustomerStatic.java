package com.ruoyi.system.domain;

import java.io.Serializable;

public class CustomerStatic implements Serializable {
    private int dataCount;
    private String groupColOne;
    private String groupColTwo;

    public int getDataCount() {
        return dataCount;
    }

    public void setDataCount(int dataCount) {
        this.dataCount = dataCount;
    }

    public String getGroupColOne() {
        return groupColOne;
    }

    public void setGroupColOne(String groupColOne) {
        this.groupColOne = groupColOne;
    }

    public String getGroupColTwo() {
        return groupColTwo;
    }

    public void setGroupColTwo(String groupColTwo) {
        this.groupColTwo = groupColTwo;
    }
}
