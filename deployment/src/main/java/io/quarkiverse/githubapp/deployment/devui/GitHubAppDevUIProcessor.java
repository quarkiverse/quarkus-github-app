package io.quarkiverse.githubapp.deployment.devui;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.ExternalPageBuilder;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * Dev UI card for displaying important details such as the GitHub App Replay UI.
 */
public class GitHubAppDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createDevCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer,
            HttpRootPathBuildItem httpRootPathBuildItem,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig,
            LaunchModeBuildItem launchModeBuildItem) {
        final CardPageBuildItem card = new CardPageBuildItem();

        String uiPath = httpRootPathBuildItem.resolvePath("replay");

        final ExternalPageBuilder versionPage = Page.externalPageBuilder("Replay UI")
                .icon("font-awesome-solid:play")
                .url(uiPath, uiPath)
                .doNotEmbed();
        card.addPage(versionPage);

        card.setCustomCard("qwc-github-app-card.js");

        cardPageBuildItemBuildProducer.produce(card);
    }
}
