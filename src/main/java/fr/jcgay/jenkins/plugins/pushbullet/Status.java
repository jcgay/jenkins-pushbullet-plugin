package fr.jcgay.jenkins.plugins.pushbullet;

import hudson.model.Result;

import java.net.URL;

public enum Status {

    SUCCESS("/dialog-clean.png", "Success"),
    FAILURE("/dialog-error-5.png", "Failure");

    private final String icon;
    private final String message;

    private Status(String icon, String message) {
        this.icon = icon;
        this.message = message;
    }

    public String message() {
        return message;
    }

    public URL url() {
        return getClass().getResource(icon);
    }

    public static Status of(Result result) {
        if (result == Result.SUCCESS) {
            return SUCCESS;
        }
        return FAILURE;
    }
}
