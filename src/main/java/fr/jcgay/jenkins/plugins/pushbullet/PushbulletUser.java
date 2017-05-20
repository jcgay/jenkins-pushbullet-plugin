package fr.jcgay.jenkins.plugins.pushbullet;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class PushbulletUser extends UserProperty {

    private final String accountId;

    @DataBoundConstructor
    public PushbulletUser(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return this.accountId;
    }

    @Extension
    public static final class DescriptorImpl extends UserPropertyDescriptor {
        public String getDisplayName() {
            return "Pushbullet";
        }

        @Override
        public UserProperty newInstance(User user) {
            return new PushbulletUser(null);
        }
    }

}
