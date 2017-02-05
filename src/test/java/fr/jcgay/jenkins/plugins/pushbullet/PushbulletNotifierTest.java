package fr.jcgay.jenkins.plugins.pushbullet;

import com.google.common.base.Function;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.User;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushbulletNotifierTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<User> user;

    @Mock
    private SendNotification pushbullet;

    private Function<String, User> getUserById = new Function<String, User>() {
        @Override
        public User apply(@Nullable String id) {
            return user(id);
        }
    };

    private PushbulletNotifier notifier;

    @Test
    public void send_notification_to_the_user_who_has_launched_the_build() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(buildLaunchedBy("jc"), anyLauncher(), anyBuildListener());

        verify(pushbullet).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    @Test
    public void send_notification_to_the_users_configured_for_the_job() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier("jc,toto,titi", pushbullet, getUserById);
        notifier.perform(anyBuild(), anyLauncher(), anyBuildListener());

        verify(pushbullet, times(3)).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getAllValues())
            .extracting("id")
            .hasSize(3)
            .containsOnly("jc", "toto", "titi");
    }

    @Test
    public void send_notification_to_the_user_configured_for_the_job() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier("jc", pushbullet, getUserById);
        notifier.perform(anyBuild(), anyLauncher(), anyBuildListener());

        verify(pushbullet).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    @Test
    public void send_notification_to_the_users_present_in_the_build_culprits_list() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(buildWithCulprits("jc", "toto", "titi"), anyLauncher(), anyBuildListener());

        verify(pushbullet, times(3)).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getAllValues())
            .extracting("id")
            .hasSize(3)
            .containsOnly("jc", "toto", "titi");
    }

    @Test
    public void no_notification_when_no_real_user_has_been_found() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(anyBuild(), anyLauncher(), anyBuildListener());

        verify(pushbullet, never()).notify(any(AbstractBuild.class), any(User.class), any(PrintStream.class));
    }

    @Test
    public void send_notification_only_once_when_user_appears_in_multiple_source() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier("jc", pushbullet, getUserById);
        notifier.perform(buildLaunchedBy("jc", withCulprits("jc")), anyLauncher(), anyBuildListener());

        verify(pushbullet, times(1)).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    private static String[] withCulprits(String userId) {
        return new String[]{userId};
    }

    private static BuildListener anyBuildListener() {
        BuildListener listener = mock(BuildListener.class);
        when(listener.getLogger()).thenReturn(mock(PrintStream.class));
        return listener;
    }

    private static Launcher anyLauncher() {
        return mock(Launcher.class);
    }

    private static AbstractBuild anyBuild() {
        return mock(AbstractBuild.class);
    }

    private static AbstractBuild buildLaunchedBy(String userId, String... culpritsUserIds) {
        Cause.UserIdCause user = mock(Cause.UserIdCause.class);
        when(user.getUserId()).thenReturn(userId);

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getCause(Cause.UserIdCause.class)).thenReturn(user);

        if (culpritsUserIds != null) {
            addCulprits(build, culpritsUserIds);
        }

        return build;
    }

    private static AbstractBuild buildWithCulprits(String... userIds) {
        AbstractBuild build = mock(AbstractBuild.class);
        addCulprits(build, userIds);
        return build;
    }

    private static void addCulprits(AbstractBuild build, String... userIds) {
        LinkedHashSet<User> users = new LinkedHashSet<>(userIds.length);
        for (String userId : userIds) {
            users.add(user(userId));
        }
        when(build.getCulprits()).thenReturn(users);
    }

    private static User user(String id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
