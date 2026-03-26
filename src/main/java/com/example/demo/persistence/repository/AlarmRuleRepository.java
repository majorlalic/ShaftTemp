package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmRuleEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlarmRuleRepository {

    @Select("select * from alarm_rule where (deleted is null or deleted = 0) order by updated_on desc")
    List<AlarmRuleEntity> findAllActive();

    @Select("select * from alarm_rule where id = #{id} and (deleted is null or deleted = 0) limit 1")
    Optional<AlarmRuleEntity> findActiveById(Long id);

    @Insert({
        "insert into alarm_rule (",
        "id, rule_name, biz_type, alarm_type, scope_type, scope_id, level, threshold_value, threshold_value2,",
        "duration_seconds, enabled, remark, deleted, created_on, updated_on",
        ") values (",
        "#{id}, #{ruleName}, #{bizType}, #{alarmType}, #{scopeType}, #{scopeId}, #{level}, #{thresholdValue}, #{thresholdValue2},",
        "#{durationSeconds}, #{enabled}, #{remark}, #{deleted}, #{createdOn}, #{updatedOn}",
        ")"
    })
    int insert(AlarmRuleEntity entity);

    @Update({
        "update alarm_rule set",
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
