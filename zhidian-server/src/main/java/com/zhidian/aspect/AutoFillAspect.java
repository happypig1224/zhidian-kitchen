package com.zhidian.aspect;

import com.zhidian.annotation.AutoFill;
import com.zhidian.constant.AutoFillConstant;
import com.zhidian.context.BaseContext;
import com.zhidian.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* com.zhidian.mapper.*.*(..)) && @annotation(com.zhidian.annotation.AutoFill)")
    public void autoFillPointcut(){}
    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("公共字段填充执行。。。。");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType type = annotation.value();
        Object[] args = joinPoint.getArgs();
        if (args==null || args.length==0) {
            return;
        }
        Object entity = args[0];
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        if (type== OperationType.INSERT) {
            try{
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME
                        , LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setCreateUser.invoke(entity, currentId);
                setCreateTime.invoke(entity, now);
                setUpdateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else if(type==OperationType.UPDATE){
            try{
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            return;
        }
    }
}
