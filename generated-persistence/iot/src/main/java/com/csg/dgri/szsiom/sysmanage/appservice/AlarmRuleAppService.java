package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractAlarmRuleAppService;
import com.csg.dgri.szsiom.sysmanage.model.AlarmRuleVO;

/**
 * ODS_DWEQ_DM_ALARM_RULE_D 业务类（自动生成）
 */
@Service(value = "alarmRuleAppService")
public class AlarmRuleAppService<T extends AlarmRuleVO> extends AbstractAlarmRuleAppService<AlarmRuleVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.AlarmRuleVO.";

    public List<AlarmRuleVO> findAllActive() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActive", null);
    }

    @SuppressWarnings("unchecked")
    public Optional<AlarmRuleVO> findActiveById(Long id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        AlarmRuleVO one = (AlarmRuleVO) this.getCapBaseCommonDAO().selectOne(NS + "findActiveById", params);
        return Optional.ofNullable(one);
    }

    public int insert(AlarmRuleVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "insert", entity);
    }

    public int updateById(AlarmRuleVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "updateById", entity);
    }

}
