package com.github.lit.jdbc.statement.select;

import com.github.lit.commons.page.Page;
import com.github.lit.jdbc.enums.JoinType;
import com.github.lit.jdbc.statement.where.Condition;

import java.util.List;

/**
 * User : liulu
 * Date : 2017/6/4 16:48
 * version $Id: Select.java, v 0.1 Exp $
 */
public interface Select<T> extends Condition<Select<T>, SelectExpression<T>> {

    /**
     * 查询语句中 要查询的属性(不能和 exclude 同时使用)
     *
     * @param fieldNames 字段名
     * @return Select
     */
    Select<T> include(String... fieldNames);

    /**
     * 查询语句中 要排除的属性
     *
     * @param fieldNames 字段名
     * @return Select
     */
    Select<T> exclude(String... fieldNames);

    /**
     * 多表联合查询要增加的字段
     *
     * @param tableClass 要关联表对应的实体
     * @param fieldNames 实体中的属性
     * @return Select
     */
    Select<T> addField(Class<?> tableClass, String... fieldNames);

    Select<T> function(String funcName);

    Select<T> function(String funcName, String... fieldNames);

    /**
     * Select 语句中添加函数，目前只支持合计函数（Aggregate functions）
     *
     * @param funcName   函数名，如：max，count
     * @param distinct   是否去重
     * @param fieldNames 函数执行的列
     * @return Select
     */
    Select<T> function(String funcName, boolean distinct, String... fieldNames);

    /**
     * 设置字段别名
     *
     * @param alias 别名
     * @return Select
     */
    Select<T> alias(String... alias);

    /**
     * 设置表别名
     *
     * @param alias 别名
     * @return Select
     */
    Select<T> tableAlias(String alias);

    /**
     * 添加join
     *
     * @param tableClass join 表对应的实体
     * @return Select
     */
    JoinExpression<T> join(Class<?> tableClass);

    /**
     * 添加简单join 只是将表名列在 from 后 没有 on 条件
     *
     * @param tableClass join 表对应的实体
     * @return Select
     */
    Select<T> simpleJoin(Class<?> tableClass);

    /**
     * 添加join, 并指定 join 类型
     *
     * @param tableClass join 表对应的实体
     * @param joinType   join类型
     * @return Select
     */
    JoinExpression<T> join(JoinType joinType, Class<?> tableClass);

    SelectExpression<T> and(Class<?> tableClass, String fieldName);

    SelectExpression<T> or(Class<?> tableClass, String fieldName);


    /**
     * 添加 group by 的字段
     *
     * @param fields 字段
     * @return Select
     */
    Select<T> groupBy(String... fields);

    SelectExpression<T> having(String fieldName);

    /**
     * 添加升序排列属性
     *
     * @param fieldNames 字段名
     * @return Select
     */
    Select<T> asc(String... fieldNames);

    /**
     * 添加降序排列属性
     *
     * @param fieldNames 字段名
     * @return Select
     */
    Select<T> desc(String... fieldNames);

    /**
     * 查询count
     *
     * @return 记录数
     */
    int count();

    /**
     * 查询单条记录
     *
     * @return 实体
     */
    T single();

    /**
     * 查询单条记录
     *
     * @param clazz class
     * @param <E>   return type
     * @return 指定类型
     */
    <E> E single(Class<E> clazz);

    /**
     * 查询列表
     *
     * @return 实体列表
     */
    List<T> list();

    /**
     * 查询列表
     *
     * @param clazz class
     * @param <E>   return type
     * @return 指定类型列表
     */
    <E> List<E> list(Class<E> clazz);

    Select<T> page(Page pager);

    Select<T> page(int pageNum, int pageSize);

    Select<T> page(int pageNum, int pageSize, boolean queryCont);


}
