package com.cloudbees.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;
import hudson.model.Queue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inject the payload received by BitBucket into the build through $BITBUCKET_PAYLOAD so it can be processed
 * The class implements FoldableAction (through CauseAction) to support payload capture when multiple repositories
 * trigger the job at nearly same time when the job has concurrent builds disabled and quiet period zero.
 * Jenkins Queue class works in such a way that in such a case it allows one build into the queue and ignores the others as "duplicates",
 * but it attaches the FoldableAction of the ignored builds into the action list of the first scheduled build.
 * This way we guarantee that every single event does not get lost and its payload is merged with the summary payload.
 * @since September 15, 2026
 * @version 1.1.6
 */
public class BitBucketPayload extends CauseAction implements EnvironmentContributingAction {
    private final @NonNull String payload;

    public BitBucketPayload(Cause cause, @NonNull String payload) {
        super(cause);
        this.payload = payload;
    }

    @Override
    public void foldIntoExisting(Queue.Item item, Queue.Task owner, List<Action> otherActions) {
        super.foldIntoExisting(item, owner, otherActions);
        item.addAction(this);
    }

    @NonNull
    public String getPayload() {
        return payload;
    }

    @Override
    public void buildEnvironment(@NonNull Run<?, ?> run, @NonNull EnvVars env) {
        EnvironmentContributingAction.super.buildEnvironment(run, env);
        String payload = getPayload();
        String currentPayloads = env.get("BITBUCKET_PAYLOAD");
        if (currentPayloads != null && !currentPayloads.trim().isEmpty()) {
            payload += ',' + currentPayloads;
        }
        LOGGER.log(Level.FINEST, "Injecting BITBUCKET_PAYLOAD: {0}", payload);
        env.put("BITBUCKET_PAYLOAD", payload);
    }

    private static final Logger LOGGER = Logger.getLogger(BitBucketPayload.class.getName());
}
