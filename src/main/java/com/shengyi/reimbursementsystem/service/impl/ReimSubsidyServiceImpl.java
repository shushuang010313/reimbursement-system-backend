package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.component.SubsidyCalcEngine;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.mapper.ReimSubsidyMapper;
import com.shengyi.reimbursementsystem.mapper.ReimTripMapper;
import com.shengyi.reimbursementsystem.service.IReimCalendarService;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReimSubsidyServiceImpl extends ServiceImpl<ReimSubsidyMapper, ReimSubsidy> implements IReimSubsidyService {

    private final ReimTripMapper reimTripMapper;
    private final IReimCalendarService reimCalendarService;
    private final SubsidyCalcEngine subsidyCalcEngine;

    public ReimSubsidyServiceImpl(ReimTripMapper reimTripMapper,
                                   IReimCalendarService reimCalendarService,
                                   SubsidyCalcEngine subsidyCalcEngine) {
        this.reimTripMapper = reimTripMapper;
        this.reimCalendarService = reimCalendarService;
        this.subsidyCalcEngine = subsidyCalcEngine;
    }

    /**
     * 生成补助信息
     * @param tripId 行程ID
     * @param reimId 报销单ID
     * @return 补助信息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateSubsidy(String tripId, String reimId) {
        ReimTrip trip = reimTripMapper.selectById(tripId);
        if (trip == null) {
            return null;
        }

        ReimSubsidy subsidy = new ReimSubsidy();
        subsidy.setReimId(reimId);
        subsidy.setTripId(tripId);
        subsidy.setTravelerId(trip.getTravelerId());
        subsidy.setTravelerName(trip.getTravelerName());
        subsidy.setTripStartDate(trip.getDepartureDate());
        subsidy.setTripEndDate(trip.getArriveDate());
        subsidy.setSubsidyDays(trip.getTripDays());
        subsidy.setSubsidyCityId(trip.getArriveCityId());
        subsidy.setSubsidyCityName(trip.getArriveCityName());

        String cityLevel = trip.getArriveCityLevel();
        Map<String, BigDecimal> standardAmounts = subsidyCalcEngine.calculateStandardAmount(cityLevel, trip.getTripDays());
        
        subsidy.setMealSubsidy(standardAmounts.get("meal"));
        subsidy.setTransportSubsidy(standardAmounts.get("transport"));
        subsidy.setPhoneSubsidy(standardAmounts.get("phone"));
        
        BigDecimal applyAmount = standardAmounts.get("meal")
            .add(standardAmounts.get("transport"))
            .add(standardAmounts.get("phone"));
        subsidy.setApplyAmount(applyAmount);
        subsidy.setSubsidyAmount(applyAmount);

        save(subsidy);
        log.info("生成补助信息成功: subsidyId={}", subsidy.getId());

        reimCalendarService.generateCalendar(tripId, reimId, subsidy.getId());
        return subsidy.getId();
    }

    /**
     * 根据报销单ID查询补助信息
     * @param reimId 报销单ID
     * @return 补助信息列表
     */
    @Override
    public List<ReimSubsidy> getSubsidyByReimId(String reimId) {
        return lambdaQuery()
            .eq(ReimSubsidy::getReimId, reimId)
            .list();
    }

    /**
     * 更新补助金额
     * @param subsidyId 补助信息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubsidyAmount(String subsidyId) {
        ReimSubsidy subsidy = getById(subsidyId);
        if (subsidy == null) {
            return;
        }

        List<ReimCalendar> calendarList = reimCalendarService.getCalendarBySubsidyId(subsidyId);
        
        BigDecimal totalMealAmount = calendarList.stream()
            .map(ReimCalendar::getMealAmount)
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));
        
        BigDecimal totalTransportAmount = calendarList.stream()
            .map(ReimCalendar::getTransportAmount)
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));
        
        BigDecimal totalPhoneAmount = calendarList.stream()
            .map(ReimCalendar::getPhoneAmount)
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));

        subsidy.setMealSubsidy(totalMealAmount.setScale(2, RoundingMode.HALF_UP));
        subsidy.setTransportSubsidy(totalTransportAmount.setScale(2, RoundingMode.HALF_UP));
        subsidy.setPhoneSubsidy(totalPhoneAmount.setScale(2, RoundingMode.HALF_UP));
        
        BigDecimal totalSubsidy = totalMealAmount.add(totalTransportAmount).add(totalPhoneAmount);
        subsidy.setSubsidyAmount(totalSubsidy.setScale(2, RoundingMode.HALF_UP));

        updateById(subsidy);
        log.info("更新补助金额成功: subsidyId={}, totalAmount={}", subsidyId, totalSubsidy);
    }
}