# Jenkins Pushbullet Plugin

A Jenkins plugin to report build status with [Pushbullet](https://www.pushbullet.com).

## Installation

### Build

Build the plugin locally with Maven:

    > mvn package

The plugin binary will be available in `target/pushbullet.hpi`.

## Usage

### Activate Pushbullet notification for a job

Add `Report build status with Pusbullet` as a `Post Build Action` in your Jenkins job:

![Job Configuration](https://jeanchristophegay.com/images/jenkins-pushbullet-plugin-job-configuration.png)

### Each user must set its Pushbullet access token

To send notification each user must defines its access token accessible from [here](https://www.pushbullet.com/account) in Jenkins user settings (`http://jenkins/user/{me}/configure`).  

![User Configuration](https://jeanchristophegay.com/images/jenkins-pushbullet-plugin-user-configuration.png)

If a user has not set an access token, Jenkins will not try to send him push notifications.

### Notified users

By default a push notification will be send to each user present in the build culprits list (as documented [here](http://javadoc.jenkins.io/hudson/model/AbstractBuild.html#getCulprits())).  
Also if a user has launched a build manually, he will receive a notification.  
Eventually users can be added in the job configuration `Report build status with Pushbullet`, these ones will be notified for every build. `Users` is a string of comma separated Jenkins user IDs (`toto,tata,titi` for example).

# Build

## Status

[![Build Status](https://travis-ci.org/jcgay/jenkins-pushbullet-plugin.png)](https://travis-ci.org/jcgay/jenkins-pushbullet-plugin)

## Release

    mvn -B release:prepare release:perform