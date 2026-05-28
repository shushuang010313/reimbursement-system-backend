package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimCalendarDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;

import java.util.List;

public interface IReimCalendarService extends IService<ReimCalendar> {
    /**
     * 生成补助日历
     * @param tripId 补录行程ID
     * @param reimId 补录行程ID
     * @param subsidyId 补助ID
     */
    void generateCalendar(String tripId, String reimId, String subsidyId);

    /**
     * 获取补助日历
     * @param subsidyId 补助ID
     * @return 补助日历列表
     */
    List<ReimCalendar> getCalendarBySubsidyId(String subsidyId);

    /**
     * 更新补助日历状态
     * @param dtoList 补助日历状态DTO列表
     */
    void updateCalendarStatus(List<ReimCalendarDTO> dtoList);
}