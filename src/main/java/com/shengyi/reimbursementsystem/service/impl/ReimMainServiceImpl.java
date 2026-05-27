package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.dto.ReimSaveDTO;
import com.shengyi.reimbursementsystem.dto.ReimSplitDTO;
import com.shengyi.reimbursementsystem.dto.ReimSubmitDTO;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.FkMqMessage;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.entity.ReimSplit;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import com.shengyi.reimbursementsystem.mapper.FkMqMessageMapper;
import com.shengyi.reimbursementsystem.mapper.ReimMainMapper;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import com.shengyi.reimbursementsystem.service.IReimSplitService;
import com.shengyi.reimbursementsystem.service.IReimTripService;
import com.shengyi.reimbursementsystem.vo.ReimMainVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReimMainServiceImpl extends ServiceImpl<ReimMainMapper, ReimMain> implements IReimMainService {

    private final IReimSplitService reimSplitService;
    private final IReimTripService reimTripService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final ReimMainMapper reimMainMapper;
    private final FkMqMessageMapper fkMqMessageMapper;

    @Override
    public IPage<ReimMainVO> queryPageList(ReimPageQueryDTO dto) {
        Page<ReimMainVO> page = new Page<>(dto.getCurrent(), dto.getSize());
        ReimMainVO req = new ReimMainVO();
        BeanUtils.copyProperties(dto, req);
        // 将 DTO 字段映射到 VO 字段，用于 XML 查询
        req.setReimbursementTitle(dto.getReimTitle());
        req.setReimCompanyId(dto.getCompanyId());
        req.setReimDepartmentId(dto.getDepartmentId());
        
        return reimMainMapper.queryPageList(page, req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveReimMain(ReimSaveDTO dto) {
        ReimMain main = new ReimMain();
        BeanUtils.copyProperties(dto, main);

        boolean isNew = !StringUtils.hasText(dto.getId());
        if (isNew) {
            // 生成报销单号：REIM + 年月日 + 序列号
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String redisKey = "reim:seq:" + dateStr;
            Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
            if (seq != null) {
                String seqStr = String.format("%04d", seq);
                main.setReimNo("REIM" + dateStr + seqStr);
            }
            main.setReimStatus(0); // 0-草稿
            this.save(main);
        } else {
            // 检查乐观锁版本号
            ReimMain oldMain = this.getById(dto.getId());
            if (oldMain == null) {
                throw new BusinessException(ErrorCodeEnum.REIM_001);
            }
            // 非草稿状态不可修改
            if (oldMain.getReimStatus() != 0) {
                throw new BusinessException(ErrorCodeEnum.REIM_002);
            }
            // 检查前端是否传入了乐观锁版本号
            if (dto.getVersion() == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "更新失败：未传入版本号");
            }
            
            // 将前端传入的版本号赋予当前实体
            main.setVersion(dto.getVersion());
            
            // 执行 updateById 时，MyBatis-Plus 的 OptimisticLockerInnerInterceptor 会自动拦截
            // 并在 SQL 末尾追加: WHERE id = ? AND version = ? 并且自动 SET version = version + 1
            // 如果在此期间有其他人修改了数据，version 发生了改变，那么受影响的行数 updated 将会返回 0 (false)
            boolean updated = this.updateById(main);
            
            // 根据受影响的行数判断乐观锁是否被触发（即并发更新冲突）
            if (!updated) {
                // 如果产生并发冲突，立刻外抛自定义的业务异常，GlobalExceptionHandler 会拦截此异常并返回友好提示给前端
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "数据已被其他人修改，请刷新后重试");
            }
            
            // 逻辑删除旧的分摊明细和行程明细
            reimSplitService.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ReimSplit>().eq("reim_id", dto.getId()).set("del_flag", 1));
            reimTripService.update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ReimTrip>().eq("reim_id", dto.getId()).set("del_flag", 1));
        }

        String reimId = main.getId();

        // 批量保存行程明细
        if (dto.getTripList() != null && !dto.getTripList().isEmpty()) {
            List<ReimTrip> tripList = new ArrayList<>();
            for (ReimTripDTO tripDTO : dto.getTripList()) {
                ReimTrip trip = new ReimTrip();
                BeanUtils.copyProperties(tripDTO, trip);
                trip.setReimId(reimId);
                tripList.add(trip);
            }
            reimTripService.saveBatch(tripList);
        }

        // 批量保存分摊明细
        if (dto.getSplitList() != null && !dto.getSplitList().isEmpty()) {
            List<ReimSplit> splitList = new ArrayList<>();
            for (ReimSplitDTO splitDTO : dto.getSplitList()) {
                ReimSplit split = new ReimSplit();
                BeanUtils.copyProperties(splitDTO, split);
                split.setReimId(reimId);
                splitList.add(split);
            }
            reimSplitService.saveBatch(splitList);
        }
        return reimId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReim(ReimSubmitDTO dto) {
        String reimId = dto.getId();
        String lockKey = "fk:reim:lock:submit:" + reimId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "请勿频繁重复提交");
            }

            ReimMain main = this.getById(reimId);
            if (main == null) {
                throw new BusinessException(ErrorCodeEnum.REIM_001);
            }
            
            // 校验乐观锁版本号是否一致
            if (!main.getVersion().equals(dto.getVersion())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "数据已被修改，请刷新后重试");
            }
            
            // 非草稿状态不能提交
            if (main.getReimStatus() != 0) {
                throw new BusinessException(ErrorCodeEnum.REIM_002);
            }

            // 校验合法性（必须有行程明细，且补助总计不能为空）
            long tripCount = reimTripService.count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ReimTrip>().eq("reim_id", reimId));
            if (tripCount == 0 || main.getSubsidyTotal() == null) {
                throw new BusinessException(ErrorCodeEnum.REIM_005);
            }

            // 更新状态为已完成
            ReimMain updateMain = new ReimMain();
            updateMain.setId(reimId);
            updateMain.setVersion(dto.getVersion());
            updateMain.setReimStatus(1); // 1-已完成
            
            boolean updated = this.updateById(updateMain);
            if (!updated) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "数据已被修改，提交失败");
            }
            
            // 记录到本地消息表，在同一个事务内完成
            String mqMessageId = java.util.UUID.randomUUID().toString().replace("-", "");
            FkMqMessage mqMessage = new FkMqMessage();
            mqMessage.setId(mqMessageId);
            mqMessage.setBusinessId(reimId);
            mqMessage.setTopic("reim.submit.topic");
            mqMessage.setMessageContent(reimId); // 简单起见只存ID，如果是复杂对象则转为JSON
            mqMessage.setStatus(0); // 0待发送
            fkMqMessageMapper.insert(mqMessage);

            // 发送 MQ 消息进行异步处理
            try {
                rabbitTemplate.convertAndSend("reim.submit.topic", "", reimId);
                // 成功则更新状态
                FkMqMessage successMsg = new FkMqMessage();
                successMsg.setId(mqMessageId);
                successMsg.setStatus(1); // 1发送成功
                fkMqMessageMapper.updateById(successMsg);
            } catch (Exception e) {
                // 发送失败时由定时任务补偿处理，记录失败状态
                FkMqMessage failMsg = new FkMqMessage();
                failMsg.setId(mqMessageId);
                failMsg.setStatus(2); // 2发送失败
                fkMqMessageMapper.updateById(failMsg);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, Integer status) {
        ReimMain updateMain = new ReimMain();
        updateMain.setId(id);
        updateMain.setReimStatus(status);
        this.updateById(updateMain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReim(String id) {
        ReimMain main = this.getById(id);
        if (main == null) {
            throw new BusinessException(ErrorCodeEnum.REIM_001);
        }
        // 只有已完成（1）状态可以作废
        if (main.getReimStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.REIM_002.getCode(), "只有已完成的报销单可以作废");
        }
        ReimMain updateMain = new ReimMain();
        updateMain.setId(id);
        updateMain.setReimStatus(2); // 2-已作废
        this.updateById(updateMain);
    }
}
