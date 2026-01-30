package com.lektralabs.thrones.pallbearer.api.model.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FindUserResponseAccess {
    @JsonProperty("manageGroupMembership")
    private boolean manageGroupMembership;
    @JsonProperty("view")
    private boolean view;
    @JsonProperty("mapRoles")
    private boolean mapRoles;
    @JsonProperty("impersonate")
    private boolean impersonate;
    @JsonProperty("manage")
    private boolean manage;

    public FindUserResponseAccess() {
    }

    public FindUserResponseAccess(boolean manageGroupMembership, boolean view, boolean mapRoles, boolean impersonate, boolean manage) {
        this.manageGroupMembership = manageGroupMembership;
        this.view = view;
        this.mapRoles = mapRoles;
        this.impersonate = impersonate;
        this.manage = manage;
    }
}
