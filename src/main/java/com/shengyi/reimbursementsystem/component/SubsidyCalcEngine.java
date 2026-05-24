package com.shengyi.reimbursementsystem.component;

import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SubsidyCalcEngine {

    private static final BigDecimal MEAL_STANDARD_TIER1 = new BigDecimal("100");
    private static final BigDecimal MEAL_STANDARD_TIER2 = new BigDecimal("80");
    private static final BigDecimal MEAL_STANDARD_TIER3 = new BigDecimal("50");
    private static final BigDecimal TRANSPORT_STANDARD = new BigDecimal("40");
    private static final BigDecimal PHONE_STANDARD = new BigDecimal("40");

    private final ConcurrentHashMap<String, String> cityLevelCache = new ConcurrentHashMap<>();

    public String matchCityLevel(String cityId) {
        String level = cityLevelCache.get(cityId);
        if (level != null) {
            return level;
        }
        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "城市等级信息未找到: " + cityId);
    }

    public void cacheCityLevel(String cityId, String cityLevel) {
        cityLevelCache.put(cityId, cityLevel);
    }

    public BigDecimal calculateMealStandard(String cityLevel) {
        return switch (cityLevel) {
            case "1" -> MEAL_STANDARD_TIER1;
            case "2" -> MEAL_STANDARD_TIER2;
            case "3" -> MEAL_STANDARD_TIER3;
            default -> throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "无效的城市等级: " + cityLevel);
        };
    }

    public BigDecimal calculateTransportStandard() {
        return TRANSPORT_STANDARD;
    }

    public BigDecimal calculatePhoneStandard() {
        return PHONE_STANDARD;
    }

    public BigDecimal calculateStandardAmount(String cityLevel, int days) {
        BigDecimal dailyTotal = calculateMealStandard(cityLevel)
                .add(TRANSPORT_STANDARD)
                .add(PHONE_STANDARD);
        return dailyTotal.multiply(BigDecimal.valueOf(days));
    }
}
