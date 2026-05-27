package com.shengyi.reimbursementsystem.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.shengyi.reimbursementsystem.common.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;

public class MyDataPermissionHandler implements DataPermissionHandler {

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 只有查询操作才进行拦截
        if (!mappedStatementId.endsWith("queryPageList") && !mappedStatementId.endsWith("selectList") && !mappedStatementId.endsWith("selectPage") && !mappedStatementId.endsWith("selectCount")) {
            return where;
        }

        // 我们这里只对 fk_reim_main 这个表的查询做过滤（在 mapper 对应的 ID 中判断）
        // 更严谨的做法是在解析 AST 树时判断表名。由于我们这里演示核心功能，这里通过 Mapper ID 进行简单过滤
        if (!mappedStatementId.contains("ReimMainMapper")) {
            return where;
        }

        String userId = UserContext.getUserId();
        if (userId == null) {
            return where; // 如果获取不到用户（如系统内部调用），则放行
        }

        // 构建 reimburser_id = '当前用户ID'
        EqualsTo dataPermissionExpr = new EqualsTo();
        dataPermissionExpr.setLeftExpression(new Column("reimburser_id"));
        dataPermissionExpr.setRightExpression(new StringValue(userId));

        // 如果原 SQL 没有 WHERE 条件，直接返回新建的过滤条件
        if (where == null) {
            return dataPermissionExpr;
        }

        // 否则将原条件与数据隔离条件进行 AND 拼接
        return new AndExpression(where, dataPermissionExpr);
    }
}
