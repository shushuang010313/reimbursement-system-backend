package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.vo.ReimMainVO;

/**
 * 报销单主表服务接口
 */
public interface IReimMainService extends IService<ReimMain> {
    /**
     * 查询报销单分页列表
     * @param dto 查询条件
     * @return 分页结果
     */
    IPage<ReimMainVO> queryPageList(ReimPageQueryDTO dto);
    
    /**
     * 保存报销单(包含主表、行程明细、分摊明细)
     * @param dto 报销单数据
     */
    void saveReimMain(com.shengyi.reimbursementsystem.dto.ReimSaveDTO dto);
    
    /**
     * 提交报销单
     * @param dto 包含报销单ID及乐观锁版本号
     */
    void submitReim(com.shengyi.reimbursementsystem.dto.ReimSubmitDTO dto);
}
