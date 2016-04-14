package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.collect.ImmutableList;

import java.net.SocketAddress;

public class NodeStat {

    private final SocketAddress address;
    private Boolean healthy;
    private Boolean usedForRouting;
    private final ImmutableList<String> filters;

    public NodeStat(SocketAddress address, Boolean healthy,Boolean usedForRouting, ImmutableList<String> filters){
        this.address = address;
        this.healthy = healthy;
        this.usedForRouting = usedForRouting;
        this.filters = filters;
    }

    public NodeStat(Node node){
        this.address = node.address();
        this.filters = node.getFilters();
    }

    public Boolean getUsedForRouting() {
        return usedForRouting;
    }

    public void setUsedForRouting(Boolean usedForRouting) {
        this.usedForRouting = usedForRouting;
    }

    public Boolean getHealthy() {
        return healthy;
    }

    public void setHealthy(Boolean healthy) {
        this.healthy = healthy;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public ImmutableList<String> getFilters() {
        return filters;
    }

}
