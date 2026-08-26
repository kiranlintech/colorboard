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

        // Application
        APP_NAME = 'colorboard'
        IMAGE_NAME = 'kiranlintech/colorboard'
        DOCKERFILE = 'backend/Dockerfile'

        // Deployment servers
        HOMELAB_HOST = '192.168.5.9'
        VPS_HOST = '213.210.37.106'

        HOMELAB_SSH = 'mylab-ssh'
        VPS_SSH = 'vps-ssh-key'

        // Jenkins credentials
        DOCKER_CREDS = 'dockerhub-credentials'
        DB_CREDS = 'db-key'

        // SonarQube
        SONAR_SERVER = 'sonarqube'
        SONAR_PROJECT_KEY = 'colorboard'
        SONAR_PROJECT_NAME = 'ColorBoard'

        // Maven
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Initialize') {
            steps {
                script {
                    def shortCommit = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = "${BUILD_NUMBER}-${shortCommit}"

                    echo "Build      : ${BUILD_NUMBER}"
                    echo "Commit     : ${shortCommit}"
                    echo "Image      : ${IMAGE_NAME}:${env.IMAGE_TAG}"
                    echo "Target     : ${params.DEPLOY_TARGET}"
                }
            }
        }

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

        stage('OWASP Dependency Check') {
            steps {

                dependencyCheck(
                    odcInstallation: 'OWASP-Dependency-Check',
                    additionalArguments: '--scan . --disableAssembly'
                )

                dependencyCheckPublisher(
                    pattern: 'dependency-check-report.xml'
                )
            }
        }

        stage('SonarQube Analysis') {
            steps {

                dir('backend') {

                    withSonarQubeEnv("${SONAR_SERVER}") {

                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectName=${SONAR_PROJECT_NAME} \
                                -Dsonar.java.binaries=target/classes
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {

                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate(
                        abortPipeline: true
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {

                sh """
                    docker build \
                        -f ${DOCKERFILE} \
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest \
                        backend
                """
            }
        }

        stage('Trivy Scan') {
            steps {

                sh '''
                    if ! command -v trivy >/dev/null 2>&1; then
                        echo "ERROR: Trivy is not installed."
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

        stage('Push to Docker Hub') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: "${DOCKER_CREDS}",
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

            when {
                expression {
                    return !params.SKIP_DEPLOY
                }
            }

            steps {

                script {

                    def deployHost
                    def sshCredential

                    if (params.DEPLOY_TARGET == 'HOMELAB') {
                        deployHost = env.HOMELAB_HOST
                        sshCredential = env.HOMELAB_SSH
                    } else {
                        deployHost = env.VPS_HOST
                        sshCredential = env.VPS_SSH
                    }

                    echo "Deploying ${IMAGE_NAME}:${IMAGE_TAG}"
                    echo "Target: ${params.DEPLOY_TARGET}"

                    withCredentials([

                        sshUserPrivateKey(
                            credentialsId: sshCredential,
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        ),

                        usernamePassword(
                            credentialsId: "${DB_CREDS}",
                            usernameVariable: 'DB_USER',
                            passwordVariable: 'DB_PASSWORD'
                        )

                    ]) {

                        sh(
                            script: """
                                set -e

                                chmod 600 "\$SSH_KEY"

                                echo "Preparing database configuration..."

                                printf '%s\\\\n' \\
                                    "DB_USER=\$DB_USER" \\
                                    "DB_PASSWORD=\$DB_PASSWORD" \\
                                    "SPRING_DATASOURCE_USERNAME=\$DB_USER" \\
                                    "SPRING_DATASOURCE_PASSWORD=\$DB_PASSWORD" \\
                                    "SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/colorboard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \\
                                    "DB_URL=jdbc:mysql://mysql:3306/colorboard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \\
                                    | ssh -i "\$SSH_KEY" \\
                                        -o StrictHostKeyChecking=no \\
                                        "\$SSH_USER@${deployHost}" \\
                                        'cat > /tmp/colorboard.env'


                                ssh -i "\$SSH_KEY" \\
                                    -o StrictHostKeyChecking=no \\
                                    "\$SSH_USER@${deployHost}" '
                                    
                                    set -e

                                    echo "Pulling Docker image..."

                                    docker pull ${IMAGE_NAME}:${IMAGE_TAG}


                                    echo "Checking Docker network..."

                                    if ! docker network inspect colorboard-net >/dev/null 2>&1; then
                                        docker network create colorboard-net
                                    fi


                                    echo "Checking MySQL..."

                                    if ! docker inspect colorboard-mysql >/dev/null 2>&1; then
                                        echo "ERROR: colorboard-mysql container not found."
                                        rm -f /tmp/colorboard.env
                                        exit 1
                                    fi


                                    if [ "\$(docker inspect -f "{{.State.Running}}" colorboard-mysql)" != "true" ]; then
                                        echo "ERROR: colorboard-mysql is not running."
                                        rm -f /tmp/colorboard.env
                                        exit 1
                                    fi


                                    echo "Connecting MySQL to application network..."

                                    docker network connect \
                                        --alias mysql \
                                        colorboard-net \
                                        colorboard-mysql 2>/dev/null || true


                                    echo "Stopping old application..."

                                    docker stop ${APP_NAME} 2>/dev/null || true
                                    docker rm ${APP_NAME} 2>/dev/null || true


                                    echo "Starting new application..."

                                    docker run -d \
                                        --name ${APP_NAME} \
                                        --restart unless-stopped \
                                        --network colorboard-net \
                                        -p 8087:8080 \
                                        --env-file /tmp/colorboard.env \
                                        ${IMAGE_NAME}:${IMAGE_TAG}


                                    rm -f /tmp/colorboard.env


                                    echo "Application started."


                                    echo "Waiting for application health..."

                                    HEALTH_OK=false

                                    for i in \$(seq 1 18); do

                                        if curl -fsS \
                                            --max-time 5 \
                                            http://127.0.0.1:8087/api/tasks/health \
                                            >/dev/null 2>&1; then

                                            echo "Application is healthy."

                                            HEALTH_OK=true
                                            break
                                        fi

                                        echo "Waiting for application... \$i/18"

                                        sleep 5

                                    done


                                    if [ "\$HEALTH_OK" != "true" ]; then

                                        echo "ERROR: Application did not become healthy."

                                        echo ""
                                        echo "Application logs:"

                                        docker logs --tail 100 ${APP_NAME} || true

                                        exit 1
                                    fi


                                    echo "Deployment successful."

                                '
                            """,
                            label: "Deploy ColorBoard"
                        )
                    }
                }
            }
        }
    }

    post {

        success {
            echo """
            ==========================================
            COLORBOARD DEPLOYMENT SUCCESSFUL
            ==========================================

            Image       : ${env.IMAGE_NAME}:${env.IMAGE_TAG}
            Environment : ${params.DEPLOY_TARGET}

            ==========================================
            """
        }

        failure {
            echo """
            ==========================================
            COLORBOARD PIPELINE FAILED
            ==========================================

            Build       : ${env.BUILD_NUMBER}
            Application : ${env.APP_NAME}

            Check Jenkins logs for details.

            ==========================================
            """
        }

        always {
            cleanWs(
                deleteDirs: true,
                disableDeferredWipeout: true
            )
        }
    }
}
