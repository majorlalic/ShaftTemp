package com.example.demo.dao;

import com.example.demo.entity.DeviceEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    @Select({
        "<script>",
        "select count(*)",
        "from ODS_DWEQ_DM_DEVICE_D d",
        "left join ODS_DWEQ_DM_ORG_D o on o.id = d.org_id",
        "where (d.deleted is null or d.deleted = 0)",
        "and (o.deleted is null or o.deleted = 0)",
        "<if test='deviceType != null and deviceType != \"\"'>",
        "and d.device_type = #{deviceType}",
        "</if>",
        "<if test='status != null and status != \"\"'>",
        "and d.asset_status = #{status}",
        "</if>",
        "<if test='orgId != null'>",
        "and (d.org_id = #{orgId} or instr('/' || nvl(o.path_ids, '') || '/', '/' || #{orgId} || '/') &gt; 0)",
        "</if>",
        "<if test='manufacturer != null and manufacturer != \"\"'>",
        "and d.manufacturer = #{manufacturer}",
        "</if>",
        "<if test='model != null and model != \"\"'>",
        "and d.model = #{model}",
        "</if>",
        "</script>"
    })
    Long countAccessListRows(
        @Param("deviceType") String deviceType,
        @Param("status") String status,
        @Param("orgId") Long orgId,
        @Param("manufacturer") String manufacturer,
        @Param("model") String model
    );

    @Select({
        "<script>",
        "select * from (",
        "  select",
        "    d.id, d.iot_code, d.name, d.device_type, d.model, d.manufacturer, d.factory_date, d.run_date,",
        "    d.asset_status, d.area_id, d.org_id, d.online_status, d.last_report_time, d.last_offline_time,",
        "    d.remark, d.created_on, d.updated_on,",
        "    b.monitor_id as monitor_id,",
        "    m.name as monitor_name,",
        "    row_number() over(order by d.updated_on desc nulls last, d.id desc) rn",
        "  from ODS_DWEQ_DM_DEVICE_D d",
        "  left join ODS_DWEQ_DM_ORG_D o on o.id = d.org_id",
        "  left join ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D b on b.id = (",
        "    select max(b1.id)",
        "    from ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D b1",
        "    where b1.device_id = d.id and (b1.deleted is null or b1.deleted = 0)",
        "  )",
        "  left join ODS_DWEQ_DM_MONITOR_D m on m.id = b.monitor_id and (m.deleted is null or m.deleted = 0)",
        "  where (d.deleted is null or d.deleted = 0)",
        "  and (o.deleted is null or o.deleted = 0)",
        "  <if test='deviceType != null and deviceType != \"\"'>",
        "  and d.device_type = #{deviceType}",
        "  </if>",
        "  <if test='status != null and status != \"\"'>",
        "  and d.asset_status = #{status}",
        "  </if>",
        "  <if test='orgId != null'>",
        "  and (d.org_id = #{orgId} or instr('/' || nvl(o.path_ids, '') || '/', '/' || #{orgId} || '/') &gt; 0)",
        "  </if>",
        "  <if test='manufacturer != null and manufacturer != \"\"'>",
        "  and d.manufacturer = #{manufacturer}",
        "  </if>",
        "  <if test='model != null and model != \"\"'>",
        "  and d.model = #{model}",
        "  </if>",
        ") x",
        "where x.rn &gt;= #{startRow} and x.rn &lt;= #{endRow}",
        "order by x.rn",
        "</script>"
    })
    List<Map<String, Object>> findAccessListPage(
        @Param("deviceType") String deviceType,
        @Param("status") String status,
        @Param("orgId") Long orgId,
        @Param("manufacturer") String manufacturer,
        @Param("model") String model,
        @Param("startRow") Integer startRow,
        @Param("endRow") Integer endRow
    );

    @Insert({
        "insert into ODS_DWEQ_DM_DEVICE_D (",
        "id, iot_code, name, device_type, model, manufacturer, factory_date, run_date, asset_status, area_id, org_id, ",
        "online_status, last_report_time, last_offline_time, deleted, created_on, updated_on, remark",
        ") values (",
        "#{id}, #{iotCode}, #{name}, #{deviceType}, #{model}, #{manufacturer}, #{factoryDate}, #{runDate}, #{assetStatus}, #{areaId}, #{orgId}, ",
        "#{onlineStatus}, #{lastReportTime}, #{lastOfflineTime}, #{deleted}, #{createdOn}, #{updatedOn}, #{remark}",
        ")"
    })
    int insert(DeviceEntity entity);

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
