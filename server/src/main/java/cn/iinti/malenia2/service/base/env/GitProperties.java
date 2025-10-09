package cn.iinti.malenia2.service.base.env;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class GitProperties {
    private static final String CONFIG_FILE = "git.properties";
    private static final Properties properties = new Properties();

    static {
        load();
    }

    public static final IT GIT_ID = new IT("git.commit.id", "");
    public static final IT GIT_TIME = new IT("git.commit.time", "");
    public static final IT GIT_USER_EMAIL = new IT("git.commit.user.email", "iinti@iinti.cn");
    public static final IT GIT_BRANCH = new IT("git.branch", "main");


    public static class IT {
        public final String key;
        public final String value;

        public IT(String key, String value) {
            this.key = key;
            this.value = StringUtils.defaultString(properties.getProperty(key), value);
        }
    }

    private static void load() {
        InputStream stream = GitProperties.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (stream == null) {
            return;
        }
        try (stream) {
            properties.load(stream);
        } catch (IOException e) {
            // just skip
            log.error("load git properties error", e);
        }
    }
}
