package fr.jcgay.jenkins.plugins.pushbullet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PushbulletNotifier extends Notifier implements SimpleBuildStep {

    private String users;
    private SendNotification pushbullet;
    private Function<String, User> getUserById;
    private DescriptorImpl descriptor;

    @DataBoundConstructor
    public PushbulletNotifier() {
        this.pushbullet = new SendNotification();
        this.getUserById = GetUserById.INSTANCE;
    }

    @VisibleForTesting
    PushbulletNotifier(String users, SendNotification pushbullet, Function<String, User> getUserById, PushbulletNotifier.DescriptorImpl descriptor) {
        this.users = users;
        this.pushbullet = pushbullet;
        this.getUserById = getUserById;
        this.descriptor = descriptor;
    }

    public String getUsers() {
        return users;
    }

    @DataBoundSetter
    public void setUsers(String users) {
        this.users = users;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        dirtyInit();
        perform(build, build.getCulprits(), listener.getLogger());
        return true;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        dirtyInit();
        perform(run, getUsersInvolvedInChanges(run), listener.getLogger());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        if (descriptor != null) {
            return descriptor;
        }
        return (DescriptorImpl) super.getDescriptor();
    }

    private static Set<User> getUsersInvolvedInChanges(Run<?, ?> run) {
        if (run instanceof WorkflowRun) {
            return FluentIterable.from(((WorkflowRun) run).getChangeSets())
                .transformAndConcat(getUsersFromChangeSet())
                .filter(Predicates.<User>notNull())
                .toSet();
        }
        return Collections.emptySet();
    }

    private static Function<ChangeLogSet<? extends ChangeLogSet.Entry>, Iterable<User>> getUsersFromChangeSet() {
        return new Function<ChangeLogSet<? extends ChangeLogSet.Entry>, Iterable<User>>() {
            @Override
            public Iterable<User> apply(@Nullable ChangeLogSet<? extends ChangeLogSet.Entry> entries) {
                if (entries == null) {
                    return Collections.emptySet();
                }

                Set<User> result = new HashSet<>();
                for (ChangeLogSet.Entry entry : entries) {
                    result.add(entry.getAuthor());
                }
                return result;
            }
        };
    }

    private void dirtyInit() {
        // Initialize fields here because Jenkins doesn't seem to call constructors when instantiating its plugins...
        // There may be a better way to do it with Guice but no time to dit into it for now.
        this.pushbullet = firstNonNull(this.pushbullet, new SendNotification());
        this.getUserById = firstNonNull(this.getUserById, GetUserById.INSTANCE);
    }

    private void perform(Run run, Set<User> culprits, PrintStream logger) {
        UserIdEquivalence byId = new UserIdEquivalence();
        LinkedHashSet<Equivalence.Wrapper<User>> usersToNotify = new LinkedHashSet<>();

        Cause.UserIdCause currentUser = (Cause.UserIdCause) run.getCause(Cause.UserIdCause.class);
        if (currentUser != null) {
            usersToNotify.add(byId.wrap(getUserById.apply(currentUser.getUserId())));
        }

        if (isNotBlank(this.users)) {
            for (String user : this.users.split(",")) {
                usersToNotify.add(byId.wrap(getUserById.apply(user)));
            }
        }

        Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) run.getCause(Cause.UpstreamCause.class);
        if (upstreamCause != null) {
            String userId = tryFindUserWhoHasLaunchedUpstreamJob(upstreamCause);
            if (userId != null) {
                usersToNotify.add(byId.wrap(getUserById.apply(userId)));
            }
        }

        for (User user : culprits) {
            usersToNotify.add(byId.wrap(user));
        }

        Secret token = getDescriptor().getSecretApiToken();
        for (Equivalence.Wrapper<User> user : usersToNotify) {
            pushbullet.notify(run, user.get(), token, logger);
        }
    }

    private String tryFindUserWhoHasLaunchedUpstreamJob(Cause.UpstreamCause cause) {
        for (Cause upstreamCause : cause.getUpstreamCauses()) {
            if (upstreamCause instanceof Cause.UserIdCause) {
                return ((Cause.UserIdCause) upstreamCause).getUserId();
            }
            if (upstreamCause instanceof Cause.UpstreamCause) {
                return tryFindUserWhoHasLaunchedUpstreamJob((Cause.UpstreamCause) upstreamCause);
            }
        }
        return null;
    }

    private enum GetUserById implements Function<String, User> {
        INSTANCE {
            @Override
            public User apply(@Nullable String id) {
                return User.get(id, false, Collections.emptyMap());
            }
        }
    }

    @Extension
    @Symbol("pushbullet")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private Secret apiToken;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return super.configure(req, json);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Report build status with Pushbullet";
        }

        public String getApiToken() {
            if (apiToken == null) {
                return null;
            }
            String token = apiToken.getPlainText();
            if (token == null || token.isEmpty()) {
                return null;
            }
            return apiToken.getEncryptedValue();
        }

        public void setApiToken(String apiToken) {
            this.apiToken = Secret.fromString(apiToken);
        }

        public Secret getSecretApiToken() {
            return apiToken;
        }
    }

    private static class UserIdEquivalence extends Equivalence<User> {
        @Override
        protected boolean doEquivalent(User a, User b) {
            return Objects.equals(a.getId(), b.getId());
        }

        @Override
        protected int doHash(User user) {
            return Objects.hash(user.getId());
        }
    }

}
