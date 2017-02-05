package fr.jcgay.jenkins.plugins.pushbullet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

import static com.google.common.base.Objects.firstNonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PushbulletNotifier extends Notifier {

    private final String users;

    private SendNotification pushbullet;
    private Function<String, User> getUserById;

    @DataBoundConstructor
    public PushbulletNotifier(String users) {
        this.users = users;
        this.getUserById = GetUserById.INSTANCE;
        this.pushbullet = new SendNotification();
    }

    @VisibleForTesting
    PushbulletNotifier(String users, SendNotification pushbullet, Function<String, User> getUserById) {
        this.users = users;
        this.pushbullet = pushbullet;
        this.getUserById = getUserById;
    }

    public String getUsers() {
        return users;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        // Initialize fields here because Jenkins doesn't seem to call constructors when instantiating its plugins...
        // There may be a better way to do it with Guice but no time to dit into it for now.
        this.pushbullet = firstNonNull(this.pushbullet, new SendNotification());
        this.getUserById = firstNonNull(this.getUserById, GetUserById.INSTANCE);

        UserIdEquivalence byId = new UserIdEquivalence();
        LinkedHashSet<Equivalence.Wrapper<User>> users = new LinkedHashSet<>();

        Cause.UserIdCause currentUser = build.getCause(Cause.UserIdCause.class);
        if (currentUser != null) {
            users.add(byId.wrap(getUserById.apply(currentUser.getUserId())));
        }

        if (isNotBlank(this.users)) {
            for (String user : this.users.split(",")) {
                users.add(byId.wrap(getUserById.apply(user)));
            }
        }

        for (User user : build.getCulprits()) {
            users.add(byId.wrap(user));
        }

        for (Equivalence.Wrapper<User> user : users) {
            pushbullet.notify(build, user.get(), listener.getLogger());
        }

        return true;
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
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Report build status with Pushbullet";
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
