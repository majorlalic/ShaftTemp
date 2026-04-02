package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractMonitorDeviceBindAppService;
import com.csg.dgri.szsiom.sysmanage.model.MonitorDeviceBindVO;

/**
 * ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D 业务类（自动生成）
 */
@Service(value = "monitorDeviceBindAppService")
public class MonitorDeviceBindAppService<T extends MonitorDeviceBindVO> extends AbstractMonitorDeviceBindAppService<MonitorDeviceBindVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.MonitorDeviceBindVO.";

    @SuppressWarnings("unchecked")
    public Optional<MonitorDeviceBindVO> findActiveByMonitorIdAndDeviceId(Long monitorId, Long deviceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("monitorId", monitorId);
        params.put("deviceId", deviceId);
        MonitorDeviceBindVO one = (MonitorDeviceBindVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveByMonitorIdAndDeviceId", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public List<MonitorDeviceBindVO> findAllActiveByMonitorId(Long monitorId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("monitorId", monitorId);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByMonitorId", params);
    }

    @SuppressWarnings("unchecked")
    public List<MonitorDeviceBindVO> findAllActiveByDeviceId(Long deviceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceId", deviceId);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByDeviceId", params);
    }

    public int insert(MonitorDeviceBindVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "insert", entity);
    }

}
