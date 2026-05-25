package com.shengyi.reimbursementsystem.component;

import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.component.strategy.SubsidyCalcStrategyFactory;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubsidyCalcEngine {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SubsidyCalcStrategyFactory strategyFactory;
    
    private static final String CITY_LIST_KEY = "fk:reim:city:list";
    private static final String CITY_CACHE_PREFIX = "fk:reim:city:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @PostConstruct
    public void init() {
        strategyFactory.init();
    }

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

        SubsidyCalcStrategy strategy = strategyFactory.getStrategy(cityLevel);
        return strategy.calculateAllStandards(days);
    }

    public BigDecimal calculateDayStandardAmount(String cityLevel, String subsidyType) {
        if (cityLevel == null || subsidyType == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        SubsidyCalcStrategy strategy = strategyFactory.getStrategy(cityLevel);
        return strategy.calculateDayStandard(subsidyType);
    }
}