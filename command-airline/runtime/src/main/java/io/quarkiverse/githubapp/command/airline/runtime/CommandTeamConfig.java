package io.quarkiverse.githubapp.command.airline.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CommandTeamConfig {

    private final Set<String> teams;

    public CommandTeamConfig(String[] teams) {
        this.teams = teams != null ? new HashSet<>(Arrays.asList(teams)) : Collections.emptySet();
    }

    public Set<String> getTeams() {
        return teams;
    }
}
