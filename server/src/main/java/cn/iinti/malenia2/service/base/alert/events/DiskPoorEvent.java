package cn.iinti.malenia2.service.base.alert.events;

public record DiskPoorEvent(
        long totalSpace,
        long freeSpace,
        String serverId
) {
}
