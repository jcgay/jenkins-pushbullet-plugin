package fr.jcgay.jenkins.plugins.pushbullet;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;

public class PushbulletTokenEncryptionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void return_null_when_apiToken_is_not_set() {
        PushbulletNotifier.DescriptorImpl descriptor = new PushbulletNotifier.DescriptorImpl();

        assertThat(descriptor.getApiToken()).isNullOrEmpty();
    }

    @Test
    public void return_encrypted_value_for_apiToken() {
        PushbulletNotifier.DescriptorImpl descriptor = new PushbulletNotifier.DescriptorImpl();
        descriptor.setApiToken("token");

        assertThat(descriptor.getApiToken())
            .isNotEmpty()
            .isNotEqualTo("token");
    }
}
