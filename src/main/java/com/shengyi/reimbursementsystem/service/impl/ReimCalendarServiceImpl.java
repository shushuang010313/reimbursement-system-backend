package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.component.SubsidyCalcEngine;
import com.shengyi.reimbursementsystem.dto.ReimCalendarDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.mapper.ReimCalendarMapper;
import com.shengyi.reimbursementsystem.mapper.ReimTripMapper;
import com.shengyi.reimbursementsystem.service.IReimCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReimCalendarServiceImpl extends ServiceImpl<ReimCalendarMapper, ReimCalendar> implements IReimCalendarService {

    private final ReimTripMapper reimTripMapper;
    private final SubsidyCalcEngine subsidyCalcEngine;
    private final ExecutorService calendarExecutor = Executors.newFixedThreadPool(4);

    public ReimCalendarServiceImpl(ReimTripMapper reimTripMapper,
                                    SubsidyCalcEngine subsidyCalcEngine) {
        this.reimTripMapper = reimTripMapper;
        this.subsidyCalcEngine = subsidyCalcEngine;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateCalendar(String tripId, String reimId, String subsidyId) {
        ReimTrip trip = reimTripMapper.selectById(tripId);
        if (trip == null) {
            return;
        }

        LocalDate startDate = trip.getDepartureDate();
        LocalDate endDate = trip.getArriveDate();
        int tripDays = trip.getTripDays();

        List<LocalDate> dateList = new ArrayList<>();
        for (int i = 0; i < tripDays; i++) {
            dateList.add(startDate.plusDays(i));
        }

        List<CompletableFuture<ReimCalendar>> futures = dateList.stream()
            .map(date -> CompletableFuture.supplyAsync(() -> createCalendarRecord(date, trip, reimId, subsidyId), calendarExecutor))
            .collect(Collectors.toList());

        List<ReimCalendar> calendarList = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        saveBatch(calendarList);
        log.info("批量生成补助日历成功: tripId={}, count={}", tripId, calendarList.size());
    }

    private ReimCalendar createCalendarRecord(LocalDate date, ReimTrip trip, String reimId, String subsidyId) {
        ReimCalendar calendar = new ReimCalendar();
        calendar.setReimId(reimId);
        calendar.setSubsidyId(subsidyId);
        calendar.setTripDate(date);
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String weekDay = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE);
        calendar.setWeekDay(weekDay);
        
        calendar.setSubsidyCityId(trip.getArriveCityId());
        calendar.setSubsidyCityName(trip.getArriveCityName());

        String cityLevel = trip.getArriveCityLevel();
        BigDecimal mealStandard = subsidyCalcEngine.calculateDayStandardAmount(cityLevel, "meal");
        BigDecimal transportStandard = subsidyCalcEngine.calculateDayStandardAmount(cityLevel, "transport");
        BigDecimal phoneStandard = subsidyCalcEngine.calculateDayStandardAmount(cityLevel, "phone");

        calendar.setMealChecked(1);
        calendar.setMealStandard(mealStandard);
        calendar.setMealAmount(mealStandard);
        
        calendar.setTransportChecked(1);
        calendar.setTransportStandard(transportStandard);
        calendar.setTransportAmount(transportStandard);
        
        calendar.setPhoneChecked(1);
        calendar.setPhoneStandard(phoneStandard);
        calendar.setPhoneAmount(phoneStandard);

        return calendar;
    }

    @Override
    public List<ReimCalendar> getCalendarBySubsidyId(String subsidyId) {
        return lambdaQuery()
            .eq(ReimCalendar::getSubsidyId, subsidyId)
            .orderByAsc(ReimCalendar::getTripDate)
            .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalendarStatus(List<ReimCalendarDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        for (ReimCalendarDTO dto : dtoList) {
            ReimCalendar calendar = new ReimCalendar();
            calendar.setId(dto.getId());
            
            if (dto.getMealChecked() != null) {
                calendar.setMealChecked(dto.getMealChecked());
                if (dto.getMealChecked() == 0) {
                    calendar.setMealAmount(BigDecimal.ZERO);
                } else if (dto.getMealChecked() == 1) {
                    if (dto.getMealAmount() == null || dto.getMealAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        calendar.setMealAmount(dto.getMealStandard());
                    } else {
                        calendar.setMealAmount(dto.getMealAmount());
                    }
                }
            }
            
            if (dto.getTransportChecked() != null) {
                calendar.setTransportChecked(dto.getTransportChecked());
                if (dto.getTransportChecked() == 0) {
                    calendar.setTransportAmount(BigDecimal.ZERO);
                } else if (dto.getTransportChecked() == 1) {
                    if (dto.getTransportAmount() == null || dto.getTransportAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        calendar.setTransportAmount(dto.getTransportStandard());
                    } else {
                        calendar.setTransportAmount(dto.getTransportAmount());
                    }
                }
            }
            
            if (dto.getPhoneChecked() != null) {
                calendar.setPhoneChecked(dto.getPhoneChecked());
                if (dto.getPhoneChecked() == 0) {
                    calendar.setPhoneAmount(BigDecimal.ZERO);
                } else if (dto.getPhoneChecked() == 1) {
                    if (dto.getPhoneAmount() == null || dto.getPhoneAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        calendar.setPhoneAmount(dto.getPhoneStandard());
                    } else {
                        calendar.setPhoneAmount(dto.getPhoneAmount());
                    }
                }
            }

            updateById(calendar);
            log.info("更新补助日历状态成功: calendarId={}", dto.getId());
        }

        if (!dtoList.isEmpty()) {
            String subsidyId = dtoList.get(0).getSubsidyId();
            updateSubsidyAndMainAmount(subsidyId);
        }
    }

    private void updateSubsidyAndMainAmount(String subsidyId) {
        LambdaQueryWrapper<ReimCalendar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReimCalendar::getSubsidyId, subsidyId);
        List<ReimCalendar> calendarList = list(queryWrapper);

        BigDecimal totalMealAmount = calendarList.stream()
            .map(ReimCalendar::getMealAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalTransportAmount = calendarList.stream()
            .map(ReimCalendar::getTransportAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPhoneAmount = calendarList.stream()
            .map(ReimCalendar::getPhoneAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSubsidy = totalMealAmount.add(totalTransportAmount).add(totalPhoneAmount)
            .setScale(2, RoundingMode.HALF_UP);

        log.info("计算补助总金额成功: subsidyId={}, totalAmount={}", subsidyId, totalSubsidy);
    }
}