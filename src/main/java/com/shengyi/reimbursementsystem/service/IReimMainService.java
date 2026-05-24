package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.vo.ReimMainVO;

public interface IReimMainService extends IService<ReimMain> {
    IPage<ReimMainVO> queryPageList(ReimPageQueryDTO dto);
    
    void saveReimMain(com.shengyi.reimbursementsystem.dto.ReimSaveDTO dto);
    
    void submitReim(com.shengyi.reimbursementsystem.dto.ReimSubmitDTO dto);
}
