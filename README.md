# Jenkins Pushbullet Plugin

A Jenkins plugin to report build status with [Pushbullet](https://www.pushbullet.com).

## Installation

Download [pushbullet.hpi](https://bintray.com/jcgay/maven/download_file?file_path=fr%2Fjcgay%2Fjenkins%2Fplugins%2Fpushbullet%2F0.2.0%2Fpushbullet-0.2.0.hpi) and install it manually in Jenkins (`http://jenkins/pluginManager/advanced`).

## Usage

### Each user must set its Pushbullet access token

To receive notification each user must define its access token accessible from [here](https://www.pushbullet.com/account) in Jenkins user settings (`http://<jenkins.url>/me/configure`).  

![User Configuration](https://jeanchristophegay.com/images/jenkins-pushbullet-plugin-user-configuration.png)

If a user has not set an access token, Jenkins will not try to send him push notifications.

### Notified users

By default a push notification will be send to each user present in the build culprits list (as documented [here](http://javadoc.jenkins.io/hudson/model/AbstractBuild.html#getCulprits())).  
Also if a user has launched a build manually, he will receive a notification.  
Eventually users can be added in the job configuration `Report build status with Pushbullet`, these ones will be notified for every build. `Users` is a string of comma separated Jenkins user IDs (`toto,tata,titi` for example).

### Activate Pushbullet notification for a job

#### Freestyle Job

Add `Report build status with Pusbullet` as a `Post Build Action` in your Jenkins job:

![Job Configuration](https://jeanchristophegay.com/images/jenkins-pushbullet-plugin-job-configuration.png)

#### Pipeline script

To report a build state in a pipeline script, you'll need to handle errors.  
Basically you'll need to wrap your build sript in a `try/catch` (see [Documentation](https://github.com/jenkinsci/workflow-basic-steps-plugin/blob/master/CORE-STEPS.md#plain-catch-blocks))

```
node {
    try {
        sh 'might fail'
        pushbullet users:''
    } catch (e) {
        pushbullet users:''
        throw e
    }
}
```

#### Declarative pipeline

With declarative pipeline there is a post action that will be called before exiting the build.

```
pipeline {
    agent any
    stages {
        stage("Build") {
            steps {
                sh 'might fail'
            }
        }
    }
    post {
        always {
            pushbullet users: ''
        }
    }
}
```

# Build

Build the plugin locally with Maven:

    > mvn package

The plugin binary will be available in `target/pushbullet.hpi`.

## Status

[![Build Status](https://travis-ci.org/jcgay/jenkins-pushbullet-plugin.png)](https://travis-ci.org/jcgay/jenkins-pushbullet-plugin)

## Release

    mvn -B release:prepare release:perform