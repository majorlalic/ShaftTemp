package com.csg.dgri.szsiom.sysmanage.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 描述表作用
 * @author wjq
 */
@Table(name = "ECMS_DEVICE_OFFLINE")
public class EcmsDeviceOfflineVO extends CapBaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 构造方法
     */
    public EcmsDeviceOfflineVO() {
    }
    
}