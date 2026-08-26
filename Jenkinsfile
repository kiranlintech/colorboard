pipeline {

    agent any

    tools {
        maven 'Maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        skipDefaultCheckout(true)
    }

    parameters {
        choice(
            name: 'DEPLOY_TARGET',
            choices: ['HOMELAB', 'VPS'],
            description: 'Select deployment target'
        )

        booleanParam(
            name: 'SKIP_DEPLOY',
            defaultValue: false,
            description: 'Build and scan only. Do not deploy.'
        )
    }

    environment {

        /* =====================================================
         * APPLICATION
         * ===================================================== */

        APP_NAME = 'colorboard'

        /* Docker Hub repository */
        IMAGE_NAME = 'kiranlintech/colorboard'
        IMAGE_TAG      = "${BUILD_NUMBER}"

        /* Dockerfile */
        DOCKERFILE = 'backend/Dockerfile'


        /* =====================================================
         * DEPLOYMENT SERVERS
         * ===================================================== */

        DOCKER_CREDS   = 'dockerhub-credentials'
        VPS_SSH        = 'vps-ssh-key'
        MYLAB_SSH      = 'mylab-ssh'
        HOMELAB_HOST = '192.168.5.9'
        VPS_HOST     = '213.210.37.106'
        SSH_CREDENTIALS = 'vps-ssh-key'

        /* =====================================================
         * SECURITY / QUALITY
         * ===================================================== */

        SONAR_SERVER = 'sonarqube'
        SONAR_PROJECT_KEY = 'colorboard'
        SONAR_PROJECT_NAME = 'colorboard'


        /* =====================================================
         * JENKINS / BUILD
         * ===================================================== */

        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }


    stages {


        /* =====================================================
         * 1. CHECKOUT
         * ===================================================== */

        stage('Checkout') {

            steps {

                echo '=========================================='
                echo 'CHECKOUT SOURCE CODE'
                echo '=========================================='

                checkout scm

                sh '''
                    echo "Git branch:"
                    git branch --show-current || true

                    echo "Git commit:"
                    git rev-parse HEAD

                    echo "Project files:"
                    find . -maxdepth 2 -type f | sort | head -100
                '''
            }
        }


        /* =====================================================
         * 2. INITIALIZE BUILD VARIABLES
         * ===================================================== */

        stage('Initialize') {

            steps {

                script {

                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = "${BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"

                    echo "Application : ${env.APP_NAME}"
                    echo "Build       : ${BUILD_NUMBER}"
                    echo "Git Commit  : ${env.GIT_COMMIT_SHORT}"
                    echo "Image Tag   : ${env.IMAGE_TAG}"
                    echo "Deploy To   : ${params.DEPLOY_TARGET}"
                }
            }
        }


        /* =====================================================
         * 3. MAVEN TEST
         * ===================================================== */

        stage('Build & Test') {
            steps {
                dir('backend') {
                    sh 'mvn clean test package'
                }
            }

            post {
                always {
                    junit(
                    testResults: 'backend/target/surefire-reports/*.xml',
                    allowEmptyResults: true
                )
            }
        }
    }


        /* =====================================================
         * 4. OWASP DEPENDENCY CHECK
         * ===================================================== */

        stage('OWASP Dependency Check') {
            steps {
                echo '=========================================='
                echo 'OWASP DEPENDENCY CHECK'
                echo '=========================================='

                dependencyCheck(
                    odcInstallation: 'OWASP-Dependency-Check',
                    additionalArguments: '--scan . --disableAssembly'
                    )

                dependencyCheckPublisher(
                    pattern: 'dependency-check-report.xml'
                    )
                }
            }


        /* =====================================================
         * 5. SONARQUBE
         * ===================================================== */

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    withSonarQubeEnv('sonarqube') {
                    sh '''
                        mvn sonar:sonar \
                        -Dsonar.projectKey=colorboard \
                        -Dsonar.projectName=ColorBoard \
                        -Dsonar.java.binaries=target/classes
                        '''
                        }
                    }
                }
            }

        /* =====================================================
         * 6. SONAR QUALITY GATE
         * ===================================================== */

        stage('SonarQube Quality Gate') {
    steps {
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
                }
            }
        }    


        /* =====================================================
         * 7. PACKAGE APPLICATION
         * ===================================================== */

        stage('Package Application') {

            steps {

                echo '=========================================='
                echo 'PACKAGING APPLICATION'
                echo '=========================================='

                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }


        /* =====================================================
         * 8. DOCKER BUILD
         * ===================================================== */

        stage('Build Docker Image') {

            steps {

                echo '=========================================='
                echo 'BUILDING DOCKER IMAGE'
                echo '=========================================='

                sh """
                    docker build \
                        -f ${DOCKERFILE} \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest \
                        backend
                """

                sh """
                    docker images ${IMAGE_NAME}
                """
            }
        }


        /* =====================================================
         * 9. TRIVY SECURITY SCAN
         * ===================================================== */

        stage('Trivy Security Scan') {

            steps {

                echo '=========================================='
                echo 'TRIVY IMAGE SECURITY SCAN'
                echo '=========================================='

                sh '''
                    if ! command -v trivy >/dev/null 2>&1; then
                        echo "ERROR: Trivy is not installed on Jenkins agent."
                        exit 1
                    fi

                    trivy image \
                        --severity HIGH,CRITICAL \
                        --exit-code 1 \
                        --ignore-unfixed \
                        ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }


        /* =====================================================
         * 10. PUSH TO DOCKER HUB
         * ===================================================== */

        stage('Push to Docker Hub') {

            steps {

                echo '=========================================='
                echo 'PUSHING IMAGE TO DOCKER HUB'
                echo '=========================================='

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {

                    sh '''
                        set +x

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


        /* =====================================================
         * 11. DEPLOY
         * ===================================================== */

        stage('Deploy') {

            when {

                expression {
                    return !params.SKIP_DEPLOY
                }
            }

            steps {

                script {

                    def deployHost

                    if (params.DEPLOY_TARGET == 'HOMELAB') {

                        deployHost = env.HOMELAB_HOST

                    } else {

                        deployHost = env.VPS_HOST
                    }

                    echo '=========================================='
                    echo "DEPLOYING TO ${params.DEPLOY_TARGET}"
                    echo '=========================================='

                    echo "Target host: ${deployHost}"

                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: "${SSH_CREDENTIALS}",
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        )
                    ]) {

                        sh """
                            chmod 600 "\$SSH_KEY"

                            ssh -i "\$SSH_KEY" \
                                -o BatchMode=yes \
                                -o ConnectTimeout=10 \
                                -o StrictHostKeyChecking=no \
                                \$SSH_USER@${deployHost} '
                                    set -e

                                    echo "Pulling image..."

                                    docker pull ${IMAGE_NAME}:${IMAGE_TAG}

                                    echo "Stopping old container..."

                                    docker stop ${APP_NAME} || true

                                    echo "Removing old container..."

                                    docker rm ${APP_NAME} || true

                                    echo "Starting new container..."

                                    docker run -d \
                                        --name ${APP_NAME} \
                                        --restart unless-stopped \
                                        -p 8087:8080 \
                                        ${IMAGE_NAME}:${IMAGE_TAG}

                                    echo "Waiting for container..."

                                    sleep 10

                                    echo "Container status..."

                                    docker ps --filter name=${APP_NAME}

                                    echo "Checking container..."

                                    docker inspect \
                                        --format="{{.State.Status}}" \
                                        ${APP_NAME}
                                '
                        """
                    }
                }
            }
        }


        /* =====================================================
         * 12. DEPLOYMENT HEALTH CHECK
         * ===================================================== */

        stage('Health Check') {

            when {

                expression {
                    return !params.SKIP_DEPLOY
                }
            }

            steps {

                script {

                    def deployHost

                    if (params.DEPLOY_TARGET == 'HOMELAB') {

                        deployHost = env.HOMELAB_HOST

                    } else {

                        deployHost = env.VPS_HOST
                    }

                    echo '=========================================='
                    echo 'APPLICATION HEALTH CHECK'
                    echo '=========================================='

                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: "${SSH_CREDENTIALS}",
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        )
                    ]) {

                        sh """
                            ssh -i "\$SSH_KEY" \
                                -o BatchMode=yes \
                                -o ConnectTimeout=10 \
                                -o StrictHostKeyChecking=no \
                                \$SSH_USER@${deployHost} '

                                    set -e

                                    echo "Checking container..."

                                    docker ps --filter name=${APP_NAME}

                                    if [ "\$(docker inspect -f "{{.State.Running}}" ${APP_NAME})" != "true" ]; then

                                        echo "ERROR: Container is not running."

                                        docker logs ${APP_NAME} || true

                                        exit 1
                                    fi

                                    echo "Container is running successfully."
                                '
                        """
                    }
                }
            }
        }


        /* =====================================================
         * 13. SMOKE TEST
         * ===================================================== */

        stage('Smoke Test') {

            when {

                expression {
                    return !params.SKIP_DEPLOY
                }
            }

            steps {

                script {

                    def deployHost

                    if (params.DEPLOY_TARGET == 'HOMELAB') {

                        deployHost = env.HOMELAB_HOST

                    } else {

                        deployHost = env.VPS_HOST
                    }

                    echo '=========================================='
                    echo 'RUNNING SMOKE TEST'
                    echo '=========================================='

                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: "${SSH_CREDENTIALS}",
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        )
                    ]) {

                        sh """
                            ssh -i "\$SSH_KEY" \
                                -o BatchMode=yes \
                                -o ConnectTimeout=10 \
                                -o StrictHostKeyChecking=no \
                                \$SSH_USER@${deployHost} '

                                    echo "Testing application..."

                                    curl -f \
                                        --max-time 10 \
                                        http://127.0.0.1:8087/ || exit 1

                                    echo "Smoke test successful."
                                '
                        """
                    }
                }
            }
        }
    }


    /* =========================================================
     * POST ACTIONS
     * ========================================================= */

    post {

        success {

            echo """
            ==========================================
             colorboard DEPLOYMENT SUCCESSFUL
            ==========================================

            Application : ${env.APP_NAME}
            Image       : ${env.IMAGE_NAME}:${env.IMAGE_TAG}
            Environment : ${params.DEPLOY_TARGET}

            ==========================================
            """
        }

        failure {

            echo """
            ==========================================
             colorboard PIPELINE FAILED
            ==========================================

            Build       : ${env.BUILD_NUMBER}
            Application : ${env.APP_NAME}

            Check Jenkins logs for details.

            ==========================================
            """
        }

        always {

            echo 'Cleaning Jenkins workspace...'

            cleanWs(
                deleteDirs: true,
                disableDeferredWipeout: true
            )

            sh '''
                echo "Cleaning unused local Docker images..."

                docker image prune -f || true
            '''
        }
    }
}
