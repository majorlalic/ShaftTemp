package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractMonitorAppService;
import com.csg.dgri.szsiom.sysmanage.model.MonitorVO;

/**
 * ODS_DWEQ_DM_MONITOR_D 业务类（自动生成）
 */
@Service(value = "monitorAppService")
public class MonitorAppService<T extends MonitorVO> extends AbstractMonitorAppService<MonitorVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.MonitorVO.";

    @SuppressWarnings("unchecked")
    public Optional<MonitorVO> findActiveByDeviceId(Long deviceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceId", deviceId);
        MonitorVO one = (MonitorVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveByDeviceId", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public Optional<MonitorVO> findActiveById(Long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        MonitorVO one = (MonitorVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveById", params);
        return Optional.ofNullable(one);
    }

    public List<MonitorVO> findAllActive() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActive", null);
    }

}
