package cn.iinti.malenia2.api.ip.resource;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public interface IpAuthBuilder {
    /**
     * 构建ip源鉴权账户
     *
     * @param sessionParam 当前隧道携带账户，框架解析后的参数
     * @see SessionParam
     */
    default void buildAuthUser(Map<String, String> sessionParam, AuthUser.AuthUserBuilder builder) {
    }


    @Getter
    class AuthUser {
        private String userName;
        private String password;

        @Override
        public String toString() {
            return "userName:" + userName + " password:" + password;
        }

        private AuthUser() {

        }

        public boolean hasAuth(){
            return StringUtils.isNoneBlank(userName,password);
        }

        public static AuthUserBuilder builder() {
            return new AuthUser().new AuthUserBuilder();
        }

        public class AuthUserBuilder {

            public AuthUserBuilder userName(String userName) {
                AuthUser.this.userName = userName;
                return this;
            }

            public AuthUserBuilder password(String password) {
                AuthUser.this.password = password;
                return this;
            }

            public AuthUser build() {
                return AuthUser.this;
            }

            public String get_UserName() {
                return AuthUser.this.userName;
            }

            public String get_Password() {
                return AuthUser.this.password;
            }
        }
    }
}
