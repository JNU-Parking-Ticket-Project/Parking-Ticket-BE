package com.jnu.ticketapi.common.aop;

import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketdomain.domains.events.exception.DuplicateSectorNumException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@Slf4j
public class SectorNumCheckAop {

//    @Before("execution(* *.*(.., @com.jnu.ticketapi.common.aop.SectorNumCheck (*), ..))")
//    public void checkSectorNumberDuplication(JoinPoint joinPoint) {
//        Object[] args = joinPoint.getArgs();
//        Set<String> uniqueSectorNumbers = new HashSet<>();
//        for (Object arg : args) {
//            if (arg instanceof List) {
//                List<?> list = (List<?>) arg;
//                for (Object request : list) {
//                    if (request instanceof SectorRegisterRequest) {
//                        checkDuplication((SectorRegisterRequest) request, uniqueSectorNumbers);
//                    }
//                }
//            }
//        }
//    }

    @Before("execution(* *.*(.., @com.jnu.ticketapi.common.aop.SectorNumCheck (*), ..))")
    public void checkSectorNumberDuplication(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        List<SectorRegisterRequest> requests = Arrays.stream(args).filter(List.class::isInstance)
                .flatMap(arg -> ((List<?>) arg).stream())
                .filter(SectorRegisterRequest.class::isInstance)
                .map(SectorRegisterRequest.class::cast)
                .toList();
        List<String> distinctRequest = requests.stream().map(SectorRegisterRequest::sectorNumber).distinct().toList();
        if(requests.size() != distinctRequest.size()){
            throw DuplicateSectorNumException.EXCEPTION;
        }
    }
}
