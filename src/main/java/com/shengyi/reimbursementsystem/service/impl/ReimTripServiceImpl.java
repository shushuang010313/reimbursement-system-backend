package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import com.shengyi.reimbursementsystem.mapper.ReimTripMapper;
import com.shengyi.reimbursementsystem.service.IReimCalendarService;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import com.shengyi.reimbursementsystem.service.IReimTripService;
import com.shengyi.reimbursementsystem.vo.ReimTripVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReimTripServiceImpl extends ServiceImpl<ReimTripMapper, ReimTrip> implements IReimTripService {

    private final IReimSubsidyService reimSubsidyService;
    private final IReimCalendarService reimCalendarService;
    private final IReimMainService reimMainService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReimTripVO saveTrip(ReimTripDTO dto) {
        if (dto.getArriveDate().isBefore(dto.getDepartureDate())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "到达日期不能早于出发日期");
        }

        checkTripConflict(dto);

        ReimTrip trip = new ReimTrip();
        BeanUtils.copyProperties(dto, trip);
        trip.setTripDays((int) ChronoUnit.DAYS.between(dto.getDepartureDate(), dto.getArriveDate()) + 1);

        boolean isUpdate = dto.getId() != null && !dto.getId().isEmpty();
        if (isUpdate) {
            ReimTrip existing = this.getById(dto.getId());
            if (existing == null) {
                throw new BusinessException(404, "行程不存在");
            }
            cascadeDeleteSubsidyAndCalendar(dto.getId());
            this.updateById(trip);
        } else {
            this.save(trip);
        }

        ReimSubsidy subsidy = reimSubsidyService.createSubsidyFromTrip(trip);
        reimCalendarService.generateCalendar(trip, subsidy.getId());
        reimMainService.updateTotalAmount(trip.getReimId());

        ReimTripVO vo = new ReimTripVO();
        BeanUtils.copyProperties(trip, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrip(String tripId) {
        ReimTrip trip = this.getById(tripId);
        if (trip == null) {
            throw new BusinessException(404, "行程不存在");
        }

        cascadeDeleteSubsidyAndCalendar(tripId);
        this.removeById(tripId);
        reimMainService.updateTotalAmount(trip.getReimId());
    }

    private void checkTripConflict(ReimTripDTO dto) {
        LambdaQueryWrapper<ReimTrip> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReimTrip::getTravelerId, dto.getTravelerId())
                .le(ReimTrip::getDepartureDate, dto.getArriveDate())
                .ge(ReimTrip::getArriveDate, dto.getDepartureDate());
        if (dto.getId() != null && !dto.getId().isEmpty()) {
            wrapper.ne(ReimTrip::getId, dto.getId());
        }
        long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.REIM_003);
        }
    }

    private void cascadeDeleteSubsidyAndCalendar(String tripId) {
        List<ReimSubsidy> subsidies = reimSubsidyService.list(
                new LambdaQueryWrapper<ReimSubsidy>()
                        .eq(ReimSubsidy::getTripId, tripId)
        );
        for (ReimSubsidy subsidy : subsidies) {
            reimCalendarService.remove(
                    new LambdaQueryWrapper<ReimCalendar>()
                            .eq(ReimCalendar::getSubsidyId, subsidy.getId())
            );
        }
        reimSubsidyService.remove(
                new LambdaQueryWrapper<ReimSubsidy>()
                        .eq(ReimSubsidy::getTripId, tripId)
        );
    }
}
