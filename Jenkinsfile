pipeline {
agent any

parameters {
    choice(
        name: 'DEPLOY_TARGET',
        choices: ['HOMELAB', 'VPS'],
        description: 'Select deployment target'
    )
}

environment {
    IMAGE_NAME = "kiranlintech/colorboard"
    IMAGE_TAG  = "${BUILD_NUMBER}"

    HOMELAB_HOST = "192.168.5.9"
    VPS_HOST     = "213.210.37.106"
}

stages {

    stage('Checkout') {
        steps {
            git branch: 'main',
                url: 'https://github.com/kiranlintech/colorboard.git'

            sh '''
                echo "===== WORKSPACE ====="
                pwd
                ls -la

                echo "===== PROJECT FILES ====="
                find . -maxdepth 2 -type f | sort | head -100
            '''
        }
    }

    stage('OWASP Dependency Check') {
        steps {
            dependencyCheck(
                additionalArguments: '--scan ./',
                odcInstallation: 'OWASP-Dependency-Check'
            )

            dependencyCheckPublisher(
                pattern: '**/dependency-check-report.xml'
            )
        }
    }

    stage('SonarQube Analysis') {
        steps {
            script {

                def scannerHome = tool 'sonar-scanner'

                withSonarQubeEnv('sonarqube') {

                    sh """
                    ${scannerHome}/bin/sonar-scanner \
                    -Dsonar.projectKey=colorboard \
                    -Dsonar.projectName=colorboard \
                    -Dsonar.sources=. \
                    -Dsonar.inclusions=**/*.jsp,**/*.java,WEB-INF/** \
                    -Dsonar.exclusions=assets/**
                    """
                }
            }
        }
    }

    stage('Build Docker Image') {
        steps {
            sh """
                docker build \
                -f docker/Dockerfile \
                -t ${IMAGE_NAME}:${IMAGE_TAG} .

                docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
            """
        }
    }

    stage('Trivy Scan') {
        steps {
            sh """
                trivy image \
                --exit-code 0 \
                --severity HIGH,CRITICAL \
                ${IMAGE_NAME}:${IMAGE_TAG}
            """
        }
    }

    stage('Push to Docker Hub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login \
                            --username "$DOCKER_USERNAME" \
                            --password-stdin

                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest

                        docker logout
                    '''
                }
            }
        }

    stage('Deploy') {
        steps {
            script {

                def target = params.DEPLOY_TARGET == "HOMELAB" ?
                             "ubuntu@${HOMELAB_HOST}" :
                             "ubuntu@${VPS_HOST}"

                sh """
                ssh -o StrictHostKeyChecking=no ${target} '

                    docker pull ${IMAGE_NAME}:latest

                    docker stop colorboard || true
                    docker rm colorboard || true

                    docker image prune -f

                    docker run -d \
                      --name colorboard \
                      --restart unless-stopped \
                      -p 8082:8080 \
                      ${IMAGE_NAME}:latest
                '
                """
            }
        }
    }
}

post {

    success {
        echo "Deployment successful to ${params.DEPLOY_TARGET}"
    }

    failure {
        echo "Pipeline failed. Check logs for details."
    }

    always {
        cleanWs()
    }
}

}
