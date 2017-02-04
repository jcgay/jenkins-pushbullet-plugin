package fr.jcgay.jenkins.plugins.pushbullet;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class PushbulletUser extends UserProperty {

    private final String apiToken;

    @DataBoundConstructor
    public PushbulletUser(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getApiToken() {
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
