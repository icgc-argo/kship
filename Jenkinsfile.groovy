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
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
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

// BEGINNING OF TEST BLOCK
// DELETE BEFORE PR
        stage('Test publish to ghcr.io') {
            when {
                branch "Docker-image-ghcr-migration"
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
// END OF TEST BLOCK

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
                    withCredentials([usernamePassword(credentialsId: 'argoContainers', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${githubRepo} --tags"
                    }

                    withCredentials([usernamePassword(credentialsId:'argoDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
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