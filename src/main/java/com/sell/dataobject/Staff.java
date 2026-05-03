package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;

/** 员工 - PRD 10 */
@Entity
@Table(name = "staff",
        uniqueConstraints = @UniqueConstraint(name = "uk_staff_username", columnNames = "username"))
@Data
@DynamicUpdate
public class Staff {

    @Id
    @GeneratedValue
    private Integer staffId;

    private String username;

    private String password;

    private String name;

    private String phone;

    /** 角色: 0老板 1店长 2收银员 3制作员. */
    private Integer role;

    /** 是否启用. */
    private Boolean enabled = true;

    private Date createTime;

    private Date updateTime;
}
