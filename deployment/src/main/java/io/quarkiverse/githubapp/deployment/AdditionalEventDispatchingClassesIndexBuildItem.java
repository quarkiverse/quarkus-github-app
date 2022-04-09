package io.quarkiverse.githubapp.deployment;

import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalEventDispatchingClassesIndexBuildItem extends MultiBuildItem {

    private final IndexView index;

    public AdditionalEventDispatchingClassesIndexBuildItem(IndexView index) {
        this.index = index;
    }

    public IndexView getIndex() {
        return index;
    }
}
