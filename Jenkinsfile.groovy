dockerRegistry = "ghcr.io"
githubRepo = "icgc-argo/kship"
def commit = "UNKNOWN"
def version = "UNKNOWN"
pipeline {
    agent {
        kubernetes {
            label 'kship-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: openjdk:11-jdk
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
  - name: docker
    image: docker:18-git
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
      privileged: true
      runAsUser: 0
    volumeMounts:
    - name: docker-graph-storage
      mountPath: /var/lib/docker
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
"""
        }
    }
    stages {

        // get the commit and version number for current release
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = sh(returnStdout: true, script: "cat ./.mvn/maven.config | grep revision | cut -d '=' -f2").trim()
                }
            }
        }

        // run tests and package
        stage('Test') {
            steps {
                container('jdk') {
                    // remove the snapshot and append the commit (the dot before ${commit} is intentional)
                    // this does NOT publish to a maven artifacts store
                    sh "./mvnw -Dsha1=.${commit} -Dchangelist=-${BUILD_NUMBER} test package"
                }
            }
        }

        // publish the edge tag
        stage('Publish Develop') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "docker login ${dockerRegistry} -u $USERNAME -p $PASSWORD"
                    }

                    // the network=host needed to download dependencies using the host network (since we are inside 'docker'
                    // container)
                    sh "docker build --network=host . -t ${dockerRegistry}/${githubRepo}:edge -t ${dockerRegistry}/${githubRepo}:${version}-${commit}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:${version}-${commit}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:edge"
                }
            }
        }

        stage('Release & Tag') {
            when {
                branch "master"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'argoGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${githubRepo} --tags"
                    }

                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "docker login ${dockerRegistry} -u $USERNAME -p $PASSWORD"
                    }
                    // DNS error if --network is default
                    sh "docker build --network=host . -t ${dockerRegistry}/${githubRepo}:latest -t ${dockerRegistry}/${githubRepo}:${version}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:${version}"
                    sh "docker push ${dockerRegistry}/${githubRepo}:latest"
                }
            }
        }

    }
}