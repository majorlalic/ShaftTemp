package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractMonitorPartitionBindAppService;
import com.csg.dgri.szsiom.sysmanage.model.MonitorPartitionBindVO;

/**
 * ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D 业务类（自动生成）
 */
@Service(value = "monitorPartitionBindAppService")
public class MonitorPartitionBindAppService<T extends MonitorPartitionBindVO> extends AbstractMonitorPartitionBindAppService<MonitorPartitionBindVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.MonitorPartitionBindVO.";

    @SuppressWarnings("unchecked")
    public Optional<MonitorPartitionBindVO> findActiveByPartitionCode(String partitionCode) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("partitionCode", partitionCode);
        MonitorPartitionBindVO one = (MonitorPartitionBindVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveByPartitionCode", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public Optional<MonitorPartitionBindVO> findActiveByDataReference(String dataReference) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dataReference", dataReference);
        MonitorPartitionBindVO one = (MonitorPartitionBindVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveByDataReference", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public Optional<MonitorPartitionBindVO> findActiveByDeviceAndPartitionId(Long deviceId, Integer partitionId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceId", deviceId);
        params.put("partitionId", partitionId);
        MonitorPartitionBindVO one = (MonitorPartitionBindVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveByDeviceAndPartitionId", params);
        return Optional.ofNullable(one);
    }

    public List<MonitorPartitionBindVO> findAllActive() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActive", null);
    }

    @SuppressWarnings("unchecked")
    public List<MonitorPartitionBindVO> findAllActiveByMonitorId(Long monitorId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("monitorId", monitorId);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByMonitorId", params);
    }

    @SuppressWarnings("unchecked")
    public List<MonitorPartitionBindVO> findAllActiveByDeviceId(Long deviceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceId", deviceId);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByDeviceId", params);
    }

}
