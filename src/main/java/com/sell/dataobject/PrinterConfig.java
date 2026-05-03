package com.sell.dataobject;

import com.sell.enums.PrintStationEnum;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Date;

/**
 * 打印机配置 - 每台打印机对应一个工位
 */
@Entity
@Data
@DynamicUpdate
public class PrinterConfig {

    @Id
    private String printerId;

    /** 打印机名称, 如"吧台打印机"、"后厨打印机". */
    private String printerName;

    /** 工位: 0吧台 1后厨. */
    private Integer station = PrintStationEnum.BAR.getCode();

    /** 打印机IP地址. */
    private String ipAddress;

    /** 打印机端口, 默认9100(ESC/POS标准端口). */
    private Integer port = 9100;

    /** 是否启用. */
    private Boolean enabled = true;

    private Date createTime;

    private Date updateTime;
}
