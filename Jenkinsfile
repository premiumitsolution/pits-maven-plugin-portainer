pipeline {
  agent any
  tools {
    maven 'Maven 3.8.1'
    jdk 'jdk11'
  }

  environment {
    MS_TEAMS_HOOK_URL = credentials('MSTeamsJenkinsHookURL')
  }

  stages {
    stage('Check environment') {
      steps{
        sh 'mvn -v'
        sh 'java --version'
      }
    }

    stage('Build') {
      steps {
         withMaven( maven: 'Maven 3.8.1'){
          sh '''
            mvn clean package
          '''
        }
      }
    }

    stage('Deploy') {
      when { expression { return env.BRANCH_NAME == 'develop'} }
      steps {
        withMaven( maven: 'Maven 3.8.1'){
          sh '''
            mvn deploy
          '''
        }
      }
    }

  }

  post {
      always {
          office365ConnectorSend webhookUrl: "$MS_TEAMS_HOOK_URL"
      }
    }
}