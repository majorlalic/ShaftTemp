package com.csg.dgri.szsiom.sysmanage.model;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * ODS_DWEQ_DM_ORG_D 对应 VO（自动生成）
 */
@Table(name = "ODS_DWEQ_DM_ORG_D")
public class OrgVO extends CapBaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public OrgVO() {
    }
    @Id
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    private String name;
    private String type;

    @Column(name = "path_ids")
    private String pathIds;

    @Column(name = "path_names")
    private String pathNames;

    private Integer sort;
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPathIds() {
        return pathIds;
    }

    public void setPathIds(String pathIds) {
        this.pathIds = pathIds;
    }

    public String getPathNames() {
        return pathNames;
    }

    public void setPathNames(String pathNames) {
        this.pathNames = pathNames;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
