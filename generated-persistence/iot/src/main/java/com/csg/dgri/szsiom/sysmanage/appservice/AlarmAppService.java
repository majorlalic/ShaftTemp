package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractAlarmAppService;
import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;

/**
 * ODS_DWEQ_DM_ALARM_D 业务类（自动生成）
 */
@Service(value = "alarmAppService")
public class AlarmAppService<T extends AlarmVO> extends AbstractAlarmAppService<AlarmVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.AlarmVO.";

    @SuppressWarnings("unchecked")
    public Optional<AlarmVO> findById(Long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        AlarmVO one = (AlarmVO) this.getCapBaseCommonDAO().selectOne(NS + "findById", params);
        return Optional.ofNullable(one);
    }

    public List<AlarmVO> findAll() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAll", null);
    }

    @SuppressWarnings("unchecked")
    public Optional<AlarmVO> findByMergeKey(String mergeKey) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("mergeKey", mergeKey);
        AlarmVO one = (AlarmVO) this.getCapBaseCommonDAO().selectOne(NS + "findByMergeKey", params);
        return Optional.ofNullable(one);
    }

    public int upsertPendingAlarm(AlarmVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "upsertPendingAlarm", entity);
    }

    public int updateById(AlarmVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "updateById", entity);
    }

}
