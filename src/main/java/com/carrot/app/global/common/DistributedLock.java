package com.carrot.app.global.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

// 사용자 정의 에노테이션
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * Lock의 이름 (고유 키)
     */
    String key();

    /**
     * Lock의 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * Lock 획득을 위해 대기하는 시간 (default 5s)
     * 이 시간이 지나면 Lock 획득 실패로 간주
     */
    long waitTime() default 5000L;

    /**
     * Lock 임대 시간 (default 3s)
     * 이 시간이 지나면 Lock은 자동으로 해제됨 (Deadlock 방지)
     */
    long leaseTime() default 3000L;
}
