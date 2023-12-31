// package com.jnu.ticketdomain.common.aop.redissonLock;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;
//
// import com.jnu.ticketcommon.exception.BadLockIdentifierException;
// import com.jnu.ticketdomain.domains.coupon.domain.Sector;
// import java.lang.reflect.InvocationTargetException;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mock;
// import org.redisson.api.RedissonClient;
//
// class RedissonLockAopTest {
//    @Mock RedissonClient redissonClient;
//    @Mock CallTransactionFactory callTransactionFactory;
//
//    RedissonLockAop redissonLockAop;
//
//    @BeforeEach
//    public void beforeEach() {
//        redissonLockAop = new RedissonLockAop(redissonClient, callTransactionFactory);
//    }
//
//    @Test
//    public void 커스텀오브젝트에서_클래스타입과_식별자로_키를_가져와야한다()
//            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
//        Sector profileViewDto = Sector.builder().id(1000L).build();
//        String otherParam = "param";
//
//        Object[] testArgs = {profileViewDto, otherParam};
//        String[] parameterNames = {"profileViewDto", "otherParam"};
//        Class<?> paramClassType = profileViewDto.getClass();
//        String dynamicKey =
//                redissonLockAop.createDynamicKeyFromObject(testArgs, paramClassType, "id");
//        assertEquals("1000", dynamicKey);
//    }
//
//    @Test
//    public void 기본오브젝트에서_식별자로_키를_가져와야한다() {
//        Sector profileViewDto = Sector.builder().id(1000L).build();
//        String keyParamName = "다이내믹키가될값";
//
//        Object[] testArgs = {profileViewDto, keyParamName};
//        String[] parameterNames = {"파라미터첫번째", "파라미터두번째"};
//        Class<?> paramClassType = profileViewDto.getClass();
//
//        String dynamicKey =
//                redissonLockAop.createDynamicKeyFromPrimitive(parameterNames, testArgs,
// "파라미터두번째");
//        assertEquals("다이내믹키가될값", dynamicKey);
//    }
//
//    @Test
//    public void 잘못된_인자를_설정하면_오류가_발생해야한다() {
//        Sector profileViewDto = Sector.builder().id(1000L).build();
//        // 키값
//        String keyParamName = "다른값";
//        // 실제 인자로 넘겨질 값들
//        Object[] testArgs = {profileViewDto, keyParamName};
//        // 파라미터 이름
//        String[] parameterNames = {"profileViewDto", "otherParam"};
//        // 클래스 타입
//        Class<?> paramClassType = profileViewDto.getClass();
//
//        assertThrows(
//                BadLockIdentifierException.class,
//                () -> {
//                    redissonLockAop.generateDynamicKey(
//                            "이상한식별자", testArgs, paramClassType, parameterNames);
//                });
//
//        assertThrows(
//                BadLockIdentifierException.class,
//                () -> {
//                    redissonLockAop.generateDynamicKey(
//                            "이상한식별자", testArgs, Object.class, parameterNames);
//                });
//    }
// }
