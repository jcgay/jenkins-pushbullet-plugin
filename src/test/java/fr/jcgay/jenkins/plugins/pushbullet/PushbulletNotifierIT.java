package fr.jcgay.jenkins.plugins.pushbullet;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PushbulletNotifierIT {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test(timeout = 30 * 1000)
    public void should_fail_fast_when_access_token_is_not_valid() throws Exception {
        PushbulletNotifier notifier = new PushbulletNotifier();
        notifier.getDescriptor().setApiToken("bad-access-token");
        notifier.setUsers(User.get("jc").getId());

        FreeStyleProject project = jenkins.createFreeStyleProject("notify-me");
        project.getPublishersList().add(notifier);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Notification of user [jc] with Pushbullet has failed.", build);
    }
}
