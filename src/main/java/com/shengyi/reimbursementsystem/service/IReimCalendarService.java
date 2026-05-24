package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimCalendarDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;

import java.util.List;

public interface IReimCalendarService extends IService<ReimCalendar> {
    void generateCalendar(String tripId, String reimId, String subsidyId);
    
    List<ReimCalendar> getCalendarBySubsidyId(String subsidyId);
    
    void updateCalendarStatus(List<ReimCalendarDTO> dtoList);
}