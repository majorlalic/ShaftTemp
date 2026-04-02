package com.example.demo.dao;

import com.example.demo.entity.DeviceEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeviceRepository {

    @Select("select * from ODS_DWEQ_DM_DEVICE_D where iot_code = #{iotCode} and (deleted is null or deleted = 0)")
    Optional<DeviceEntity> findActiveByIotCode(String iotCode);

    @Select("select * from ODS_DWEQ_DM_DEVICE_D where id = #{id} and (deleted is null or deleted = 0)")
    Optional<DeviceEntity> findActiveById(Long id);

    @Select("select * from ODS_DWEQ_DM_DEVICE_D where deleted is null or deleted = 0")
    List<DeviceEntity> findAllActive();

    @Select("select * from ODS_DWEQ_DM_DEVICE_D where asset_status = #{assetStatus} and (deleted is null or deleted = 0)")
    List<DeviceEntity> findAllActiveByAssetStatus(String assetStatus);

    @Update({
        "update ODS_DWEQ_DM_DEVICE_D set",
        "iot_code = #{iotCode},",
        "name = #{name},",
        "device_type = #{deviceType},",
        "model = #{model},",
        "manufacturer = #{manufacturer},",
        "factory_date = #{factoryDate},",
        "run_date = #{runDate},",
        "asset_status = #{assetStatus},",
        "area_id = #{areaId},",
        "org_id = #{orgId},",
        "online_status = #{onlineStatus},",
        "last_report_time = #{lastReportTime},",
        "last_offline_time = #{lastOfflineTime},",
        "deleted = #{deleted},",
        "updated_on = #{updatedOn},",
        "created_on = #{createdOn},",
        "remark = #{remark}",
        "where id = #{id}"
    })
    int updateById(DeviceEntity entity);
}
