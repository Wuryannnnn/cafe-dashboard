package com.sell.enums;

import lombok.Getter;

/** 员工角色 - PRD 10 */
@Getter
public enum StaffRoleEnum implements CodeEnum {

    BOSS(0, "老板"),
    MANAGER(1, "店长"),
    CASHIER(2, "收银员"),
    MAKER(3, "制作员"),
    ;

    private Integer code;
    private String message;

    StaffRoleEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
