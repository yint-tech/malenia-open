package cn.iinti.malenia2.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class TuningParam {


    public TuningParam() {

    }

    public TuningParam(String content) {
        items = new ArrayList<>();
        if (StringUtils.isBlank(content)) {
            return;
        }
        JSONArray items = JSONObject.parseObject(content).getJSONArray("items");
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            this.items.add(items.getJSONObject(i).toJavaObject(TuningParamItem.class));
        }
    }

    private List<TuningParamItem> items;

    @Data
    private static class TuningParamItem {

        @Schema(description = "参数名称")
        private String param;

        @Schema(description = "作用描述")
        private String description;

        @Schema(description = "是否可以为空")
        private Boolean nullable;

        @Schema(description = "可选枚举")
        private List<String> enums;

    }

}
