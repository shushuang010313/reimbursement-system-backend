package com.shengyi.reimbursementsystem.component;

import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubsidyCalcEngine {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CITY_LIST_KEY = "fk:reim:city:list";
    private static final String CITY_CACHE_PREFIX = "fk:reim:city:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    public String matchCityLevel(String cityId) {
        if (cityId == null || cityId.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        String cacheKey = CITY_CACHE_PREFIX + cityId;
        Object cachedLevel = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedLevel != null) {
            return cachedLevel.toString();
        }

        Map<Object, Object> cityData = redisTemplate.opsForHash().entries(CITY_LIST_KEY);
        if (!cityData.containsKey(cityId)) {
            log.warn("城市ID {} 不存在于缓存中", cityId);
            return "3";
        }

        Object cityInfo = cityData.get(cityId);
        String cityLevel = extractCityLevel(cityInfo.toString());
        
        redisTemplate.opsForValue().set(cacheKey, cityLevel, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return cityLevel;
    }

    private String extractCityLevel(String cityInfo) {
        if (cityInfo.contains("\"csfllx\":1")) {
            return "1";
        } else if (cityInfo.contains("\"csfllx\":2")) {
            return "2";
        }
        return "3";
    }

    public Map<String, BigDecimal> calculateStandardAmount(String cityLevel, int days) {
        if (cityLevel == null || days <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        BigDecimal mealStandard;
        BigDecimal transportStandard = new BigDecimal("40");
        BigDecimal phoneStandard = new BigDecimal("40");

        switch (cityLevel) {
            case "1":
                mealStandard = new BigDecimal("100");
                break;
            case "2":
                mealStandard = new BigDecimal("80");
                break;
            case "3":
                mealStandard = new BigDecimal("50");
                break;
            default:
                mealStandard = new BigDecimal("50");
        }

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("meal", mealStandard.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP));
        result.put("transport", transportStandard.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP));
        result.put("phone", phoneStandard.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP));
        
        return result;
    }

    public BigDecimal calculateDayStandardAmount(String cityLevel, String subsidyType) {
        if (cityLevel == null || subsidyType == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        switch (subsidyType) {
            case "meal":
                switch (cityLevel) {
                    case "1": return new BigDecimal("100");
                    case "2": return new BigDecimal("80");
                    case "3": return new BigDecimal("50");
                    default: return new BigDecimal("50");
                }
            case "transport":
            case "phone":
                return new BigDecimal("40");
            default:
                return BigDecimal.ZERO;
        }
    }
}