package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractAlarmRawAppService;
import com.csg.dgri.szsiom.sysmanage.model.AlarmRawVO;

/**
 * ODS_DWEQ_DM_ALARM_RAW_D 业务类（自动生成）
 */
@Service(value = "alarmRawAppService")
public class AlarmRawAppService<T extends AlarmRawVO> extends AbstractAlarmRawAppService<AlarmRawVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.AlarmRawVO.";

    public int insert(AlarmRawVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "insert", entity);
    }

}
