pipeline {
  agent {
    kubernetes {
      label 'jenkins-slave1'
      defaultContainer 'jnlp'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: busybox
    image: busybox
    command:
    - cat
    tty: true
  - name: sbt
    image: mozilla/sbt
    alwaysPullImage: true
    command:
    - cat
    tty: true
  - name: dind
    image: docker:18.09-dind
    securityContext:
      privileged: true
  - name: docker
    env:
    - name: DOCKER_HOST
      value: 127.0.0.1
    image: vvavepacket/infra-docker
    command:
    - cat
    tty: true
  - name: tools
    image: argoproj/argo-cd-ci-builder:v1.0.1
    command:
    - cat
    tty: true
"""
    }
  }
  stages {
    stage('Compile') {
      steps {
        container('sbt') {
          sh 'sbt compile'
        }
      }
    }

    stage('Build Docker') {
      environment {
        AWS_ACCESS_KEY_ID = credentials('ECR_AWS_ACCESS_KEY_ID')
        AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
      }
      steps {
        container('sbt') {
          sh 'sbt docker:stage'
        }
        container('docker') {
          // Configure aws creds
          sh "echo '$AWS_ACCESS_KEY_ID\n$AWS_SECRET_ACCESS_KEY\nus-east-1\ntable' | ~/bin/aws configure"
          // Authenticate docker client to ecr registry
          sh "\$(~/bin/aws ecr get-login --no-include-email --region us-east-1)"
          // Build new image
          sh "until docker ps; do sleep 3; done && docker build -t goxhere/miniapp -f ./miniapp-impl/target/docker/stage/Dockerfile ./miniapp-impl/target/docker/stage"
          // Tag the image
          sh "docker tag goxhere/miniapp:latest 510158725010.dkr.ecr.us-east-1.amazonaws.com/goxhere/miniapp:${env.GIT_COMMIT}"
          // Publish new image
          sh "docker push 510158725010.dkr.ecr.us-east-1.amazonaws.com/goxhere/miniapp:${env.GIT_COMMIT}"
        }
      }
    }

    stage('Deploy Test') {
      steps {
        container('tools') {
          sshagent (credentials: ['61952383-2418-46b9-a4da-47f6ae109fc9']) {
            sh "ssh -o StrictHostKeyChecking=no git@bitbucket.org"
            sh "git clone git@bitbucket.org:goxhere/miniapp-deploy.git"
            sh "git config --global user.email 'ci@goxhere.com'"
            sh "git config --global user.name 'jenkins'"
          }
          sh "cd ./miniapp-deploy/test/miniapp && kustomize edit set image 510158725010.dkr.ecr.us-east-1.amazonaws.com/goxhere/miniapp:${env.GIT_COMMIT}"
          sshagent (credentials: ['61952383-2418-46b9-a4da-47f6ae109fc9']) {
            sh "cd ./miniapp-deploy && git commit -am 'Publish new version in Test' && git push || echo 'no changes'"
          }
        }
      }
    }

    stage('Prod Approval'){
        steps {
            input message 'Deploy to Prod?'
        }
    }

    stage('Deploy Prod') {
      steps {
        container('tools') {
          sshagent (credentials: ['61952383-2418-46b9-a4da-47f6ae109fc9']) {
            sh "ssh -o StrictHostKeyChecking=no git@bitbucket.org"
            sh "git clone git@bitbucket.org:goxhere/miniapp-deploy.git"
            sh "git config --global user.email 'ci@goxhere.com'"
            sh "git config --global user.name 'jenkins'"
          }
          sh "cd ./miniapp-deploy/prod/miniapp && kustomize edit set image 510158725010.dkr.ecr.us-east-1.amazonaws.com/goxhere/miniapp:${env.GIT_COMMIT}"
          sshagent (credentials: ['61952383-2418-46b9-a4da-47f6ae109fc9']) {
            sh "cd ./miniapp-deploy && git commit -am 'Publish new version in Prod' && git push || echo 'no changes'"
          }
        }
      }
    }
  }
}