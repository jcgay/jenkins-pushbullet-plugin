package fr.jcgay.jenkins.plugins.pushbullet;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

public class PushbulletUser extends UserProperty {

    private final Secret apiToken;

    @DataBoundConstructor
    public PushbulletUser(String apiToken) {
        this.apiToken = Secret.fromString(apiToken);
    }

    public String getApiToken() {
        return apiToken.getEncryptedValue();
    }

    public Secret getSecretApiToken() {
        return apiToken;
    }

    @Extension
    public static final class DescriptorImpl extends UserPropertyDescriptor {
        public String getDisplayName() {
            return "Pusbullet";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new PushbulletUser(null);
        }
    }

}
