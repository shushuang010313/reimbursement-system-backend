package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.component.SubsidyCalcEngine;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import com.shengyi.reimbursementsystem.mapper.ReimTripMapper;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import com.shengyi.reimbursementsystem.service.IReimTripService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class ReimTripServiceImpl extends ServiceImpl<ReimTripMapper, ReimTrip> implements IReimTripService {

    private final ApplicationContext applicationContext;
    private final SubsidyCalcEngine subsidyCalcEngine;

    public ReimTripServiceImpl(ApplicationContext applicationContext,
                               SubsidyCalcEngine subsidyCalcEngine) {
        this.applicationContext = applicationContext;
        this.subsidyCalcEngine = subsidyCalcEngine;
    }

    private IReimSubsidyService getReimSubsidyService() {
        return applicationContext.getBean(IReimSubsidyService.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, String> saveTrip(ReimTripDTO tripDTO) {
        validateTripTime(tripDTO);

        ReimTrip reimTrip = new ReimTrip();
        BeanUtils.copyProperties(tripDTO, reimTrip);

        if (tripDTO.getId() != null && !tripDTO.getId().isEmpty()) {
            updateById(reimTrip);
            log.info("更新行程成功: tripId={}", tripDTO.getId());
        } else {
            reimTrip.setArriveCityLevel(subsidyCalcEngine.matchCityLevel(tripDTO.getArriveCityId()));
            int tripDays = (int) ChronoUnit.DAYS.between(tripDTO.getDepartureDate(), tripDTO.getArriveDate()) + 1;
            reimTrip.setTripDays(tripDays);
            save(reimTrip);
            log.info("保存行程成功: tripId={}", reimTrip.getId());
        }

        String subsidyId = getReimSubsidyService().generateSubsidy(reimTrip.getId(), reimTrip.getReimId());
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("tripId", reimTrip.getId());
        result.put("subsidyId", subsidyId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrip(String tripId) {
        if (tripId == null || tripId.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        ReimTrip trip = getById(tripId);
        if (trip == null) {
            throw new BusinessException(ErrorCodeEnum.REIM_001);
        }

        removeById(tripId);
        log.info("删除行程成功: tripId={}", tripId);
    }

    private void validateTripTime(ReimTripDTO tripDTO) {
        if (tripDTO.getDepartureDate().isAfter(tripDTO.getArriveDate())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        if (tripDTO.getDepartureDate().isAfter(LocalDate.now()) || 
            tripDTO.getArriveDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<ReimTrip> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReimTrip::getTravelerId, tripDTO.getTravelerId())
                   .ne(tripDTO.getId() != null, ReimTrip::getId, tripDTO.getId())
                   .and(wrapper -> wrapper
                       .and(w -> w.le(ReimTrip::getDepartureDate, tripDTO.getDepartureDate())
                                   .ge(ReimTrip::getArriveDate, tripDTO.getDepartureDate()))
                       .or(w -> w.le(ReimTrip::getDepartureDate, tripDTO.getArriveDate())
                                   .ge(ReimTrip::getArriveDate, tripDTO.getArriveDate()))
                       .or(w -> w.ge(ReimTrip::getDepartureDate, tripDTO.getDepartureDate())
                                   .le(ReimTrip::getArriveDate, tripDTO.getArriveDate()))
                   );

        Long count = count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.REIM_003);
        }
    }
}