package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractRawDataAppService;
import com.csg.dgri.szsiom.sysmanage.model.RawDataVO;

/**
 * ODS_DWEQ_DM_RAW_DATA_D 业务类（自动生成）
 */
@Service(value = "rawDataAppService")
public class RawDataAppService<T extends RawDataVO> extends AbstractRawDataAppService<RawDataVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.RawDataVO.";

    public int insert(RawDataVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "insert", entity);
    }

    @SuppressWarnings("unchecked")
    public List<RawDataVO> query(Long monitorId, Long deviceId, Long shaftFloorId, Integer partitionId, java.time.LocalDateTime from, java.time.LocalDateTime to, int limit) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("monitorId", monitorId);
        params.put("deviceId", deviceId);
        params.put("shaftFloorId", shaftFloorId);
        params.put("partitionId", partitionId);
        params.put("from", from);
        params.put("to", to);
        params.put("limit", limit);
        return this.getCapBaseCommonDAO().queryList(NS + "query", params);
    }

}
