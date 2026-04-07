package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractDeviceAppService;
import com.csg.dgri.szsiom.sysmanage.model.DeviceVO;

/**
 * ODS_DWEQ_DM_DEVICE_D 业务类（自动生成）
 */
@Service(value = "deviceAppService")
public class DeviceAppService<T extends DeviceVO> extends AbstractDeviceAppService<DeviceVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.DeviceVO.";

    @SuppressWarnings("unchecked")
    public Optional<DeviceVO> findActiveByIotCode(String iotCode) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("iotCode", iotCode);
        DeviceVO one = (DeviceVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveByIotCode", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public Optional<DeviceVO> findActiveById(Long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        DeviceVO one = (DeviceVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveById", params);
        return Optional.ofNullable(one);
    }

    public List<DeviceVO> findAllActive() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActive", null);
    }

    @SuppressWarnings("unchecked")
    public List<DeviceVO> findAllActiveByAssetStatus(String assetStatus) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("assetStatus", assetStatus);
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActiveByAssetStatus", params);
    }

    public int updateById(DeviceVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "updateById", entity);
    }

}
