#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper


def runPipeline() {


  def environment = ""
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
  def messanger = new com.lib.JenkinsNotificator()
  def slackChannel = "devops"

  switch(branch) {
    case "master": environment = "prod"
    branch = "prod"
    break

    case "qa": environment = "qa"
    break

    case "dev": environment = "dev"
    break

    default:
        currentBuild.result = 'FAILURE'
        print('This branch does not supported')
  }

  try {
    properties([ parameters([
      choice(name: 'SelectedDockerImage', choices: findDockerImages(branch), description: 'Please select docker image to deploy!'),
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraformApply'),
      booleanParam(defaultValue: false, description: 'Destroy deployment', name: 'terraformDestroy'),
      string( defaultValue: 'webplatform', name: 'mysql_database', value: 'dbwebplatform', description: 'Please enter database name'),
      string(defaultValue: 'webplatformUser',  name: 'mysql_user',description: 'Please enter a username for MySQL', trim: true),
      string(defaultValue: 'webplatformPassword',  name: 'mysql_password',description: 'Please enter a password for MySQL', trim: true),
      string(defaultValue: 'fuchicorp-google-service-account', name: 'common_service_account', description: 'Please enter service Account ID', trim: true)
      ]
      )])

      node('master') {
        withCredentials([
          file(credentialsId: "${common_service_account}", variable: 'common_user')]) {
            messanger.sendMessage("slack", "STARED", slackChannel)
            stage('Poll code') {
              checkout scm
              sh """#!/bin/bash -e
              cp -rf ${common_user} ${WORKSPACE}/fuchicorp-service-account.json
              """
            }

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
            credentials               =  "./fuchicorp-service-account.json"
            """
          }

          stage('Terraform Apply/Plan') {
            if (!params.terraformDestroy) {
              if (params.terraformApply) {

                dir("${WORKSPACE}/deployment/terraform") {
                  echo "##### Terraform Applying the Changes ####"
                  sh '''#!/bin/bash -e
                  source set-env.sh ./deployment/terraform/webplatform.tfvars
                  terraform apply --auto-approve -var-file=$DATAFILE'''
                  messanger.sendMessage("slack", "APPLIED", slackChannel)
                }

              } else {

                  dir("${WORKSPACE}/deployment/terraform") {
                    echo "##### Terraform Plan (Check) the Changes #####"
                    sh '''#!/bin/bash -e
                    source set-env.sh ./deployment/terraform/webplatform.tfvars
                    terraform plan -var-file=$DATAFILE'''
                    messanger.sendMessage("slack", "PLANED", slackChannel)
                  }

              }
            }
          }
          stage('Terraform Destroy') {
            if (!params.terraformApply) {
              if (params.terraformDestroy) {
                if ( branch == 'dev' || branch == 'qa' ) {
                  dir("${WORKSPACE}/deployment/terraform") {
                    echo "##### Terraform Destroing #####"
                    sh "terraform destroy --auto-approve -var-file=webplatform.tfvars"
                    messanger.sendMessage("slack", "DESTROYED", slackChannel)
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

  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
    messanger.sendMessage("slack", "FAILURE", slackChannel)
  }
}

import groovy.json.JsonSlurper


def findDockerImages(branchName) {
  def versionList = []
  def token       = ""
  def myJsonreader = new JsonSlurper()
  def nexusData = myJsonreader.parse(new URL("https://nexus.fuchicorp.com/service/rest/v1/components?repository=webplatform"))
  while (true) {
    if (nexusData.continuationToken) {
      token = nexusData.continuationToken
      nexusData = myJsonreader.parse(new URL("https://nexus.fuchicorp.com/service/rest/v1/components?repository=webplatform&continuationToken=${token}"))
      nexusData.items.each {
        if (it.name.contains(branchName)) {
           versionList.add(it.name + ':' + it.version)
         }
        }
    }
    if (nexusData.continuationToken == null ){
      break
    }

  }


  return versionList.sort()
}

return this
