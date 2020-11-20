package io.quarkiverse.githubapp.runtime.github;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;

public final class PayloadHelper {

    public static GHRepository getRepository(GHEventPayload eventPayload) {
        if (eventPayload instanceof GHEventPayload.CheckRun) {
            return ((GHEventPayload.CheckRun) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.CheckSuite) {
            return ((GHEventPayload.CheckSuite) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.PullRequest) {
            return ((GHEventPayload.PullRequest) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.PullRequestReview) {
            return ((GHEventPayload.PullRequestReview) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.PullRequestReviewComment) {
            return ((GHEventPayload.PullRequestReviewComment) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Issue) {
            return ((GHEventPayload.Issue) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.IssueComment) {
            return ((GHEventPayload.IssueComment) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.CommitComment) {
            return ((GHEventPayload.CommitComment) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Create) {
            return ((GHEventPayload.Create) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Delete) {
            return ((GHEventPayload.Delete) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Deployment) {
            return ((GHEventPayload.Deployment) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.DeploymentStatus) {
            return ((GHEventPayload.DeploymentStatus) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Fork) {
            return ((GHEventPayload.Fork) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Ping) {
            return ((GHEventPayload.Ping) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Public) {
            return ((GHEventPayload.Public) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Push) {
            return ((GHEventPayload.Push) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Release) {
            return ((GHEventPayload.Release) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Repository) {
            return ((GHEventPayload.Repository) eventPayload).getRepository();
        }
        if (eventPayload instanceof GHEventPayload.Status) {
            return ((GHEventPayload.Status) eventPayload).getRepository();
        }

        throw new IllegalStateException("Unable to extract repository information from payload type: "
                + eventPayload.getClass().getName() + ". This is needed for config files.");
    }

    private PayloadHelper() {
    }
}
