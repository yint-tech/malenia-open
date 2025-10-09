package cn.iinti.malenia2.api.ip.resource;

import lombok.Data;

@Data
public class CountStatus {
    private int totalCount;
    private long fistActive;
    private long totalFlow;
}
