package io.quarkiverse.githubapp.command.airline.runtime;

import org.kohsuke.github.GHPermissionType;

public class CommandPermissionConfig {

    private final GHPermissionType permission;

    public CommandPermissionConfig(GHPermissionType permission) {
        this.permission = permission;
    }

    public GHPermissionType getPermission() {
        return permission;
    }
}
