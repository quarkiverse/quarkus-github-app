package io.quarkiverse.githubapp.runtime.github;

import java.io.IOException;
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
        if (eventPayload instanceof GHEventPayload.CheckRun checkRun) {
            return Optional.of(checkRun.getCheckRun().getDetailsUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.CheckSuite checkSuite) {
            return Optional.of(checkSuite.getCheckSuite().getCheckRunsUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.PullRequest pullRequest) {
            return Optional.of(pullRequest.getPullRequest().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.PullRequestReview pullRequestReview) {
            return Optional.of(pullRequestReview.getPullRequest().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.PullRequestReviewComment pullRequestReviewComment) {
            return Optional.of(pullRequestReviewComment.getPullRequest().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Issue issue) {
            return Optional.of(issue.getIssue().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.IssueComment issueComment) {
            return Optional.of(issueComment.getComment().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.CommitComment commitComment) {
            return Optional.of(commitComment.getComment().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Create create) {
            return Optional.of(create.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Delete delete) {
            return Optional.of(delete.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Deployment deployment) {
            return Optional.of(deployment.getDeployment().getStatusesUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.DeploymentStatus deploymentStatus) {
            return Optional.of(deploymentStatus.getDeploymentStatus().getDeploymentUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Fork fork) {
            return Optional.of(fork.getForkee().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Public publicEvent) {
            return Optional.of(publicEvent.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Push push) {
            return Optional.of(push.getRef());
        }
        if (eventPayload instanceof GHEventPayload.Release release) {
            return Optional.of(release.getRelease().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Repository repositoryEvent) {
            return Optional.of(repositoryEvent.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Status status) {
            return Optional.of(status.getDescription());
        }
        if (eventPayload instanceof GHEventPayload.WorkflowDispatch workflowDispatch) {
            return Optional.of(workflowDispatch.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.WorkflowRun workflowRun) {
            // getHtmlUrl() for workflow runs can throw an exception
            try {
                return Optional.of(workflowRun.getWorkflowRun().getHtmlUrl().toString());
            } catch (IOException e) {
                return Optional.of(workflowRun.getRepository().getHtmlUrl().toString());
            }
        }
        if (eventPayload instanceof GHEventPayload.WorkflowJob workflowJob) {
            return Optional.of(workflowJob.getWorkflowJob().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Label label) {
            return Optional.of(label.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Discussion discussion) {
            return Optional.of(discussion.getDiscussion().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.DiscussionComment discussionComment) {
            return Optional.of(discussionComment.getComment().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Star star) {
            return Optional.of(star.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.ProjectsV2Item projectsV2Item) {
            return Optional.of(projectsV2Item.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.TeamAdd teamAdd) {
            return Optional.of(teamAdd.getTeam().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Team team) {
            return Optional.of(team.getTeam().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Member member) {
            return Optional.of(member.getRepository().getHtmlUrl().toString());
        }
        if (eventPayload instanceof GHEventPayload.Membership membership) {
            return Optional.of(membership.getTeam().getHtmlUrl().toString());
        }

        return Optional.empty();
    }

    private PayloadHelper() {
    }
}
