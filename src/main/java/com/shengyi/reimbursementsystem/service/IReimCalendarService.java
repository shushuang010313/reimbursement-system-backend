package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimCalendarDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.vo.ReimCalendarVO;

import java.util.List;

public interface IReimCalendarService extends IService<ReimCalendar> {

    void generateCalendar(ReimTrip trip, String subsidyId);

    List<ReimCalendarVO> getCalendarBySubsidyId(String subsidyId);

    void updateCalendarStatus(String subsidyId, List<ReimCalendarDTO> calendarList);
}
