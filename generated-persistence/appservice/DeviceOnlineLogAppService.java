package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractDeviceOnlineLogAppService;
import com.csg.dgri.szsiom.sysmanage.model.DeviceOnlineLogVO;

/**
 * ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D 业务类（自动生成）
 */
@Service(value = "deviceOnlineLogAppService")
public class DeviceOnlineLogAppService<T extends DeviceOnlineLogVO> extends AbstractDeviceOnlineLogAppService<DeviceOnlineLogVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.DeviceOnlineLogVO.";

    public int insert(DeviceOnlineLogVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "insert", entity);
    }

}
