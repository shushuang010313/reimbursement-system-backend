package com.shengyi.reimbursementsystem.component.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SecondCityStrategy implements SubsidyCalcStrategy {
    
    private static final BigDecimal MEAL_STANDARD = new BigDecimal("80");
    private static final BigDecimal TRANSPORT_STANDARD = new BigDecimal("40");
    private static final BigDecimal PHONE_STANDARD = new BigDecimal("40");

    /**
     * 计算二级城市的饮食补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    @Override
    public BigDecimal calculateMealStandard(int days) {
        return MEAL_STANDARD.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算二级城市的交通补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    @Override
    public BigDecimal calculateTransportStandard(int days) {
        return TRANSPORT_STANDARD.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算二级城市的通信补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    @Override
    public BigDecimal calculatePhoneStandard(int days) {
        return PHONE_STANDARD.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算二级城市的总补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    @Override
    public Map<String, BigDecimal> calculateAllStandards(int days) {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("meal", calculateMealStandard(days));
        result.put("transport", calculateTransportStandard(days));
        result.put("phone", calculatePhoneStandard(days));
        return result;
    }

    /**
     * 计算二级城市的每日补贴标准。
     * @param subsidyType 补贴类型
     * @return 计算结果
     */
    @Override
    public BigDecimal calculateDayStandard(String subsidyType) {
        switch (subsidyType) {
            case "meal":
                return MEAL_STANDARD;
            case "transport":
                return TRANSPORT_STANDARD;
            case "phone":
                return PHONE_STANDARD;
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 获取二级城市的补贴等级。
     * @return 补贴等级
     */
    @Override
    public String getCityLevel() {
        return "2";
    }
}