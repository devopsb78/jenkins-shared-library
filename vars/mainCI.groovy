def call() {
    node('ci-server') {

        stage('CodeCheckout') {

            sh "find ."
            sh "find . | sed -e '1d' |xargs rm -rf"
            if(env.TAG_NAME ==~ ".*") {
                env.branch_name = "refs/tags/${env.TAG_NAME}"
            } else {
                env.branch_name = "${env.BRANCH_NAME}"
            }
            checkout scmGit(
                    branches: [[name: "${branch_name}"]],
                    userRemoteConfigs: [[url: "https://github.com/devopsb78/expense-${component}"]]
            )
        }

        if (env.TAG_NAME ==~ '.*') {
            stage('Build Code') {
                sh 'docker build -t 368761340104.dkr.ecr.us-east-1.amazonaws.com/expense-${component}:${TAG_NAME} .'
            }
            stage('Release Software') {
                sh 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 368761340104.dkr.ecr.us-east-1.amazonaws.com'
                sh 'docker push  368761340104.dkr.ecr.us-east-1.amazonaws.com/expense-${component}:${TAG_NAME}'
            }
            stage('Deploy to Dev') {
                sh 'aws eks update-kubeconfig --name dev-eks'
                sh 'argocd login $(kubectl get svc -n argocd argocd-server | awk \'{print $4}\' | tail -1) --username admin --password $(argocd admin initial-password -n argocd | head -1) --insecure --grpc-web'
                sh 'argocd app set ${component} --parameter appVersion=${TAG_NAME}'
                sh 'argocd app sync ${component}'
            }
        }
        else {
            stage('Lint Code') {
                print 'OK'
            }
            if(env.BRANCH_NAME != 'main') {
                stage('Run Unit Tests') {
                    print 'OK'
                }
                stage('Run Integration Tests') {
                    print 'OK'
                }
            }
            stage('Sonar Scan Code Review') {
                print 'OK'
            }
        }

    }

}