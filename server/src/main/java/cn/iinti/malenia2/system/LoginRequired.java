package cn.iinti.malenia2.system;


import proguard.annotation.Keep;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Keep
public @interface LoginRequired {
    boolean forAdmin() default false;

    boolean apiToken() default false;

    /**
     * 标记了alert的接口发生调用时，系统发出敏感接口操作事件
     */
    boolean alert() default false;

    boolean skipLogRecord() default false;
}
