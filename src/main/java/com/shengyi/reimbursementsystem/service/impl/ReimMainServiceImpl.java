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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        // 将 DTO 字段映射到 VO 字段，用于 XML 查询
        req.setReimNo(dto.getReimNo());
        req.setReimbursementTitle(dto.getReimTitle());
        req.setBusinessTripReason(dto.getBusinessTripReason());
        req.setReimCompanyId(dto.getCompanyId());
        req.setReimDepartmentId(dto.getDepartmentId());
        req.setReimburserId(dto.getReimburserId());
        req.setBusinessTypeId(dto.getBusinessTypeId());
        return reimMainMapper.queryPageList(page, req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveReimMain(ReimSaveDTO dto) {
        // 【学习指引】1. 创建实体类并将 DTO 数据拷贝过去
        ReimMain main = new ReimMain();
        BeanUtils.copyProperties(dto, main);

        // 【学习指引】2. 判断是新增还是更新操作：如果前端没有传 ID，说明是新增
        boolean isNew = !StringUtils.hasText(dto.getId());
        if (isNew) {
            // 【学习指引】2.1 新增逻辑：生成规则的报销单号：REIM + 年月日 + 4位序列号
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String redisKey = "reim:seq:" + dateStr;
            // 【学习指引】利用 Redis 的 incr 操作保证单号的原子自增，避免并发产生重复单号
            Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
            if (seq != null) {
                String seqStr = String.format("%04d", seq);
                main.setReimNo("REIM" + dateStr + seqStr);
            }
            // 【学习指引】新建的报销单默认为“草稿”状态
            main.setReimStatus(0); // 0-草稿
            this.save(main);
        } else {
            // 【学习指引】2.2 更新逻辑：先校验旧数据状态
            ReimMain oldMain = this.getById(dto.getId());
            if (oldMain == null) {
                throw new BusinessException(ErrorCodeEnum.REIM_001); // 单据不存在
            }
            // 【学习指引】状态机校验：非草稿状态的报销单不允许被修改
            if (oldMain.getReimStatus() != 0) {
                throw new BusinessException(ErrorCodeEnum.REIM_002); // 状态不合法
            }
            // 【学习指引】乐观锁校验1：更新操作必须带有前端传入的版本号
            if (dto.getVersion() == null) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "更新失败：未传入版本号");
            }
            
            // 将前端传入的版本号赋予当前实体
            main.setVersion(dto.getVersion());
            
            // 【学习指引】乐观锁校验2：执行 updateById 时，MyBatis-Plus 的 OptimisticLockerInnerInterceptor 会自动拦截
            // 并在 SQL 末尾追加: WHERE id = ? AND version = ? 并且自动 SET version = version + 1
            // 如果在此期间有其他人修改了数据，version 发生了改变，那么受影响的行数 updated 将会返回 0 (false)
            boolean updated = this.updateById(main);
            
            // 根据受影响的行数判断乐观锁是否被触发（即并发更新冲突）
            if (!updated) {
                // 如果产生并发冲突，立刻外抛自定义的业务异常，GlobalExceptionHandler 会拦截此异常并返回友好提示给前端
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "数据已被其他人修改，请刷新后重试");
            }
            
            // 【学习指引】2.3 清理旧明细：在更新主单时，简单粗暴地将关联的明细全部逻辑删除，后续再重新插入新明细（即全删全插策略）
            reimSplitService.update(new UpdateWrapper<ReimSplit>().eq("reim_id", dto.getId()).set("del_flag", 1));
            reimTripService.update(new UpdateWrapper<ReimTrip>().eq("reim_id", dto.getId()).set("del_flag", 1));
        }

        String reimId = main.getId();

        // 【学习指引】3. 级联保存明细数据：先校验再批量保存行程明细
        if (dto.getTripList() != null && !dto.getTripList().isEmpty()) {
            for (ReimTripDTO tripDTO : dto.getTripList()) {
                tripDTO.setReimId(reimId);
                reimTripService.validateTripTime(tripDTO);
            }
            // 校验同一批次内是否存在行程时间重叠
            List<ReimTripDTO> tripDTOList = dto.getTripList();
            for (int i = 0; i < tripDTOList.size(); i++) {
                for (int j = i + 1; j < tripDTOList.size(); j++) {
                    ReimTripDTO a = tripDTOList.get(i);
                    ReimTripDTO b = tripDTOList.get(j);
                    if (a.getTravelerId().equals(b.getTravelerId())
                        && !a.getDepartureDate().isAfter(b.getArriveDate())
                        && !b.getDepartureDate().isAfter(a.getArriveDate())) {
                        throw new BusinessException(ErrorCodeEnum.REIM_003);
                    }
                }
            }
            List<ReimTrip> tripList = new ArrayList<>();
            for (ReimTripDTO tripDTO : tripDTOList) {
                ReimTrip trip = new ReimTrip();
                BeanUtils.copyProperties(tripDTO, trip);
                tripList.add(trip);
            }
            reimTripService.saveBatch(tripList); // 批量插入提高性能
        }

        // 【学习指引】4. 级联保存明细数据：批量保存分摊明细
        if (dto.getSplitList() != null && !dto.getSplitList().isEmpty()) {
            List<ReimSplit> splitList = new ArrayList<>();
            for (ReimSplitDTO splitDTO : dto.getSplitList()) {
                ReimSplit split = new ReimSplit();
                BeanUtils.copyProperties(splitDTO, split);
                split.setReimId(reimId); // 绑定主单ID
                splitList.add(split);
            }
            reimSplitService.saveBatch(splitList); // 批量插入提高性能
        }
        
        // 【学习指引】5. 返回最终的主单ID
        return reimId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReim(ReimSubmitDTO dto) {
        String reimId = dto.getId();
        // 【学习指引】1. 构建分布式锁的 Key，粒度精确到具体的报销单 ID
        String lockKey = "fk:reim:lock:submit:" + reimId;
        // 【学习指引】通过 Redisson 获取可重入锁，防止同一单据在集群环境下的并发提交
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            // 【学习指引】尝试加锁，0秒等待（获取不到立刻返回失败），锁超时时间 10 秒
            isLocked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "请勿频繁重复提交");
            }

            // 【学习指引】2. 业务校验：获取最新主单数据并进行合法性拦截
            ReimMain main = this.getById(reimId);
            if (main == null) {
                throw new BusinessException(ErrorCodeEnum.REIM_001);
            }
            
            // 【学习指引】3. 乐观锁校验：比对前端传来的版本号与数据库中的是否一致，避免脏写
            if (!main.getVersion().equals(dto.getVersion())) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "数据已被修改，请刷新后重试");
            }
            
            // 【学习指引】状态机拦截：非草稿状态（0）的单子不允许进行提交操作
            if (main.getReimStatus() != 0) {
                throw new BusinessException(ErrorCodeEnum.REIM_002);
            }

            // 【学习指引】完整性拦截：必须包含行程明细且补助总额已计算
            long tripCount = reimTripService.count(new QueryWrapper<ReimTrip>().eq("reim_id", reimId));
            if (tripCount == 0 || main.getSubsidyTotal() == null) {
                throw new BusinessException(ErrorCodeEnum.REIM_005);
            }

            // 【学习指引】4. 更新自身状态：组装更新实体，将状态流转为“已完成(待推送BPM)”
            ReimMain updateMain = new ReimMain();
            updateMain.setId(reimId);
            updateMain.setVersion(dto.getVersion());
            updateMain.setReimStatus(1); // 1-已完成
            
            // 执行更新，MyBatis-Plus 的乐观锁插件会再次保障更新的原子性
            boolean updated = this.updateById(updateMain);
            if (!updated) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "数据已被修改，提交失败");
            }
            
            // 【学习指引】5. 采用“本地消息表”模式保障分布式事务的最终一致性
            // 第一步：在当前数据库事务中，向本地消息表插入一条待发送的 MQ 消息记录
            String mqMessageId = UUID.randomUUID().toString().replace("-", "");
            FkMqMessage mqMessage = new FkMqMessage();
            mqMessage.setId(mqMessageId);
            mqMessage.setBusinessId(reimId);
            mqMessage.setTopic("reim.submit.topic");
            mqMessage.setMessageContent(reimId); // 简单起见只存ID，如果是复杂对象则转为JSON
            mqMessage.setStatus(0); // 0待发送
            fkMqMessageMapper.insert(mqMessage);

            // 【学习指引】6. 发送 MQ 消息进行异步解耦处理（通知 BPM 审批流）
            try {
                rabbitTemplate.convertAndSend("reim.submit.topic", "", reimId);
                // 发送成功后，更新本地消息表状态为“1-发送成功”
                FkMqMessage successMsg = new FkMqMessage();
                successMsg.setId(mqMessageId);
                successMsg.setStatus(1); // 1发送成功
                fkMqMessageMapper.updateById(successMsg);
            } catch (Exception e) {
                log.error("【主单提交】推送 MQ 消息失败，已记录本地消息表状态为 2，等待定时任务补偿。错误原因: {}", e.getMessage(), e);
                // 如果 MQ 宕机或网络异常导致发送失败，由于我们在本地事务中已经记录了消息状态，
                // 此时只需更新状态为“2-发送失败”，后续会由定时任务(补偿Job)扫描本地消息表并重试
                FkMqMessage failMsg = new FkMqMessage();
                failMsg.setId(mqMessageId);
                failMsg.setStatus(2); // 2发送失败
                fkMqMessageMapper.updateById(failMsg);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        } finally {
            // 【学习指引】7. 安全释放分布式锁（必须确保是当前线程持有的锁）
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