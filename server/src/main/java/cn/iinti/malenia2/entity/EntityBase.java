package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EntityBase {
    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    public static final String ID = "id";
}
