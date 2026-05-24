package com.shengyi.reimbursementsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.vo.ReimMainVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReimMainMapper extends BaseMapper<ReimMain> {

    /**
     * 分页查询主单列表
     * @param page 分页对象
     * @param req 查询条件
     * @return 分页结果
     */
    IPage<ReimMainVO> queryPageList(Page<ReimMainVO> page, @Param("req") ReimMainVO req);
}
