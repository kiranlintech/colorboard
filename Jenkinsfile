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
        BACKEND_IMAGE = "kiranlintech/colorboard"
        NGINX_IMAGE   = "kiranlintech/colorboard-nginx"

        IMAGE_TAG = "${BUILD_NUMBER}"

        HOMELAB_HOST = "192.168.5.9"
        VPS_HOST     = "213.210.37.106"
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/kiranlintech/colorboard.git'

                sh '''
                    echo "===== Workspace ====="
                    pwd

                    echo "===== Project Structure ====="
                    find . -maxdepth 3 -type f | sort
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
                sh '''
                    cd backend
                    mvn clean package -DskipTests
                '''
            }
        }


        stage('SonarQube Analysis') {
            steps {

                script {

                    withSonarQubeEnv('sonarqube') {

                        sh '''
                            cd backend

                            mvn sonar:sonar \
                              -Dsonar.projectKey=colorboard \
                              -Dsonar.projectName=colorboard \
                              -Dsonar.exclusions=assets/**
                        '''
                    }
                }
            }
        }


        stage('Build Backend Docker Image') {
            steps {

                sh '''
                    docker build \
                      -f docker/Dockerfile \
                      -t ${BACKEND_IMAGE}:${IMAGE_TAG} \
                      .
                '''
            }
        }


        stage('Build NGINX Docker Image') {
            steps {

                sh '''
                    docker build \
                      -f nginx/Dockerfile \
                      -t ${NGINX_IMAGE}:${IMAGE_TAG} \
                     .
                '''
            }
        }


        stage('Trivy Scan') {
            steps {

                sh '''
                    echo "===== Backend Image Scan ====="

                    trivy image \
                      --exit-code 0 \
                      --severity HIGH,CRITICAL \
                      ${BACKEND_IMAGE}:${IMAGE_TAG}


                    echo "===== NGINX Image Scan ====="

                    trivy image \
                      --exit-code 0 \
                      --severity HIGH,CRITICAL \
                      ${NGINX_IMAGE}:${IMAGE_TAG}
                '''
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

                        echo "===== Push Backend ====="

                        docker push ${BACKEND_IMAGE}:${IMAGE_TAG}
                        
                        echo "===== Push NGINX ====="

                        docker push ${NGINX_IMAGE}:${IMAGE_TAG}
                        
                        docker logout
                    '''
                }
            }
        }


        stage('Deploy') {
            steps {

                script {

                    def target = params.DEPLOY_TARGET == 'HOMELAB' ?
                                 "ubuntu@${HOMELAB_HOST}" :
                                 "ubuntu@${VPS_HOST}"

                    sh """
                        ssh -o StrictHostKeyChecking=no ${target} '
                            set -e

                            echo "======================================"
                            echo "Colorboard Deployment"
                            echo "======================================"

                            cd /home/ubuntu/colorboard

                            echo "===== Pull latest images ====="

                            docker compose pull


                            echo "===== Start Colorboard ====="

                            docker compose up -d


                            echo "===== Deployment Status ====="

                            docker compose ps


                            echo "===== Container Status ====="

                            docker ps --filter name=colorboard
                        '
                    """
                }
            }
        }
    }


    post {

        success {
            echo "======================================"
            echo "Deployment successful"
            echo "Target: ${params.DEPLOY_TARGET}"
            echo "Build: ${BUILD_NUMBER}"
            echo "======================================"
        }

        failure {
            echo "Pipeline failed. Check the logs."
        }

        always {
            cleanWs()
        }
    }
}
