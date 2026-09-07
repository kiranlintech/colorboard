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

echo "===== PROJECT STRUCTURE ====="
find . -maxdepth 4 -type f | sort

echo "===== POM FILES ====="
find . -name "pom.xml" -type f
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

        stage('Build') {
            steps {
                script {
                    def pomPath = sh(
                        script: 'find . -name "pom.xml" -type f | head -1',
                        returnStdout: true
                    ).trim()

                    if (!pomPath) {
                        error("pom.xml not found in Jenkins workspace")
                    }

                    def pomDir = sh(
                        script: "dirname '${pomPath}'",
                        returnStdout: true
                    ).trim()

                    echo "Found pom.xml: ${pomPath}"
                    echo "Maven project directory: ${pomDir}"

                    dir(pomDir) {
                        sh 'mvn clean package -DskipTests'
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def pomPath = sh(
                        script: 'find . -name "pom.xml" -type f | head -1',
                        returnStdout: true
                    ).trim()

                    if (!pomPath) {
                        error("pom.xml not found in Jenkins workspace")
                    }

                    def pomDir = sh(
                        script: "dirname '${pomPath}'",
                        returnStdout: true
                    ).trim()

                    echo "Running SonarQube from: ${pomDir}"

                    dir(pomDir) {
                        withSonarQubeEnv('sonarqube') {
                            sh '''
mvn sonar:sonar \
-Dsonar.projectKey=colorboard \
-Dsonar.projectName=colorboard \
-Dsonar.exclusions=assets/**
                            '''
                        }
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
set -e

echo "===== Navigate to Colorboard ====="
cd ~/colorboard

echo "===== Pull latest backend image ====="
docker compose pull backend

echo "===== Deploy Colorboard stack ====="
docker compose up -d

echo "===== Deployment status ====="
docker compose ps
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
