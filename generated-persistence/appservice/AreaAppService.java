package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractAreaAppService;
import com.csg.dgri.szsiom.sysmanage.model.AreaVO;

/**
 * ODS_DWEQ_DM_AREA_D 业务类（自动生成）
 */
@Service(value = "areaAppService")
public class AreaAppService<T extends AreaVO> extends AbstractAreaAppService<AreaVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.AreaVO.";

    public List<AreaVO> findAllActive() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActive", null);
    }

    public int upsert(AreaVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "upsert", entity);
    }

}
