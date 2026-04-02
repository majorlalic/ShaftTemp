package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractEventAppService;
import com.csg.dgri.szsiom.sysmanage.model.EventVO;

/**
 * ODS_DWEQ_DM_EVENT_D 业务类（自动生成）
 */
@Service(value = "eventAppService")
public class EventAppService<T extends EventVO> extends AbstractEventAppService<EventVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.EventVO.";

    public List<EventVO> findAll() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAll", null);
    }

    @SuppressWarnings("unchecked")
    public Optional<EventVO> findById(Long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        EventVO one = (EventVO) this.getCapBaseCommonDAO().selectOne(NS + "findById", params);
        return Optional.ofNullable(one);
    }

    @SuppressWarnings("unchecked")
    public List<EventVO> findByAlarmIdOrderByEventTimeDesc(Long alarmId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("alarmId", alarmId);
        return this.getCapBaseCommonDAO().queryList(NS + "findByAlarmIdOrderByEventTimeDesc", params);
    }

    public int insert(EventVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "insert", entity);
    }

}
