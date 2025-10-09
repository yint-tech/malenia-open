package cn.iinti.malenia2.utils.net;

public class AuthHolder {
    final String user;
    final char[] pass;

    public AuthHolder(String user, String pass) {
        this.user = user;
        this.pass = pass.toCharArray();
    }
}
