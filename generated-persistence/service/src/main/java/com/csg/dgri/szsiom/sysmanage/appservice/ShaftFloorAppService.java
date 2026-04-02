package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractShaftFloorAppService;
import com.csg.dgri.szsiom.sysmanage.model.ShaftFloorVO;

/**
 * ODS_DWEQ_DM_SHAFT_FLOOR_D 业务类（自动生成）
 */
@Service(value = "shaftFloorAppService")
public class ShaftFloorAppService<T extends ShaftFloorVO> extends AbstractShaftFloorAppService<ShaftFloorVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.ShaftFloorVO.";

    @SuppressWarnings("unchecked")
    public Optional<ShaftFloorVO> findActiveById(Long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        ShaftFloorVO one = (ShaftFloorVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveById", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public List<ShaftFloorVO> findAllActiveByMonitorId(Long monitorId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("monitorId", monitorId);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByMonitorId", params);
    }

    @SuppressWarnings("unchecked")
    public List<ShaftFloorVO> findAllActiveByDeviceId(Long deviceId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("deviceId", deviceId);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByDeviceId", params);
    }

}
