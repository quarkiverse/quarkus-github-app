package io.quarkiverse.githubapp.runtime.github;

import java.util.Optional;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHRepository;

public final class PayloadHelper {

    public static GHRepository getRepository(GHEventPayload eventPayload) {
        GHRepository repository = eventPayload.getRepository();

        if (repository == null) {
            throw new IllegalStateException("Unable to extract repository information from payload type: "
                    + eventPayload.getClass().getName() + ". This is needed for config files.");
        }

        return repository;
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
            return Optional
                    .of(((GHEventPayload.PullRequestReviewComment) eventPayload).getPullRequest().getHtmlUrl().toString());
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
            return Optional
                    .of(((GHEventPayload.DeploymentStatus) eventPayload).getDeploymentStatus().getDeploymentUrl().toString());
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
        if (eventPayload instanceof GHEventPayload.WorkflowDispatch) {
            return Optional.of(((GHEventPayload.WorkflowDispatch) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.WorkflowRun) {
            return Optional.of(((GHEventPayload.WorkflowRun) eventPayload).getWorkflowRun().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.WorkflowJob) {
            return Optional.of(((GHEventPayload.WorkflowJob) eventPayload).getWorkflowJob().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Label) {
            return Optional.of(((GHEventPayload.Label) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Discussion) {
            return Optional.of(((GHEventPayload.Discussion) eventPayload).getDiscussion().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.DiscussionComment) {
            return Optional.of(((GHEventPayload.DiscussionComment) eventPayload).getComment().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Star) {
            return Optional.of(((GHEventPayload.Star) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.ProjectsV2Item) {
            return Optional.of(((GHEventPayload.ProjectsV2Item) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.TeamAdd) {
            return Optional.of(((GHEventPayload.TeamAdd) eventPayload).getTeam().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Team) {
            return Optional.of(((GHEventPayload.Team) eventPayload).getTeam().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Member) {
            return Optional.of(((GHEventPayload.Member) eventPayload).getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Membership) {
            return Optional.of(((GHEventPayload.Membership) eventPayload).getTeam().getHtmlUrl().toString());
        }

        return Optional.empty();
    }

    private PayloadHelper() {
    }
}
