package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.component.SubsidyCalcEngine;
import com.shengyi.reimbursementsystem.dto.ReimCalendarDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import com.shengyi.reimbursementsystem.mapper.ReimCalendarMapper;
import com.shengyi.reimbursementsystem.service.IReimCalendarService;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import com.shengyi.reimbursementsystem.vo.ReimCalendarVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReimCalendarServiceImpl extends ServiceImpl<ReimCalendarMapper, ReimCalendar> implements IReimCalendarService {

    private final SubsidyCalcEngine subsidyCalcEngine;
    private final IReimSubsidyService reimSubsidyService;
    private final IReimMainService reimMainService;

    private static final int PARALLEL_BATCH_SIZE = 10;

    @Override
    public void generateCalendar(ReimTrip trip, String subsidyId) {
        String cityLevel = trip.getArriveCityLevel();
        int tripDays = trip.getTripDays();

        BigDecimal mealStandard = subsidyCalcEngine.calculateMealStandard(cityLevel);
        BigDecimal transportStandard = subsidyCalcEngine.calculateTransportStandard();
        BigDecimal phoneStandard = subsidyCalcEngine.calculatePhoneStandard();

        List<CompletableFuture<List<ReimCalendar>>> futures = new ArrayList<>();

        for (int i = 0; i < tripDays; i += PARALLEL_BATCH_SIZE) {
            int start = i;
            int end = Math.min(i + PARALLEL_BATCH_SIZE, tripDays);
            futures.add(CompletableFuture.supplyAsync(() -> {
                List<ReimCalendar> batch = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    LocalDate date = trip.getDepartureDate().plusDays(j);
                    ReimCalendar calendar = new ReimCalendar();
                    calendar.setReimId(trip.getReimId());
                    calendar.setSubsidyId(subsidyId);
                    calendar.setTripDate(date);
                    calendar.setWeekDay(convertWeekDay(date));
                    calendar.setSubsidyCityId(trip.getArriveCityId());
                    calendar.setSubsidyCityName(trip.getArriveCityName());
                    calendar.setMealChecked(1);
                    calendar.setMealStandard(mealStandard);
                    calendar.setMealAmount(mealStandard);
                    calendar.setTransportChecked(1);
                    calendar.setTransportStandard(transportStandard);
                    calendar.setTransportAmount(transportStandard);
                    calendar.setPhoneChecked(1);
                    calendar.setPhoneStandard(phoneStandard);
                    calendar.setPhoneAmount(phoneStandard);
                    batch.add(calendar);
                }
                return batch;
            }));
        }

        List<ReimCalendar> allCalendars = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        this.saveBatch(allCalendars);
    }

    @Override
    public List<ReimCalendarVO> getCalendarBySubsidyId(String subsidyId) {
        List<ReimCalendar> calendars = this.list(
                new LambdaQueryWrapper<ReimCalendar>()
                        .eq(ReimCalendar::getSubsidyId, subsidyId)
                        .orderByAsc(ReimCalendar::getTripDate)
        );
        return calendars.stream().map(cal -> {
            ReimCalendarVO vo = new ReimCalendarVO();
            BeanUtils.copyProperties(cal, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalendarStatus(String subsidyId, List<ReimCalendarDTO> calendarList) {
        for (ReimCalendarDTO dto : calendarList) {
            ReimCalendar calendar = this.getById(dto.getId());
            if (calendar == null) {
                continue;
            }

            calendar.setMealChecked(dto.getMealChecked() != null ? dto.getMealChecked() : 0);
            calendar.setTransportChecked(dto.getTransportChecked() != null ? dto.getTransportChecked() : 0);
            calendar.setPhoneChecked(dto.getPhoneChecked() != null ? dto.getPhoneChecked() : 0);

            if (calendar.getMealChecked() == 1) {
                BigDecimal mealAmt = dto.getMealAmount() != null ? dto.getMealAmount() : calendar.getMealStandard();
                if (mealAmt.compareTo(calendar.getMealStandard()) > 0) {
                    throw new BusinessException(400, "餐费补助金额不可超过标准金额");
                }
                calendar.setMealAmount(mealAmt.setScale(2, RoundingMode.HALF_UP));
            } else {
                calendar.setMealAmount(BigDecimal.ZERO);
            }

            if (calendar.getTransportChecked() == 1) {
                BigDecimal transportAmt = dto.getTransportAmount() != null ? dto.getTransportAmount() : calendar.getTransportStandard();
                if (transportAmt.compareTo(calendar.getTransportStandard()) > 0) {
                    throw new BusinessException(400, "交通补助金额不可超过标准金额");
                }
                calendar.setTransportAmount(transportAmt.setScale(2, RoundingMode.HALF_UP));
            } else {
                calendar.setTransportAmount(BigDecimal.ZERO);
            }

            if (calendar.getPhoneChecked() == 1) {
                BigDecimal phoneAmt = dto.getPhoneAmount() != null ? dto.getPhoneAmount() : calendar.getPhoneStandard();
                if (phoneAmt.compareTo(calendar.getPhoneStandard()) > 0) {
                    throw new BusinessException(400, "通讯补助金额不可超过标准金额");
                }
                calendar.setPhoneAmount(phoneAmt.setScale(2, RoundingMode.HALF_UP));
            } else {
                calendar.setPhoneAmount(BigDecimal.ZERO);
            }

            this.updateById(calendar);
        }

        recalculateSubsidyAmount(subsidyId);

        ReimSubsidy subsidy = reimSubsidyService.getById(subsidyId);
        if (subsidy != null) {
            reimMainService.updateTotalAmount(subsidy.getReimId());
        }
    }

    private void recalculateSubsidyAmount(String subsidyId) {
        List<ReimCalendar> calendars = this.list(
                new LambdaQueryWrapper<ReimCalendar>()
                        .eq(ReimCalendar::getSubsidyId, subsidyId)
        );

        BigDecimal applyAmount = BigDecimal.ZERO;
        BigDecimal subsidyAmount = BigDecimal.ZERO;
        BigDecimal mealSubsidy = BigDecimal.ZERO;
        BigDecimal transportSubsidy = BigDecimal.ZERO;
        BigDecimal phoneSubsidy = BigDecimal.ZERO;

        for (ReimCalendar cal : calendars) {
            applyAmount = applyAmount.add(cal.getMealStandard())
                    .add(cal.getTransportStandard())
                    .add(cal.getPhoneStandard());
            subsidyAmount = subsidyAmount.add(cal.getMealAmount())
                    .add(cal.getTransportAmount())
                    .add(cal.getPhoneAmount());
            mealSubsidy = mealSubsidy.add(cal.getMealAmount());
            transportSubsidy = transportSubsidy.add(cal.getTransportAmount());
            phoneSubsidy = phoneSubsidy.add(cal.getPhoneAmount());
        }

        ReimSubsidy subsidy = reimSubsidyService.getById(subsidyId);
        if (subsidy != null) {
            subsidy.setApplyAmount(applyAmount.setScale(2, RoundingMode.HALF_UP));
            subsidy.setSubsidyAmount(subsidyAmount.setScale(2, RoundingMode.HALF_UP));
            subsidy.setMealSubsidy(mealSubsidy.setScale(2, RoundingMode.HALF_UP));
            subsidy.setTransportSubsidy(transportSubsidy.setScale(2, RoundingMode.HALF_UP));
            subsidy.setPhoneSubsidy(phoneSubsidy.setScale(2, RoundingMode.HALF_UP));
            reimSubsidyService.updateById(subsidy);
        }
    }

    private String convertWeekDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }
}
