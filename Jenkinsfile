@Library('forge-shared-library')_

pipeline {
    agent {
        docker {
            image 'gradle:7-jdk16'
        }
    }
    environment {
        GRADLE_ARGS = '-Dorg.gradle.daemon.idletimeout=5000'
    }

    stages {
        stage('buildandtest') {
            steps {
                withGradle {
                    sh './gradlew ${GRADLE_ARGS} --refresh-dependencies --continue build test'
                }
                script {
                    gradleVersion(this)
                }
            }
            post {
                success {
                    writeChangelog(currentBuild, 'build/changelog.txt')
                    //archiveArtifacts artifacts: 'build/changelog.txt', fingerprint: false
                }
            }
        }
        stage('publish') {
            when {
                not {
                    changeRequest()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'maven-forge-user', usernameVariable: 'MAVEN_USER', passwordVariable: 'MAVEN_PASSWORD')]) {
                    withGradle {
                        sh './gradlew ${GRADLE_ARGS} publish'
                    }
                }
            }
             post {
                success {
                    build job: 'filegenerator', parameters: [string(name: 'COMMAND', value: "promote ${env.MYGROUP}:${env.MYARTIFACT} ${env.MYVERSION} latest")], propagate: false, wait: false
                }
            }
        }
    }
}