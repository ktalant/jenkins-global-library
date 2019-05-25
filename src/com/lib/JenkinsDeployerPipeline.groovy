#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper


def runPipeline() {

  def environment = ""
  def endpoint    = "academy.fuchicorp.com"
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
  def salckChannel = 'test-message'

  // slackUrl = 'https://fuchicorp.slack.com/services/hooks/jenkins-ci/'
  // slackTokenId = 'slack-token'

  switch(branch) {
    case 'master': environment = 'prod'
    branch = 'prod'
    break

    case 'qa': environment = 'qa'
    break

    case 'dev': environment = 'dev'
    break

    default:
        currentBuild.result = 'FAILURE'
        print('This branch does not supported')
  }

  node('master') {
    properties([ parameters([
      choice(name: 'SelectedDockerImage', choices: findDockerImages(branch), description: 'Please select docker image to deploy!'),
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraformApply'),
      booleanParam(defaultValue: false, description: 'Destroy deployment', name: 'terraformDestroy'),
      string( defaultValue: 'webplatform', name: 'mysql_database', value: 'dbwebplatform', description: 'Please enter database name'),
      string(defaultValue: 'webplatformUser',  name: 'mysql_user',description: 'Please enter a username for MySQL', trim: true),
      string(defaultValue: 'webplatformPassword',  name: 'mysql_password',description: 'Please enter a password for MySQL', trim: true)

      ]
      )])
      checkout scm
      // notifyStarted()
      stage('Generate Vars') {
        def file = new File("${WORKSPACE}/deployment/terraform/webplatform.tfvars")
        file.write """
        mysql_user                =  "${mysql_user}"
        mysql_database            =  "${mysql_database}"
        mysql_host                =  "webplatform-mysql"
        webplatform_namespace     =  "${environment}"
        webplatform_password      =  "${mysql_password}"
        webplatform_image         =  "docker.fuchicorp.com/${SelectedDockerImage}"
        environment               =  "${environment}"
        """
      }

      stage('Terraform init') {
        dir("${WORKSPACE}/deployment/terraform") {
          sh "source get-setenv.sh"
        }
      }

      stage('Terraform Apply/Plan') {
        if (!params.terraformDestroy) {
          if (params.terraformApply) {

            dir("${WORKSPACE}/deployment/terraform") {
              echo "##### Terraform Applying the Changes ####"
              sh "terraform apply  --auto-approve  -var-file=webplatform.tfvars"
            }

          } else {

              dir("${WORKSPACE}/deployment/terraform") {
                echo "##### Terraform Plan (Check) the Changes ####"
                sh "terraform plan -var-file=webplatform.tfvars"
              }

          }
        }
      }
      stage('Terraform Destroy') {
        if (!params.terraformApply) {
          if (params.terraformDestroy) {
            if ( branch == 'dev' || branch == 'qa' ) {
              dir("${WORKSPACE}/deployment/terraform") {
                echo "##### Terraform Destroing ####"
                sh "terraform destroy --auto-approve -var-file=webplatform.tfvars"
              }
            } else {
              println("""

              Sorry I can not destroy PROD!!!
              I can Destroy only dev and qa branch

              """)
            }
          }
       }

       if (params.terraformDestroy) {
         if (params.terraformApply) {
           println("""
           Sorry you can not destroy and apply at the same time
           """)
         }
     }
   }
 }
}

def findDockerImages(branchName) {
  def versionList = []
  def myJsonreader = new JsonSlurper()
  def nexusData = myJsonreader.parse(new URL("https://nexus.fuchicorp.com/service/rest/v1/components?repository=webplatform"))
  nexusData.items.each {
    if (it.name.contains(branchName)) {
       versionList.add(it.name + ':' + it.version)
     }
    }


  if (versionList.isEmpty()) {
    return ['ImageNotFound']
  }

  return versionList
}


def notifyStarted() {
    slackSend (color: '#FFFF00', baseUrl : "${slackUrl}".toString(), tokenCredentialId: "${slackTokenId}".toString(),
    message: """
    ## Please add let team know if this is mistake or please send an email
    STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}).
    email: fuchicorpsolution@gmail.com
    """)
}

def notifySuccessful() {
    slackSend (color: '#00FF00', baseUrl : "${slackUrl}".toString(), tokenCredentialId: "${slackTokenId}".toString(),
    message: """
    ## Jenkins Job was successfully built. #######
    SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})
    email: fuchicorpsolution@gmail.com
    #
    """)
}

def notifyFailed() {
    slackSend (color: '#FF0000', baseUrl : "${slackUrl}".toString(),  tokenCredentialId: "${slackTokenId}".toString(),
    message: """
    ## Jenkins build is breaking for some reason. Please go to job and take actions.
    FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    email: fuchicorpsolution@gmail.com
    """)
}

return this
