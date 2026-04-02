package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractOrgAppService;
import com.csg.dgri.szsiom.sysmanage.model.OrgVO;

/**
 * ODS_DWEQ_DM_ORG_D 业务类（自动生成）
 */
@Service(value = "orgAppService")
public class OrgAppService<T extends OrgVO> extends AbstractOrgAppService<OrgVO> {

    private static final String NS = "com.csg.dgri.szsiom.sysmanage.model.OrgVO.";

    public List<OrgVO> findAllActive() {
        return this.getCapBaseCommonDAO().queryList(NS + "findAllActive", null);
    }

    public int upsert(OrgVO entity) {
        return this.getCapBaseCommonDAO().update(NS + "upsert", entity);
    }

}
