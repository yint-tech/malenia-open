package cn.iinti.malenia2.service.base.alert.events;

public record IpSourceHealthScoreLowEvent(
        String serverId,
        String ipSource,
        double score
) {
}
