package io.quarkiverse.githubapp.deployment;

import java.util.Arrays;
import java.util.List;

import javax.naming.directory.SearchResult;

import org.jboss.jandex.DotName;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHAuthorization;
import org.kohsuke.github.GHBlob;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHBranchProtection;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckSuite;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHContentUpdateResponse;
import org.kohsuke.github.GHDeployKey;
import org.kohsuke.github.GHEmail;
import org.kohsuke.github.GHEventInfo;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHGistFile;
import org.kohsuke.github.GHHook;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueEvent;
import org.kohsuke.github.GHKey;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHMarketplaceAccount;
import org.kohsuke.github.GHMarketplacePendingChange;
import org.kohsuke.github.GHMarketplacePlan;
import org.kohsuke.github.GHMarketplacePurchase;
import org.kohsuke.github.GHMarketplaceUserPurchase;
import org.kohsuke.github.GHMembership;
import org.kohsuke.github.GHMeta;
import org.kohsuke.github.GHNotificationStream;
import org.kohsuke.github.GHObject;
import org.kohsuke.github.GHPullRequestChanges;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryStatistics;
import org.kohsuke.github.GHRepositoryTraffic;
import org.kohsuke.github.GHStargazer;
import org.kohsuke.github.GHSubscription;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GHTagObject;
import org.kohsuke.github.GHThread;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GHVerification;
import org.kohsuke.github.GitUser;
import org.kohsuke.github.PagedIterable;

import io.quarkiverse.githubapp.ConfigFile;
import io.quarkiverse.githubapp.event.Event;

final class GitHubAppDotNames {

    static final DotName EVENT = DotName.createSimple(Event.class.getName());
    static final DotName CONFIG_FILE = DotName.createSimple(ConfigFile.class.getName());

    // GitHub API

    // Root objects
    static final DotName GH_EVENT_PAYLOAD = DotName.createSimple(GHEventPayload.class.getName());
    private static final DotName GH_MARKETPLACE_ACCOUNT = DotName.createSimple(GHMarketplaceAccount.class.getName());
    private static final DotName GH_CONTENT = DotName.createSimple(GHContent.class.getName());
    private static final DotName GH_OBJECT = DotName.createSimple(GHObject.class.getName());
    private static final DotName PAGED_ITERABLE = DotName.createSimple(PagedIterable.class.getName());
    private static final DotName GH_HOOK = DotName.createSimple(GHHook.class.getName());
    private static final DotName GH_REPOSITORY_TRAFFIC = DotName.createSimple(GHRepositoryTraffic.class.getName());
    private static final DotName GH_REPOSITORY_TRAFFIC_DAILY_INFO = DotName.createSimple(GHRepositoryTraffic.DailyInfo.class.getName());
    private static final DotName GH_KEY = DotName.createSimple(GHKey.class.getName());
    private static final DotName SEARCH_RESULT = DotName.createSimple(SearchResult.class.getName());
    private static final DotName GIT_USER = DotName.createSimple(GitUser.class.getName());
    private static final DotName GH_COMMIT = DotName.createSimple(GHCommit.class.getName());

    static final List<DotName> GH_ROOT_OBJECTS = Arrays.asList(GH_EVENT_PAYLOAD, GH_MARKETPLACE_ACCOUNT, GH_CONTENT, GH_OBJECT,
            PAGED_ITERABLE, GH_HOOK, GH_REPOSITORY_TRAFFIC, GH_REPOSITORY_TRAFFIC_DAILY_INFO, GH_KEY, SEARCH_RESULT, GIT_USER, GH_COMMIT);

    // Simple objects
    private static final DotName GH_APP_INSTALLATION_GH_APP_INSTALLATION_REPOSITORY_RESULT = DotName
            .createSimple(GHAppInstallation.class.getName() + "$GHAppInstallationRepositoryResult");
    private static final DotName GH_APP_INSTALLATION_TOKEN = DotName.createSimple(GHAppInstallationToken.class.getName());
    private static final DotName GH_APP_AUTHORIZATION_APP = DotName.createSimple(GHAuthorization.class.getName() + "$App");
    private static final DotName GH_BLOB = DotName.createSimple(GHBlob.class.getName());
    private static final DotName GH_BRANCH = DotName.createSimple(GHBranch.class.getName());
    private static final DotName GH_BRANCH_COMMIT = DotName.createSimple(GHBranch.Commit.class.getName());
    private static final DotName GH_BRANCH_PROTECTION = DotName.createSimple(GHBranchProtection.class.getName());
    private static final DotName GH_BRANCH_PROTECTION_ENFORCE_ADMINS = DotName.createSimple(GHBranchProtection.EnforceAdmins.class.getName());
    private static final DotName GH_BRANCH_PROTECTION_REQUIRED_REVIEWS = DotName.createSimple(GHBranchProtection.RequiredReviews.class.getName());
    private static final DotName GH_BRANCH_PROTECTION_REQUIRED_SIGNATURES = DotName.createSimple(GHBranchProtection.class.getName() + "$RequiredSignatures");
    private static final DotName GH_BRANCH_PROTECTION_REQUIRED_STATUS_CHECKS = DotName.createSimple(GHBranchProtection.RequiredStatusChecks.class.getName());
    private static final DotName GH_BRANCH_PROTECTION_RESTRICTIONS = DotName.createSimple(GHBranchProtection.Restrictions.class.getName());
    private static final DotName GH_CHECK_RUN_OUTPUT = DotName.createSimple(GHCheckRun.Output.class.getName());
    private static final DotName GH_CHECK_RUNS_PAGE = DotName.createSimple("org.kohsuke.github.GHCheckRunsPage");
    private static final DotName GH_CHECK_SUITE_HEAD_COMMIT = DotName.createSimple(GHCheckSuite.HeadCommit.class.getName());
    private static final DotName GH_COMMIT_FILE = DotName.createSimple(GHCommit.File.class.getName());
    private static final DotName GH_COMMIT_PARENT = DotName.createSimple(GHCommit.Parent.class.getName());
    private static final DotName GH_COMMIT_SHORT_INFO = DotName.createSimple(GHCommit.ShortInfo.class.getName());
    private static final DotName GH_COMMIT_STATS = DotName.createSimple(GHCommit.Stats.class.getName());
    private static final DotName GH_COMMIT_TREE = DotName.createSimple(GHCommit.class.getName() + "$Tree");
    private static final DotName GH_COMMIT_USER = DotName.createSimple(GHCommit.class.getName() + "$User");
    private static final DotName GH_COMMIT_POINTER = DotName.createSimple(GHCommitPointer.class.getName());
    private static final DotName GH_COMPARE = DotName.createSimple(GHCompare.class.getName());
    private static final DotName GH_COMPARE_INNER_COMMIT = DotName.createSimple(GHCompare.InnerCommit.class.getName());
    private static final DotName GH_COMPARE_TREE = DotName.createSimple(GHCompare.Tree.class.getName());
    private static final DotName GH_CONTENT_UPDATE_RESPONSE = DotName.createSimple(GHContentUpdateResponse.class.getName());
    private static final DotName GH_DEPLOY_KEY = DotName.createSimple(GHDeployKey.class.getName());
    private static final DotName GH_EMAIL = DotName.createSimple(GHEmail.class.getName());
    private static final DotName GH_EVENT_INFO = DotName.createSimple(GHEventInfo.class.getName());
    private static final DotName GH_EVENT_INFO_GH_EVENT_REPOSITORY = DotName.createSimple(GHEventInfo.GHEventRepository.class.getName());
    private static final DotName GH_EVENT_PAYLOAD_PUSH_PUSHER = DotName.createSimple(GHEventPayload.Push.Pusher.class.getName());
    private static final DotName GH_EVENT_PAYLOAD_PUSH_PUSH_COMMIT = DotName.createSimple(GHEventPayload.Push.PushCommit.class.getName());
    private static final DotName GH_GIST_FILE = DotName.createSimple(GHGistFile.class.getName());
    private static final DotName GH_ISSUE_PULL_REQUEST = DotName.createSimple(GHIssue.PullRequest.class.getName());
    private static final DotName GH_ISSUE_EVENT = DotName.createSimple(GHIssueEvent.class.getName());
    private static final DotName GH_LABEL = DotName.createSimple(GHLabel.class.getName());
    private static final DotName GH_MARKETPLACE_PENDING_CHANGE = DotName.createSimple(GHMarketplacePendingChange.class.getName());
    private static final DotName GH_MARKETPLACE_PLAN = DotName.createSimple(GHMarketplacePlan.class.getName());
    private static final DotName GH_MARKETPLACE_PURCHASE = DotName.createSimple(GHMarketplacePurchase.class.getName());
    private static final DotName GH_MARKETPLACE_USER_PURCHASE = DotName.createSimple(GHMarketplaceUserPurchase.class.getName());
    private static final DotName GH_MEMBERSHIP = DotName.createSimple(GHMembership.class.getName());
    private static final DotName GH_META = DotName.createSimple(GHMeta.class.getName());
    private static final DotName GH_NOTIFICATION_STREAM = DotName.createSimple(GHNotificationStream.class.getName());
    private static final DotName GH_ORG_HOOK = DotName.createSimple("org.kohsuke.github.GHOrgHook");
    private static final DotName GH_PERMISSION = DotName.createSimple("org.kohsuke.github.GHPermission");
    private static final DotName GH_PULL_REQUEST_CHANGES = DotName.createSimple(GHPullRequestChanges.class.getName());
    private static final DotName GH_PULL_REQUEST_CHANGES_GH_FROM = DotName.createSimple(GHPullRequestChanges.GHFrom.class.getName());
    private static final DotName GH_PULL_REQUEST_CHANGES_GH_COMMIT_POINTER = DotName.createSimple(GHPullRequestChanges.GHCommitPointer.class.getName());
    private static final DotName GH_PULL_REQUEST_COMMIT_DETAIL = DotName.createSimple(GHPullRequestCommitDetail.class.getName());
    private static final DotName GH_PULL_REQUEST_COMMIT_DETAIL_COMMIT = DotName.createSimple(GHPullRequestCommitDetail.Commit.class.getName());
    private static final DotName GH_PULL_REQUEST_COMMIT_DETAIL_COMMIT_POINTER = DotName.createSimple(GHPullRequestCommitDetail.CommitPointer.class.getName());
    private static final DotName GH_PULL_REQUEST_COMMIT_DETAIL_TREE = DotName.createSimple(GHPullRequestCommitDetail.Tree.class.getName());
    private static final DotName GH_PULL_REQUEST_FILE_DETAIL = DotName.createSimple(GHPullRequestFileDetail.class.getName());
    private static final DotName GH_RATE_LIMIT = DotName.createSimple(GHRateLimit.class.getName());
    private static final DotName GH_RATE_LIMIT_RECORD = DotName.createSimple(GHRateLimit.Record.class.getName());
    private static final DotName GH_RATE_LIMIT_UNKNOWN_LIMIT_RECORD = DotName.createSimple(GHRateLimit.UnknownLimitRecord.class.getName());
    private static final DotName GH_REF = DotName.createSimple(GHRef.class.getName());
    private static final DotName GH_REF_GH_OBJECT = DotName.createSimple(GHRef.GHObject.class.getName());
    private static final DotName GH_REPOSITORY_GH_REPO_PERMISSION = DotName.createSimple(GHRepository.class.getName() + "$GHRepoPermission");
    private static final DotName GH_REPOSITORY_TOPICS = DotName.createSimple(GHRepository.class.getName() + "$Topics");
    private static final DotName GH_REPOSITORY_STATISTICS = DotName.createSimple(GHRepositoryStatistics.class.getName());
    private static final DotName GH_REPOSITORY_STATISTICS_CONTRIBUTOR_STATS_WEEK = DotName.createSimple(GHRepositoryStatistics.ContributorStats.Week.class.getName());
    private static final DotName GH_REPOSITORY_STATISTICS_CODE_FREQUENCY = DotName.createSimple(GHRepositoryStatistics.CodeFrequency.class.getName());
    private static final DotName GH_REPOSITORY_STATISTICS_PUNCH_CARD_ITEM = DotName.createSimple(GHRepositoryStatistics.PunchCardItem.class.getName());
    private static final DotName GH_STARGAZER = DotName.createSimple(GHStargazer.class.getName());
    private static final DotName GH_SUBSCRIPTION = DotName.createSimple(GHSubscription.class.getName());
    private static final DotName GH_TAG = DotName.createSimple(GHTag.class.getName());
    private static final DotName GH_TAG_OBJECT = DotName.createSimple(GHTagObject.class.getName());
    private static final DotName GH_THREAD_SUBJECT = DotName.createSimple(GHThread.class.getName() + "$Subject");
    private static final DotName GH_TREE = DotName.createSimple(GHTree.class.getName());
    private static final DotName GH_TREE_ENTRY = DotName.createSimple(GHTreeEntry.class.getName());
    private static final DotName GH_VERIFICATION = DotName.createSimple(GHVerification.class.getName());
    private static final DotName GITHUB_REQUEST = DotName.createSimple("org.kohsuke.github.GitHubRequest");
    private static final DotName GITHUB_REQUEST_ENTRY = DotName.createSimple("org.kohsuke.github.GitHubRequest$Entry");
    private static final DotName GITHUB_RESPONSE = DotName.createSimple("org.kohsuke.github.GitHubResponse");

    static final List<DotName> GH_SIMPLE_OBJECTS = Arrays.asList(GH_APP_INSTALLATION_GH_APP_INSTALLATION_REPOSITORY_RESULT,
            GH_APP_INSTALLATION_TOKEN, GH_APP_AUTHORIZATION_APP, GH_BLOB,
            GH_BRANCH,
            GH_BRANCH_COMMIT, GH_BRANCH_PROTECTION, GH_BRANCH_PROTECTION_ENFORCE_ADMINS, GH_BRANCH_PROTECTION_REQUIRED_REVIEWS,
            GH_BRANCH_PROTECTION_REQUIRED_SIGNATURES, GH_BRANCH_PROTECTION_REQUIRED_STATUS_CHECKS,
            GH_BRANCH_PROTECTION_RESTRICTIONS, GH_CHECK_RUN_OUTPUT, GH_CHECK_RUNS_PAGE, GH_CHECK_SUITE_HEAD_COMMIT,
            GH_COMMIT_FILE, GH_COMMIT_PARENT, GH_COMMIT_SHORT_INFO,
            GH_COMMIT_STATS, GH_COMMIT_TREE, GH_COMMIT_USER,
            GH_COMMIT_POINTER, GH_COMPARE, GH_COMPARE_INNER_COMMIT, GH_COMPARE_TREE, GH_CONTENT_UPDATE_RESPONSE,
            GH_DEPLOY_KEY,
            GH_EMAIL, GH_EVENT_INFO, GH_EVENT_INFO_GH_EVENT_REPOSITORY, GH_EVENT_PAYLOAD_PUSH_PUSHER,
            GH_EVENT_PAYLOAD_PUSH_PUSH_COMMIT, GH_GIST_FILE, GH_ISSUE_PULL_REQUEST, GH_ISSUE_EVENT, GH_LABEL,
            GH_MARKETPLACE_PENDING_CHANGE, GH_MARKETPLACE_PLAN,
            GH_MARKETPLACE_PURCHASE, GH_MARKETPLACE_USER_PURCHASE, GH_MEMBERSHIP, GH_META, GH_NOTIFICATION_STREAM,
            GH_ORG_HOOK, GH_PERMISSION, GH_PULL_REQUEST_CHANGES, GH_PULL_REQUEST_CHANGES_GH_FROM,
            GH_PULL_REQUEST_CHANGES_GH_COMMIT_POINTER,
            GH_PULL_REQUEST_COMMIT_DETAIL, GH_PULL_REQUEST_COMMIT_DETAIL_COMMIT,
            GH_PULL_REQUEST_COMMIT_DETAIL_COMMIT_POINTER, GH_PULL_REQUEST_COMMIT_DETAIL_TREE,
            GH_PULL_REQUEST_FILE_DETAIL, GH_RATE_LIMIT, GH_RATE_LIMIT_RECORD, GH_RATE_LIMIT_UNKNOWN_LIMIT_RECORD, GH_REF,
            GH_REF_GH_OBJECT, GH_REPOSITORY_GH_REPO_PERMISSION, GH_REPOSITORY_TOPICS,
            GH_REPOSITORY_STATISTICS, GH_REPOSITORY_STATISTICS_CONTRIBUTOR_STATS_WEEK, GH_REPOSITORY_STATISTICS_CODE_FREQUENCY,
            GH_REPOSITORY_STATISTICS_PUNCH_CARD_ITEM, GH_STARGAZER, GH_SUBSCRIPTION, GH_TAG, GH_TAG_OBJECT, GH_THREAD_SUBJECT,
            GH_TREE, GH_TREE_ENTRY,
            GH_VERIFICATION, GITHUB_REQUEST, GITHUB_REQUEST_ENTRY, GITHUB_RESPONSE);

    private GitHubAppDotNames() {
    }
}
