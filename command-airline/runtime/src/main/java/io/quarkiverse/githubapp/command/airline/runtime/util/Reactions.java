package io.quarkiverse.githubapp.command.airline.runtime.util;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHReaction;
import org.kohsuke.github.ReactionContent;

public final class Reactions {

    private static final Logger LOGGER = Logger.getLogger(Reactions.class);

    public static GHReaction createReaction(GHEventPayload.IssueComment issueCommentPayload, ReactionContent reactionContent) {
        try {
            return issueCommentPayload.getComment().createReaction(reactionContent);
        } catch (Exception e) {
            LOGGER.warn("Unable to add reaction " + reactionContent.getContent() + " to comment "
                    + issueCommentPayload.getRepository().getFullName() + "#"
                    + issueCommentPayload.getIssue().getNumber() + "#" + issueCommentPayload.getComment().getId());

            return null;
        }
    }

    public static void deleteReaction(GHEventPayload.IssueComment issueCommentPayload, GHReaction reaction) {
        if (reaction == null) {
            return;
        }

        try {
            issueCommentPayload.getComment().deleteReaction(reaction);
        } catch (Exception e) {
            LOGGER.warn("Unable to delete reaction " + reaction.getContent().getContent() + " from comment "
                    + issueCommentPayload.getRepository().getFullName() + "#"
                    + issueCommentPayload.getIssue().getNumber() + "#" + issueCommentPayload.getComment().getId());
        }
    }
}
