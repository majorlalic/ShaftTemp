package com.example.demo.dao;

import com.example.demo.entity.AlarmRuleEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlarmRuleRepository {

    @Select("select * from ODS_DWEQ_DM_ALARM_RULE_D where (deleted is null or deleted = 0) order by updated_on desc")
    List<AlarmRuleEntity> findAllActive();

    @Select("select * from ODS_DWEQ_DM_ALARM_RULE_D where id = #{id} and (deleted is null or deleted = 0)")
    Optional<AlarmRuleEntity> findActiveById(Long id);

    @Insert({
        "insert into ODS_DWEQ_DM_ALARM_RULE_D (",
        "id, rule_name, biz_type, alarm_type, scope_type, scope_id, level, threshold_value, threshold_value2,",
        "duration_seconds, enabled, remark, deleted, created_on, updated_on",
        ") values (",
        "#{id}, #{ruleName}, #{bizType}, #{alarmType}, #{scopeType}, #{scopeId}, #{level}, #{thresholdValue}, #{thresholdValue2},",
        "#{durationSeconds}, #{enabled}, #{remark}, #{deleted}, #{createdOn}, #{updatedOn}",
        ")"
    })
    int insert(AlarmRuleEntity entity);

    @Update({
        "update ODS_DWEQ_DM_ALARM_RULE_D set",
        "rule_name = #{ruleName},",
        "biz_type = #{bizType},",
        "alarm_type = #{alarmType},",
        "scope_type = #{scopeType},",
        "scope_id = #{scopeId},",
        "level = #{level},",
        "threshold_value = #{thresholdValue},",
        "threshold_value2 = #{thresholdValue2},",
        "duration_seconds = #{durationSeconds},",
        "enabled = #{enabled},",
        "remark = #{remark},",
        "deleted = #{deleted},",
        "created_on = #{createdOn},",
        "updated_on = #{updatedOn}",
        "where id = #{id}"
    })
    int updateById(AlarmRuleEntity entity);
}
