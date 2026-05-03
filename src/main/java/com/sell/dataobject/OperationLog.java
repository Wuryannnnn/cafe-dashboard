package com.sell.dataobject;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Date;

/** 操作日志 - PRD 12.5 */
@Entity
@Table(name = "operation_log",
        indexes = {
                @Index(name = "idx_op_time", columnList = "createTime"),
                @Index(name = "idx_op_type", columnList = "operationType")
        })
@Data
@DynamicUpdate
public class OperationLog {

    @Id
    @GeneratedValue
    private Long logId;

    /** 操作人 (用户名). */
    private String operator;

    /** 操作类型 (如: 菜品.新增 / 订单.退款 / 员工.删除). */
    private String operationType;

    /** 操作目标 (如: 菜品id 或 订单号). */
    private String target;

    /** 操作详情. */
    @Column(length = 1024)
    private String detail;

    /** 请求 IP. */
    private String ip;

    private Date createTime;
}
