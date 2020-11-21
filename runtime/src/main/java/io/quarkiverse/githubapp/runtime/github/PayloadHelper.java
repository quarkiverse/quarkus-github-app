package io.quarkiverse.githubapp.runtime.github;

import java.util.Optional;

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

    public static Optional<String> getContext(GHEventPayload eventPayload) {
        if (eventPayload instanceof GHEventPayload.CheckRun) {
            return Optional.of(((GHEventPayload.CheckRun) eventPayload).getCheckRun().getDetailsUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.CheckSuite) {
            return Optional.of(((GHEventPayload.CheckSuite) eventPayload).getCheckSuite().getCheckRunsUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.PullRequest) {
            return Optional.of(((GHEventPayload.PullRequest) eventPayload).getPullRequest().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.PullRequestReview) {
            return Optional.of(((GHEventPayload.PullRequestReview) eventPayload).getPullRequest().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.PullRequestReviewComment) {
            return Optional.of(((GHEventPayload.PullRequestReviewComment) eventPayload).getPullRequest().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Issue) {
            return Optional.of(((GHEventPayload.Issue) eventPayload).getIssue().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.IssueComment) {
            return Optional.of(((GHEventPayload.IssueComment) eventPayload).getComment().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.CommitComment) {
            return Optional.of(((GHEventPayload.CommitComment) eventPayload).getComment().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Create) {
            return Optional.of(((GHEventPayload.Create) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Delete) {
            return Optional.of(((GHEventPayload.Delete) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Deployment) {
            return Optional.of(((GHEventPayload.Deployment) eventPayload).getDeployment().getStatusesUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.DeploymentStatus) {
            return Optional.of(((GHEventPayload.DeploymentStatus) eventPayload).getDeploymentStatus().getDeploymentUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Fork) {
            return Optional.of(((GHEventPayload.Fork) eventPayload).getForkee().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Public) {
            return Optional.of(((GHEventPayload.Public) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Push) {
            return Optional.of(((GHEventPayload.Push) eventPayload).getRef());
        }
        if (eventPayload instanceof GHEventPayload.Release) {
            return Optional.of(((GHEventPayload.Release) eventPayload).getRelease().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Repository) {
            return Optional.of(((GHEventPayload.Repository) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Status) {
            return Optional.of(((GHEventPayload.Status) eventPayload).getDescription());
        }

        return Optional.empty();
    }

    private PayloadHelper() {
    }
}
