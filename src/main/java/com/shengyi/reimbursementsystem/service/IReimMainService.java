package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.dto.ReimSaveDTO;
import com.shengyi.reimbursementsystem.dto.ReimSubmitDTO;
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
     * 【学习指引】定义保存报销单的核心业务接口
     * @param dto 报销单数据传输对象（包含了主单信息，以及行程明细和分摊明细的 List）
     * @return 返回报销单的主键 ID
     */
    String saveReimMain(ReimSaveDTO dto);
    
    /**
     * 提交报销单
     * @param dto 包含报销单ID及乐观锁版本号
     */
    void submitReim(ReimSubmitDTO dto);
    
    /**
     * 更新报销单状态
     * @param id 报销单ID
     * @param status 新状态
     */
    void updateStatus(String id, Integer status);
    
    /**
     * 作废报销单
     * @param id 报销单ID
     */
    void cancelReim(String id);
}
