package fr.jcgay.jenkins.plugins.pushbullet;

import fr.jcgay.notification.Application;
import fr.jcgay.notification.Icon;
import fr.jcgay.notification.Notification;
import fr.jcgay.notification.Notification.Level;
import fr.jcgay.notification.SendNotification;
import fr.jcgay.notification.SendNotificationException;
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

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PushbulletNotifier extends Notifier {

    private static final Icon JENKINS_ICON = Icon.create(resource("jenkins.png"), "jenkins");
    private static final Application JENKINS = Application.builder("application/jenkins", "Jenkins", JENKINS_ICON).build();

    private final String users;

    @DataBoundConstructor
    public PushbulletNotifier(String users) {
        this.users = users;
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
        Cause.UserIdCause currentUser = build.getCause(Cause.UserIdCause.class);
        if (currentUser != null) {
            notify(build, User.get(currentUser.getUserId()), listener.getLogger());
        }

        if (isNotBlank(users)) {
            for (String user : users.split(",")) {
                notify(build, User.get(user), listener.getLogger());
            }
        }

        for (User user : build.getCulprits()) {
            notify(build, user, listener.getLogger());
        }

        return true;
    }

    private void notify(AbstractBuild<?, ?> build, User user, PrintStream logger) {
        PushbulletUser pushbullet = user.getProperty(PushbulletUser.class);
        if (pushbullet == null || isBlank(pushbullet.getApiToken())) {
            return;
        }

        Properties configuration = new Properties();
        configuration.put("notifier.pushbullet.apikey", pushbullet.getApiToken());

        fr.jcgay.notification.Notifier notifier = new SendNotification()
                .setApplication(JENKINS)
                .addConfigurationProperties(configuration)
                .setChosenNotifier("pushbullet")
                .initNotifier();

        Status result = Status.of(build.getResult());

        Notification notification = Notification.builder(result.message() + " of " + build.getFullDisplayName(), build.getUrl(), Icon.create(result.url(), result.message()))
                .subtitle(build.getDurationString())
                .level(result == Status.FAILURE ? Level.ERROR : Level.INFO)
                .build();

        try {
            notifier.send(notification);
            logger.println("User [" + user.getId() + "] has been notified with Pushbullet.");
        } catch (SendNotificationException ignored) {
            logger.println("Notification of user [" + user.getId() + "] with Pushbullet has failed.");
        } finally {
            notifier.close();
        }
    }

    private static URL resource(String resource) {
        return PushbulletNotifier.class.getClassLoader().getResource(resource);
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
