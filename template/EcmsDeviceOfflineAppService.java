package com.csg.dgri.szsiom.sysmanage.appservice;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.csg.dgri.szsiom.sysmanage.appservice.abs.AbstractEcmsDeviceOfflineAppService;
import com.csg.dgri.szsiom.sysmanage.model.EcmsDeviceOfflineVO;

/**
 * 设备离线记录表业务逻辑处理类
 *
 * @author 陈吉灵
 * @since JDK1.8
 * @version Nov 23, 2023 陈吉灵
 * @param <T> 实体对象
 */
@Service(value = "ecmsDeviceOfflineAppService")
public class EcmsDeviceOfflineAppService<T extends EcmsDeviceOfflineVO>
        extends AbstractEcmsDeviceOfflineAppService<EcmsDeviceOfflineVO> {

    public int selectOfflineListCount(EcmsDeviceOfflineVO vo) {
        return (int) this.getCapBaseCommonDAO().selectOne(
                XmlNameSpace.ECMS_DEVICE_OFFLINE_SPACE + "selectOfflineListCount", vo);
    }

    public List<DeviceOfflineVo> selectOfflineList(EcmsDeviceOfflineVO vo) {
        return this.capBaseCommonDAO.queryList(
                XmlNameSpace.ECMS_DEVICE_OFFLINE_SPACE + "selectOfflineList",
                vo,
                vo.getPageNo(),
                vo.getPageSize());
    }

    public List<EcmsDeviceOfflineVO> pageNoHandleOfflineList(EcmsDeviceOfflineVO vo) {
        return this.capBaseCommonDAO.queryList(
                XmlNameSpace.ECMS_DEVICE_OFFLINE_SPACE + "pageNoHandleOfflineList",
                vo);
    }
}