package cn.iinti.malenia2.system;


import cn.iinti.malenia2.entity.SysLog;
import cn.iinti.malenia2.entity.UserInfo;
import cn.iinti.malenia2.mapper.SysLogMapper;
import cn.iinti.malenia2.service.base.alert.EventNotifierService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import proguard.annotation.Keep;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;


@Slf4j
@Aspect
@Component
public class SysLogAspect {

    @Resource
    private SysLogMapper sysLogMapper;

    @Resource
    private EventNotifierService eventNotifierService;

    @Pointcut("@annotation(cn.iinti.malenia2.system.LoginRequired)")
    @Keep
    public void pointcut() {
    }

    @Keep
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object result = point.proceed();
        // 保存日志
        saveLog(point);
        return result;
    }


    private void saveLog(ProceedingJoinPoint point) {
        UserInfo user = AppContext.getUser();
        if (user == null) {
            return;
        }
        if (AppContext.isApiUser()) {
            return;
        }

        LoginRequired loginAnnotation = AppContext.getLoginAnnotation();
        if (loginAnnotation.skipLogRecord()) {
            return;
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getName();
        String methodName = signature.getName();
        String target = className + "#" + methodName;

        Method method = signature.getMethod();
        SysLog sysLog = new SysLog();
        sysLog.setUsername(AppContext.getUser().getUserName());
        sysLog.setOperation(method.getName());
        String params = Arrays.toString(point.getArgs());
        sysLog.setParams(StringUtils.substring(params, 0, 50));

        sysLog.setMethodName(target);
        log.info("record sys log:{}", sysLog);
        sysLogMapper.insert(sysLog);

        // alert
        if (loginAnnotation.alert()) {
            eventNotifierService.notifySensitiveOperation(sysLog.getUsername(),
                    target, sysLog.getParams()
            );
        }
    }


}
