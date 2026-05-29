package com.shengyi.reimbursementsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CityDataInitializer implements CommandLineRunner {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CITY_LIST_KEY = "fk:reim:city:list";

    @Override
    public void run(String... args) {
        try {
            Map<String, Object> cityData = new HashMap<>();
            
            cityData.put("10119", createCityInfo("10119", "北京", "1"));
            cityData.put("10621", createCityInfo("10621", "上海", "1"));
            cityData.put("10458", createCityInfo("10458", "武汉", "2"));
            cityData.put("10216", createCityInfo("10216", "杭州", "2"));
            cityData.put("10455", createCityInfo("10455", "荆州", "3"));

            redisTemplate.opsForHash().putAll(CITY_LIST_KEY, cityData);
            
            log.info("城市数据初始化完成，共加载 {} 个城市到 Redis: {}", cityData.size(), CITY_LIST_KEY);
            log.info("城市列表: 北京(一线), 上海(一线), 武汉(二线), 杭州(二线), 荆州(三线)");
        } catch (Exception e) {
            log.error("城市数据初始化失败", e);
        }
    }

    private Map<String, Object> createCityInfo(String cityNo, String cityName, String cityType) {
        Map<String, Object> cityInfo = new HashMap<>();
        cityInfo.put("cityNo", cityNo);
        cityInfo.put("cityName", cityName);
        cityInfo.put("csfllx", Integer.parseInt(cityType));
        return cityInfo;
    }
}