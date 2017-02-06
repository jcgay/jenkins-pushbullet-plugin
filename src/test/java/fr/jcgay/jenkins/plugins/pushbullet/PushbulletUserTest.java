package fr.jcgay.jenkins.plugins.pushbullet;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;

public class PushbulletUserTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void return_null_when_apiToken_is_not_set() {
        PushbulletUser user = new PushbulletUser(null);

        assertThat(user.getApiToken()).isNullOrEmpty();
    }

    @Test
    public void return_encrypted_value_for_apiToken() {
        PushbulletUser user = new PushbulletUser("token");

        assertThat(user.getApiToken())
            .isNotEmpty()
            .isNotEqualTo("token");
    }
}
