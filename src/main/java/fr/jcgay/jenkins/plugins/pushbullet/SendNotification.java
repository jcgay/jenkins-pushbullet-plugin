package fr.jcgay.jenkins.plugins.pushbullet;

import fr.jcgay.notification.Application;
import fr.jcgay.notification.Icon;
import fr.jcgay.notification.Notification;
import fr.jcgay.notification.SendNotificationException;
import hudson.model.Run;
import hudson.model.User;
import hudson.util.Secret;

import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

class SendNotification {

    private static final Icon JENKINS_ICON = Icon.create(resource("jenkins.png"), "jenkins");
    private static final Application JENKINS = Application.builder("application/jenkins", "Jenkins", JENKINS_ICON).build();

    void notify(Run<?, ?> build, User user, Secret encryptedToken, PrintStream logger) {
        if (user == null) {
            return;
        }

        if (encryptedToken == null) {
            return;
        }

        String token = encryptedToken.getPlainText();
        if (token == null || token.isEmpty()) {
            return;
        }

        Properties configuration = new Properties();
        configuration.put("notifier.pushbullet.apikey", token);

        fr.jcgay.notification.Notifier notifier = new fr.jcgay.notification.SendNotification()
            .setApplication(JENKINS)
            .addConfigurationProperties(configuration)
            .setChosenNotifier("pushbullet")
            .initNotifier();

        Status result = Status.of(build.getResult());

        Notification notification = Notification.builder()
            .title(result.message() + " of " + build.getParent().getName())
            .message(build.getFullDisplayName() + " (" + build.getDurationString() + ")")
            .icon(Icon.create(result.url(), result.message()))
            .subtitle(build.getDurationString())
            .level(result == Status.FAILURE ? Notification.Level.ERROR : Notification.Level.INFO)
            .build();

        try {
            notifier.send(notification);
            logger.println("User [" + user.getId() + "] has been notified with Pushbullet.");
        } catch (SendNotificationException e) {
            logger.println("Notification of user [" + user.getId() + "] with Pushbullet has failed.\n Error: " + e);
        } finally {
            notifier.close();
        }
    }

    private static URL resource(String resource) {
        return PushbulletNotifier.class.getClassLoader().getResource(resource);
    }
}
