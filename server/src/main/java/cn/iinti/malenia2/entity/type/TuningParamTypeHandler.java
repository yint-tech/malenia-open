package cn.iinti.malenia2.entity.type;

import cn.iinti.malenia2.entity.TuningParam;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(TuningParam.class)
public class TuningParamTypeHandler extends BaseTypeHandler<TuningParam> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TuningParam parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, JSONObject.toJSONString(parameter));
    }

    private TuningParam toJson(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        JSONObject jsonObject = JSONObject.parseObject(jsonText);
        return jsonObject.toJavaObject(TuningParam.class);
    }

    @Override
    public TuningParam getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return toJson(json);
    }

    @Override
    public TuningParam getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return toJson(json);
    }

    @Override
    public TuningParam getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getObject(columnIndex, TuningParam.class);
    }
}
