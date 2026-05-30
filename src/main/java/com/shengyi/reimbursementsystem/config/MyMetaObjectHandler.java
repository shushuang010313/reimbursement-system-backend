package com.shengyi.reimbursementsystem.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "creationTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        
        String userId = com.shengyi.reimbursementsystem.common.UserContext.getUserId();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createUserId", String.class, userId);
            this.strictInsertFill(metaObject, "updateUserId", String.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        
        String userId = com.shengyi.reimbursementsystem.common.UserContext.getUserId();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateUserId", String.class, userId);
        }
    }
}
