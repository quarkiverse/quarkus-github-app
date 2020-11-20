package io.quarkiverse.githubapp.deployment;

import java.util.Arrays;
import java.util.List;

import javax.naming.directory.SearchResult;

import org.jboss.jandex.DotName;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHBlob;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHBranchProtection;
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
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepositoryStatistics;
import org.kohsuke.github.GHRepositoryTraffic;
import org.kohsuke.github.GHStargazer;
import org.kohsuke.github.GHSubscription;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GHTagObject;
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
    static final DotName GH_MARKETPLACE_ACCOUNT = DotName.createSimple(GHMarketplaceAccount.class.getName());
    static final DotName GH_CONTENT = DotName.createSimple(GHContent.class.getName());
    static final DotName GH_OBJECT = DotName.createSimple(GHObject.class.getName());
    static final DotName PAGED_ITERABLE = DotName.createSimple(PagedIterable.class.getName());
    static final DotName GH_HOOK = DotName.createSimple(GHHook.class.getName());
    static final DotName GH_REPOSITORY_TRAFFIC = DotName.createSimple(GHRepositoryTraffic.class.getName());
    static final DotName GH_KEY = DotName.createSimple(GHKey.class.getName());
    static final DotName SEARCH_RESULT = DotName.createSimple(SearchResult.class.getName());

    static final List<DotName> GH_ROOT_OBJECTS = Arrays.asList(GH_EVENT_PAYLOAD, GH_MARKETPLACE_ACCOUNT, GH_CONTENT, GH_OBJECT,
            PAGED_ITERABLE, GH_HOOK, GH_REPOSITORY_TRAFFIC, GH_KEY, SEARCH_RESULT);

    // Simple objects
    static final DotName GH_APP_INSTALLATION_TOKEN = DotName.createSimple(GHAppInstallationToken.class.getName());
    static final DotName GH_BLOB = DotName.createSimple(GHBlob.class.getName());
    static final DotName GH_BRANCH = DotName.createSimple(GHBranch.class.getName());
    static final DotName GH_BRANCH_PROTECTION = DotName.createSimple(GHBranchProtection.class.getName());
    static final DotName GH_CHECK_RUNS_PAGE = DotName.createSimple("org.kohsuke.github.GHCheckRunsPage");
    static final DotName GH_COMMIT = DotName.createSimple(GHCommit.class.getName());
    static final DotName GH_COMMIT_POINTER = DotName.createSimple(GHCommitPointer.class.getName());
    static final DotName GH_COMPARE = DotName.createSimple(GHCompare.class.getName());
    static final DotName GH_CONTENT_UPDATE_RESPONSE = DotName.createSimple(GHContentUpdateResponse.class.getName());
    static final DotName GH_DEPLOY_KEY = DotName.createSimple(GHDeployKey.class.getName());
    static final DotName GH_EMAIL = DotName.createSimple(GHEmail.class.getName());
    static final DotName GH_EVENT_INFO = DotName.createSimple(GHEventInfo.class.getName());
    static final DotName GH_GIST_FILE = DotName.createSimple(GHGistFile.class.getName());
    static final DotName GH_ISSUE_EVENT = DotName.createSimple(GHIssueEvent.class.getName());
    static final DotName GH_LABEL = DotName.createSimple(GHLabel.class.getName());
    static final DotName GH_MARKETPLACE_PENDING_CHANGE = DotName.createSimple(GHMarketplacePendingChange.class.getName());
    static final DotName GH_MARKETPLACE_PLAN = DotName.createSimple(GHMarketplacePlan.class.getName());
    static final DotName GH_MARKETPLACE_PURCHASE = DotName.createSimple(GHMarketplacePurchase.class.getName());
    static final DotName GH_MARKETPLACE_USER_PURCHASE = DotName.createSimple(GHMarketplaceUserPurchase.class.getName());
    static final DotName GH_MEMBERSHIP = DotName.createSimple(GHMembership.class.getName());
    static final DotName GH_META = DotName.createSimple(GHMeta.class.getName());
    static final DotName GH_NOTIFICATION_STREAM = DotName.createSimple(GHNotificationStream.class.getName());
    static final DotName GH_ORG_HOOK = DotName.createSimple("org.kohsuke.github.GHOrgHook");
    static final DotName GH_PERMISSION = DotName.createSimple("org.kohsuke.github.GHPermission");
    static final DotName GH_PULL_REQUEST_COMMIT_DETAIL = DotName.createSimple(GHPullRequestCommitDetail.class.getName());
    static final DotName GH_PULL_REQUEST_FILE_DETAIL = DotName.createSimple(GHPullRequestFileDetail.class.getName());
    static final DotName GH_RATE_LIMIT = DotName.createSimple(GHRateLimit.class.getName());
    static final DotName GH_REF = DotName.createSimple(GHRef.class.getName());
    static final DotName GH_REPOSITORY_STATISTICS = DotName.createSimple(GHRepositoryStatistics.class.getName());
    static final DotName GH_STARGAZER = DotName.createSimple(GHStargazer.class.getName());
    static final DotName GH_SUBSCRIPTION = DotName.createSimple(GHSubscription.class.getName());
    static final DotName GH_TAG = DotName.createSimple(GHTag.class.getName());
    static final DotName GH_TAG_OBJECT = DotName.createSimple(GHTagObject.class.getName());
    static final DotName GH_TREE = DotName.createSimple(GHTree.class.getName());
    static final DotName GH_TREE_ENTRY = DotName.createSimple(GHTreeEntry.class.getName());
    static final DotName GH_VERIFICATION = DotName.createSimple(GHVerification.class.getName());
    static final DotName GITHUB_REQUEST = DotName.createSimple("org.kohsuke.github.GitHubRequest");
    static final DotName GITHUB_RESPONSE = DotName.createSimple("org.kohsuke.github.GitHubResponse");
    static final DotName GIT_USER = DotName.createSimple(GitUser.class.getName());

    static final List<DotName> GH_SIMPLE_OBJECTS = Arrays.asList(GH_APP_INSTALLATION_TOKEN, GH_BLOB, GH_BRANCH,
            GH_BRANCH_PROTECTION, GH_CHECK_RUNS_PAGE, GH_COMMIT, GH_COMMIT_POINTER, GH_COMPARE, GH_CONTENT_UPDATE_RESPONSE,
            GH_DEPLOY_KEY,
            GH_EMAIL, GH_EVENT_INFO, GH_GIST_FILE, GH_ISSUE_EVENT, GH_LABEL, GH_MARKETPLACE_PENDING_CHANGE, GH_MARKETPLACE_PLAN,
            GH_MARKETPLACE_PURCHASE, GH_MARKETPLACE_USER_PURCHASE, GH_MEMBERSHIP, GH_META, GH_NOTIFICATION_STREAM,
            GH_ORG_HOOK, GH_PERMISSION, GH_PULL_REQUEST_COMMIT_DETAIL, GH_PULL_REQUEST_FILE_DETAIL, GH_RATE_LIMIT, GH_REF,
            GH_REPOSITORY_STATISTICS, GH_STARGAZER, GH_SUBSCRIPTION, GH_TAG, GH_TAG_OBJECT, GH_TREE, GH_TREE_ENTRY,
            GH_VERIFICATION, GITHUB_REQUEST, GITHUB_RESPONSE, GIT_USER);

    private GitHubAppDotNames() {
    }
}
