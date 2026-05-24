package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.component.SubsidyCalcEngine;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.mapper.ReimSubsidyMapper;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReimSubsidyServiceImpl extends ServiceImpl<ReimSubsidyMapper, ReimSubsidy> implements IReimSubsidyService {

    private final SubsidyCalcEngine subsidyCalcEngine;

    @Override
    public ReimSubsidy createSubsidyFromTrip(ReimTrip trip) {
        String cityLevel = trip.getArriveCityLevel();
        int days = trip.getTripDays();

        BigDecimal mealStandard = subsidyCalcEngine.calculateMealStandard(cityLevel);
        BigDecimal transportStandard = subsidyCalcEngine.calculateTransportStandard();
        BigDecimal phoneStandard = subsidyCalcEngine.calculatePhoneStandard();

        BigDecimal mealSubsidy = mealStandard.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal transportSubsidy = transportStandard.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal phoneSubsidy = phoneStandard.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal applyAmount = mealSubsidy.add(transportSubsidy).add(phoneSubsidy).setScale(2, RoundingMode.HALF_UP);

        ReimSubsidy subsidy = new ReimSubsidy();
        subsidy.setReimId(trip.getReimId());
        subsidy.setTripId(trip.getId());
        subsidy.setTravelerId(trip.getTravelerId());
        subsidy.setTravelerName(trip.getTravelerName());
        subsidy.setTripStartDate(trip.getDepartureDate());
        subsidy.setTripEndDate(trip.getArriveDate());
        subsidy.setSubsidyDays(days);
        subsidy.setSubsidyCityId(trip.getArriveCityId());
        subsidy.setSubsidyCityName(trip.getArriveCityName());
        subsidy.setApplyAmount(applyAmount);
        subsidy.setSubsidyAmount(applyAmount);
        subsidy.setMealSubsidy(mealSubsidy);
        subsidy.setTransportSubsidy(transportSubsidy);
        subsidy.setPhoneSubsidy(phoneSubsidy);

        this.save(subsidy);
        return subsidy;
    }
}
