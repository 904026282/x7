/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.xream.x7.lock;

import io.xream.x7.common.util.ExceptionUtil;
import io.xream.x7.common.util.KeyUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class LockAspect {

    @Pointcut("@annotation(io.xream.x7.lock.Lock))")
    public void cut() {

    }

    @Around("cut() && @annotation(lock) ")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, Lock lock) {

        org.aspectj.lang.Signature signature = proceedingJoinPoint.getSignature();
        MethodSignature ms = ((MethodSignature) signature);

        final String suffix = "~lock";
        final String prefix = signature.toString();
        String condition = lock.condition();
        Object[] args = proceedingJoinPoint.getArgs();

        String key = KeyUtil.makeKey(prefix,suffix,condition,args);
        int interval = lock.interval();
        int timeout = lock.timeout();

        return DistributionLock.by(key).lock(
                interval,
                timeout,
                task -> {
            Class returnType = ms.getReturnType();
            try {
                if (returnType == void.class) {
                    proceedingJoinPoint.proceed();
                    return null;
                } else {
                    return proceedingJoinPoint.proceed();
                }
            } catch (Throwable e) {
                throw new RuntimeException(ExceptionUtil.getMessage(e));
            }
        });

    }
}
