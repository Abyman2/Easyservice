pipeline {
    agent any

    tools {
        jdk 'JDK21'
        maven 'Maven3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir('backend') {
                    sh 'mvn clean test'
                }
            }
        }

        stage('Code Coverage Check') {
            steps {
                dir('backend') {
                    sh 'mvn jacoco:check'
                }
            }
        }

        stage('Publish Reports') {
            steps {
                dir('backend') {
                    jacoco execPattern: 'target/jacoco.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java'
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
