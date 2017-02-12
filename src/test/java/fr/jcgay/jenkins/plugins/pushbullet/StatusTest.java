package fr.jcgay.jenkins.plugins.pushbullet;

import hudson.model.Result;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StatusTest {

    @Test
    public void get_status_from_build_result() {
        assertThat(Status.of(Result.SUCCESS)).isEqualTo(Status.SUCCESS);
        assertThat(Status.of(Result.FAILURE)).isEqualTo(Status.FAILURE);
        assertThat(Status.of(Result.ABORTED)).isEqualTo(Status.FAILURE);
        assertThat(Status.of(Result.NOT_BUILT)).isEqualTo(Status.FAILURE);
        assertThat(Status.of(Result.UNSTABLE)).isEqualTo(Status.FAILURE);
    }

    @Test
    public void a_pipeline_job_is_successful_when_result_is_null() {
        assertThat(Status.of(null)).isEqualTo(Status.SUCCESS);
    }
}
