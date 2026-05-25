package com.shengyi.reimbursementsystem.component;

import java.math.BigDecimal;
import java.util.Map;

public interface SubsidyCalcStrategy {

    /**
     * 计算一级城市的饮食补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    BigDecimal calculateMealStandard(int days);

    /**
     * 计算一级城市的交通补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    BigDecimal calculateTransportStandard(int days);

    /**
     * 计算一级城市的通信补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    BigDecimal calculatePhoneStandard(int days);

    /**
     * 计算一级城市的总补贴标准。
     * @param days 补贴天数
     * @return 计算结果
     */
    Map<String, BigDecimal> calculateAllStandards(int days);

    /**
     * 计算一级城市的每日补贴标准。
     * @param subsidyType 补贴类型
     * @return 计算结果
     */
    BigDecimal calculateDayStandard(String subsidyType);

    /**
     * 获取一级城市的补贴等级。
     * @return 补贴等级
     */
    String getCityLevel();
}