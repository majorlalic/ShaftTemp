package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeviceRepository {

    @Select("select * from device where iot_code = #{iotCode} and (deleted is null or deleted = 0) limit 1")
    Optional<DeviceEntity> findActiveByIotCode(String iotCode);

    @Select("select * from device where id = #{id} and (deleted is null or deleted = 0) limit 1")
    Optional<DeviceEntity> findActiveById(Long id);

    @Select("select * from device where deleted is null or deleted = 0")
    List<DeviceEntity> findAllActive();

    @Select("select * from device where asset_status = #{assetStatus} and (deleted is null or deleted = 0)")
    List<DeviceEntity> findAllActiveByAssetStatus(String assetStatus);

    @Update({
        "update device set",
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
