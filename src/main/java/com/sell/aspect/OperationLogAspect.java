package com.sell.aspect;

import com.sell.dataobject.OperationLog;
import com.sell.repository.OperationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 操作日志切面 - PRD 12.5
 * 拦截 Seller* Controller 的写操作 (save/delete/update/refund/discount/free/recharge/issue/toggle)
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired(required = false)
    private OperationLogRepository repository;

    @Pointcut("execution(public * com.sell.controller.Seller*.save*(..)) || " +
            "execution(public * com.sell.controller.Seller*.delete*(..)) || " +
            "execution(public * com.sell.controller.Seller*.update*(..)) || " +
            "execution(public * com.sell.controller.Seller*.refund*(..)) || " +
            "execution(public * com.sell.controller.Seller*.discount*(..)) || " +
            "execution(public * com.sell.controller.Seller*.free*(..)) || " +
            "execution(public * com.sell.controller.Seller*.recharge*(..)) || " +
            "execution(public * com.sell.controller.Seller*.adjustBalance*(..)) || " +
            "execution(public * com.sell.controller.Seller*.adjustPoints*(..)) || " +
            "execution(public * com.sell.controller.Seller*.issueCoupon*(..)) || " +
            "execution(public * com.sell.controller.Seller*.toggle*(..)) || " +
            "execution(public * com.sell.controller.Seller*.batchCreate*(..)) || " +
            "execution(public * com.sell.controller.Seller*.cancel*(..))")
    public void writeOps() {}

    @AfterReturning("writeOps()")
    public void log(JoinPoint jp) {
        if (repository == null) return;
        try {
            OperationLog log = new OperationLog();
            log.setOperator(currentOperator());
            log.setOperationType(jp.getSignature().getDeclaringType().getSimpleName()
                    + "." + jp.getSignature().getName());
            log.setTarget(extractTarget(jp.getArgs()));
            log.setDetail(brief(jp.getArgs()));
            log.setIp(currentIp());
            log.setCreateTime(new Date());
            repository.save(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响业务
        }
    }

    private String currentOperator() {
        // TODO: 从登录态拿真实操作人. 当前 local profile 默认无登录, 占位
        return "seller";
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            HttpServletRequest req = attrs.getRequest();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "";
        }
    }

    private String extractTarget(Object[] args) {
        if (args == null || args.length == 0) return "";
        Object first = args[0];
        return first == null ? "" : (first.toString().length() > 64 ? first.toString().substring(0, 64) : first.toString());
    }

    private String brief(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object a : args) {
            if (a == null) continue;
            String s = a.toString();
            if (s.length() > 200) s = s.substring(0, 200) + "...";
            sb.append(s).append(" | ");
        }
        return sb.toString();
    }
}
