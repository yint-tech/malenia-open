package cn.iinti.malenia2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 资产文件，为用户上传到平台，然后在MITM脚本中引用的资源
 * </p>
 *
 * @author yint
 * @since 2024-08-12
 */
@Getter
@Setter
@Schema(name = "Asset", description = "资产文件，为用户上传到平台，然后在MITM脚本中引用的资源")
public class Asset implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "自增主建")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "文件名称（带路径）")
    private String path;

    @Schema(description = "文件大小，展示的时候使用")
    private Long fileSize;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "所属用户")
    private String user;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public static final String ID = "id";

    public static final String PATH = "path";

    public static final String FILE_SIZE = "file_size";

    public static final String CREATE_TIME = "create_time";

    public static final String USER = "user";

    public static final String UPDATE_TIME = "update_time";

    public String buildStoragePath() {
        return buildStoragePath(user, path);
    }

    public static String buildStoragePath(String user, String path) {
        String viewPath;
        if (path.startsWith("/")) {
            viewPath = user + path;
        } else {
            viewPath = user + "/" + path;
        }
        return "mitm-assets/" + viewPath;
    }
}
