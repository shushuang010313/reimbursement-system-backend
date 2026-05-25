package com.shengyi.reimbursementsystem.component.strategy;

import com.shengyi.reimbursementsystem.component.SubsidyCalcStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubsidyCalcStrategyFactory {
    
    private final List<SubsidyCalcStrategy> strategies;
    private Map<String, SubsidyCalcStrategy> strategyMap;

    /**
     * 初始化补贴计算策略工厂，将所有策略加载到策略映射中。
     */
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        SubsidyCalcStrategy::getCityLevel,
                        Function.identity()
                ));
        log.info("补贴计算策略工厂初始化完成，共加载 {} 个策略", strategyMap.size());
    }

    /**
     * 根据城市等级获取对应的补贴计算策略。
     * 如果未找到对应的策略，返回默认三级城市策略。
     * @param cityLevel 城市等级，例如 "1"、"2"、"3"
     * @return 对应的补贴计算策略
     */
    public SubsidyCalcStrategy getStrategy(String cityLevel) {
        if (strategyMap == null) {
            init();
        }
        
        SubsidyCalcStrategy strategy = strategyMap.get(cityLevel);
        if (strategy == null) {
            log.warn("未找到城市等级 {} 对应的策略，使用默认三级城市策略", cityLevel);
            strategy = strategyMap.get("3");
        }
        
        return strategy;
    }
}