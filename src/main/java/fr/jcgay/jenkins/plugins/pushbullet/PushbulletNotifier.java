package fr.jcgay.jenkins.plugins.pushbullet;

import com.google.common.annotations.VisibleForTesting;
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

        Cause.UserIdCause currentUser = build.getCause(Cause.UserIdCause.class);
        if (currentUser != null) {
            pushbullet.notify(build, getUserById.apply(currentUser.getUserId()), listener.getLogger());
        }

        if (isNotBlank(users)) {
            for (String user : users.split(",")) {
                pushbullet.notify(build, getUserById.apply(user), listener.getLogger());
            }
        }

        for (User user : build.getCulprits()) {
            pushbullet.notify(build, user, listener.getLogger());
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
}
