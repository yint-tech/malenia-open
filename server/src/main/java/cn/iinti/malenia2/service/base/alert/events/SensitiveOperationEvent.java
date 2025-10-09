package cn.iinti.malenia2.service.base.alert.events;


public record SensitiveOperationEvent(String user,
                                      String api,
                                      String params) {
    public String getMessage() {
        return """
                \
                user: %s
                api: %s
                params: %s""".formatted(user, api, params);
    }
}
