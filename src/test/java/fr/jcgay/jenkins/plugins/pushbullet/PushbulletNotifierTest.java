package fr.jcgay.jenkins.plugins.pushbullet;

import com.google.common.base.Function;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
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

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

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
    public void send_notification_to_the_user_who_has_launched_the_pipeline() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(runLaunchedBy("jc"), anyWorkspace(), anyLauncher(), anyTaskListener());

        verify(pushbullet).notify(any(Run.class), user.capture(), any(PrintStream.class));
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
    public void send_notification_to_the_users_configured_for_the_pipeline() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier("jc,toto,titi", pushbullet, getUserById);
        notifier.perform(anyRun(), anyWorkspace(), anyLauncher(), anyTaskListener());

        verify(pushbullet, times(3)).notify(any(Run.class), user.capture(), any(PrintStream.class));
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
    public void no_notification_when_no_real_user_has_been_found_in_pipeline() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(anyRun(), anyWorkspace(), anyLauncher(), anyTaskListener());

        verify(pushbullet, never()).notify(any(Run.class), any(User.class), any(PrintStream.class));
    }

    @Test
    public void send_notification_only_once_when_user_appears_in_multiple_source() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier("jc", pushbullet, getUserById);
        notifier.perform(buildLaunchedBy("jc", withCulprits("jc")), anyLauncher(), anyBuildListener());

        verify(pushbullet, times(1)).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    @Test
    public void send_notification_only_once_when_user_appears_in_multiple_source_in_pipeline() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier("jc", pushbullet, getUserById);
        notifier.perform(workflowRunLaunchedBy("jc", withChangesFrom("jc")), anyWorkspace(), anyLauncher(), anyTaskListener());

        verify(pushbullet, times(1)).notify(any(Run.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    @Test
    public void send_notification_when_job_has_been_triggered_by_another_job_initially_started_by_a_user() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(buildLaunchedByUpstream("jc"), anyLauncher(), anyBuildListener());

        verify(pushbullet, times(1)).notify(any(AbstractBuild.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    @Test
    public void send_notification_when_pipeline_has_been_triggered_by_another_job_initially_started_by_a_user() throws IOException, InterruptedException {
        notifier = new PushbulletNotifier(null, pushbullet, getUserById);
        notifier.perform(runLaunchedByUpstream("jc"), anyWorkspace(), anyLauncher(), anyTaskListener());

        verify(pushbullet, times(1)).notify(any(Run.class), user.capture(), any(PrintStream.class));
        assertThat(user.getValue().getId()).isEqualTo("jc");
    }

    private static Run<?, ?> runLaunchedByUpstream(String userId) {
        Cause.UserIdCause user = mock(Cause.UserIdCause.class);
        when(user.getUserId()).thenReturn(userId);

        Cause.UpstreamCause upstream = mock(Cause.UpstreamCause.class);
        when(upstream.getUpstreamCauses()).thenReturn(singletonList((Cause) user));

        Run run = mock(Run.class);
        when(run.getCause(Cause.UpstreamCause.class)).thenReturn(upstream);

        return run;
    }

    private static AbstractBuild buildLaunchedByUpstream(String userId) {
        Cause.UserIdCause user = mock(Cause.UserIdCause.class);
        when(user.getUserId()).thenReturn(userId);

        Cause.UpstreamCause upstream = mock(Cause.UpstreamCause.class);
        when(upstream.getUpstreamCauses()).thenReturn(singletonList((Cause) user));

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getCause(Cause.UpstreamCause.class)).thenReturn(upstream);

        return build;
    }

    private static String[] withCulprits(String userId) {
        return new String[]{userId};
    }

    private static String[] withChangesFrom(String userId) {
        return new String[]{userId};
    }

    private static BuildListener anyBuildListener() {
        BuildListener listener = mock(BuildListener.class);
        when(listener.getLogger()).thenReturn(mock(PrintStream.class));
        return listener;
    }

    private static TaskListener anyTaskListener() {
        TaskListener listener = mock(TaskListener.class);
        when(listener.getLogger()).thenReturn(mock(PrintStream.class));
        return listener;
    }

    private FilePath anyWorkspace() throws IOException {
        return new FilePath(temp.newFolder());
    }

    private static Launcher anyLauncher() {
        return mock(Launcher.class);
    }

    private static AbstractBuild anyBuild() {
        return mock(AbstractBuild.class);
    }

    private static Run<?, ?> anyRun() {
        return mock(Run.class);
    }

    private static Run<?, ?> runLaunchedBy(String userId) {
        Cause.UserIdCause user = mock(Cause.UserIdCause.class);
        when(user.getUserId()).thenReturn(userId);

        Run run = mock(Run.class);
        when(run.getCause(Cause.UserIdCause.class)).thenReturn(user);

        return run;
    }

    private static WorkflowRun workflowRunLaunchedBy(String userId, String... changeSetUserIds) {
        Cause.UserIdCause user = mock(Cause.UserIdCause.class);
        when(user.getUserId()).thenReturn(userId);

        WorkflowRun run = mock(WorkflowRun.class);
        when(run.getCause(Cause.UserIdCause.class)).thenReturn(user);

        if (changeSetUserIds != null) {
            addChangeSets(run, changeSetUserIds);
        }

        return run;
    }

    private static void addChangeSets(WorkflowRun build, String... userIds) {
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changes = new ArrayList<>(userIds.length);
        for (String userId : userIds) {
            User user = user(userId);
            ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
            when(entry.getAuthor()).thenReturn(user);
            changes.add(changeLog(build, entry));
        }

        when(build.getChangeSets()).thenReturn(changes);
    }

    private static ChangeLogSet<ChangeLogSet.Entry> changeLog(final WorkflowRun build, final ChangeLogSet.Entry entry) {
        return new ChangeLogSet<ChangeLogSet.Entry>(build, mock(RepositoryBrowser.class)) {
            @Override
            public Iterator<Entry> iterator() {
                return singleton(entry).iterator();
            }

            @Override
            public boolean isEmptySet() {
                return false;
            }
        };
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
