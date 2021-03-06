package com.github.lit.support.mybatis.mapper;

import com.github.lit.support.mybatis.annotation.Condition;
import com.github.lit.support.mybatis.builder.Logic;
import com.github.lit.support.mybatis.builder.PropertyFunction;
import com.github.lit.support.mybatis.builder.SerializedLambdaUtils;
import com.github.lit.support.mybatis.builder.TableMataDate;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author liulu
 * @version : v1.0
 * date : 7/24/18 11:04
 */
public class BaseSqlProvider {


    public <Entity> String insert(Entity entity) {
        Assert.notNull(entity, "entity must not null");
        Class<?> entityClass = entity.getClass();
        TableMataDate mataDate = TableMataDate.forClass(entityClass);
        Map<String, String> fieldColumnMap = mataDate.getFieldColumnMap();

        SQL sql = new SQL();
        sql.INSERT_INTO(mataDate.getTableName());
        for (Map.Entry<String, String> entry : fieldColumnMap.entrySet()) {
            // 忽略主键
            if (Objects.equals(entry.getKey(), mataDate.getPkProperty())) {
                continue;
            }
            PropertyDescriptor ps = BeanUtils.getPropertyDescriptor(entityClass, entry.getKey());
            if (ps == null || ps.getReadMethod() == null) {
                continue;
            }
            Object value = ReflectionUtils.invokeMethod(ps.getReadMethod(), entity);
            if (!StringUtils.isEmpty(value)) {
                sql.VALUES(entry.getValue(), getTokenParam(entry.getKey()));
            }
        }
        return sql.toString();
    }

    public <Entity> String update(Entity entity) {
        Assert.notNull(entity, "entity must not null");
        Class<?> entityClass = entity.getClass();
        TableMataDate mataDate = TableMataDate.forClass(entityClass);
        Map<String, String> fieldColumnMap = mataDate.getFieldColumnMap();

        SQL sql = new SQL();
        sql.UPDATE(mataDate.getTableName());
        for (Map.Entry<String, String> entry : fieldColumnMap.entrySet()) {
            // 忽略主键
            if (Objects.equals(entry.getKey(), mataDate.getPkProperty())) {
                continue;
            }
            PropertyDescriptor ps = BeanUtils.getPropertyDescriptor(entityClass, entry.getKey());
            if (ps == null || ps.getReadMethod() == null) {
                continue;
            }
            Object value = ReflectionUtils.invokeMethod(ps.getReadMethod(), entity);
            if (!StringUtils.isEmpty(value)) {
                sql.SET(getEquals(entry.getValue(), entry.getKey()));
            }
        }

        return sql.WHERE(getEquals(mataDate.getPkColumn(), mataDate.getPkProperty())).toString();
    }

    public String delete(ProviderContext context) {
        Class<?> entityClass = getEntityClass(context);
        TableMataDate mataDate = TableMataDate.forClass(entityClass);

        return new SQL().DELETE_FROM(mataDate.getTableName())
                .WHERE(getEquals(mataDate.getPkColumn(), mataDate.getPkProperty()))
                .toString();
    }

    public String selectById(ProviderContext context) {
        Class<?> entityClass = getEntityClass(context);
        TableMataDate mataDate = TableMataDate.forClass(entityClass);

        return new SQL().SELECT(mataDate.getBaseColumns())
                .FROM(mataDate.getTableName())
                .WHERE(getEquals(mataDate.getPkColumn(), mataDate.getPkProperty()))
                .toString();
    }

    public String selectByProperty(ProviderContext context, Map<String, Object> params) {
        PropertyFunction propertyFunction = (PropertyFunction) params.get("property");
        String property = SerializedLambdaUtils.getProperty(propertyFunction);
        Class<?> entityClass = getEntityClass(context);
        TableMataDate mataDate = TableMataDate.forClass(entityClass);
        String column = mataDate.getFieldColumnMap().get(property);

        return new SQL().SELECT(mataDate.getBaseColumns())
                .FROM(mataDate.getTableName())
                .WHERE(getEquals(column, property))
                .toString();
    }

    public String selectByCondition(ProviderContext context, Object condition) {
        Class<?> entityClass = getEntityClass(context);
        TableMataDate mataDate = TableMataDate.forClass(entityClass);
        Map<String, String> fieldColumnMap = mataDate.getFieldColumnMap();

        SQL sql = new SQL().SELECT(mataDate.getBaseColumns()).FROM(mataDate.getTableName());
        Field[] fields = condition.getClass().getDeclaredFields();
        for (Field field : fields) {
            Condition logicCondition = field.getAnnotation(Condition.class);
            String mappedProperty = logicCondition == null || StringUtils.isEmpty(logicCondition.property()) ? field.getName() : logicCondition.property();
            PropertyDescriptor entityPd = BeanUtils.getPropertyDescriptor(entityClass, mappedProperty);
            if (entityPd == null) {
                continue;
            }
            PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(condition.getClass(), field.getName());
            if (pd == null || pd.getReadMethod() == null) {
                continue;
            }
            String column = fieldColumnMap.get(mappedProperty);
            Object value = ReflectionUtils.invokeMethod(pd.getReadMethod(), condition);
            if (!StringUtils.isEmpty(value)) {
                Logic logic = logicCondition == null ? Logic.EQ : logicCondition.logic();
                if (logic == Logic.IN || logic == Logic.NOT_IN) {
                    if (value instanceof Collection) {
                        sql.WHERE(column + logic.getCode() + inExpression(field.getName(), ((Collection) value).size()));
                    }
                } else if (logic == Logic.NULL || logic == Logic.NOT_NULL) {
                    sql.WHERE(column + logic.getCode());
                } else {
                    sql.WHERE(column + logic.getCode() + getTokenParam(mappedProperty));
                }
            }
        }
        return sql.toString();
    }

    private Class<?> getEntityClass(ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        for (Type parent : mapperType.getGenericInterfaces()) {
            ResolvableType parentType = ResolvableType.forType(parent);
            if (parentType.getRawClass() == BaseMapper.class) {
                return parentType.getGeneric(0).getRawClass();
            }
        }
        return null;
    }

    private String getEquals(String column, String property) {
        return column + " = " + getTokenParam(property);
    }

    private String getTokenParam(String property) {
        return "#{" + property + "}";
    }

    private String inExpression(String property, int size) {
        MessageFormat messageFormat = new MessageFormat("#'{'" + property + "[{0}]}");
        StringBuilder sb = new StringBuilder(" (");
        for (int i = 0; i < size; i++) {
            sb.append(messageFormat.format(new Object[]{i}));
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        return sb.append(")").toString();
    }
}
