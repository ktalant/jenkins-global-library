#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper

def runPipeline() {

  def environment = ""
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()

  switch(branch) {
    case 'master': environment = 'prod'
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
      choice(defaultValue: "webplatform-${environment}", name: 'SelectedDockerImage', choices: findDockerImages(branch), description: 'Please select docker image to deploy!'),
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraformApply'),
      string(name: 'mysql_database', value: 'dbwebplatform', description: 'Please enter database name')

      ]
      )])
      checkout scm
      stage('Generate Vars') {
        def file = new File("${WORKSPACE}/deployment/terraform/webplatform.tfvars")
        file.write """
        mysql_user              =  "${branch}"
        mysql_database          =  "${mysql_database}"
        mysql_host              =  "webplatform-mysql"
        webplatform_namespace   =  "${environment}"
        webplatform_image       =  "nexus.fuchicorp.com:8085/${SelectedDockerImage}"
        """
      }

      stage('Terraform init') {
        dir("${WORKSPACE}/deployment/terraform") {
          sh "terraform init"
        }
      }

      if (params.terraformApply) {
        stage('Apply Changes') {
          dir("${WORKSPACE}/deployment/terraform") {
            sh "terraform apply  --auto-approve  -var-file=webplatform.tfvars"
            sh 'ls'
          }
        }

      } else {
        stage('Terraform Plan') {
          dir("${WORKSPACE}/deployment/terraform") {
            sh "terraform plan -var-file=webplatform.tfvars"
          }
        }
      }

      stage('Clean up') {
         sh "rm -rf ${WORKSPACE}/deployment/terraform"
      }

  }
}

def findDockerImages(branchName) {

  def versionList = []
  def myJsonreader = new JsonSlurper()
  def nexusData = myJsonreader.parse(new URL("http://nexus.fuchicorp.com/service/rest/v1/components?repository=webplatform"))

  if (branchName.toLowerCase() == 'master') {
    branchName = 'prod'
  }

  nexusData.items.each {
    if (it.name.contains(branchName)) {
       versionList.add(it.name + ':' + it.version)
    } else {
      versionList.add('NONE')
    }
  }
  return versionList
}



return this
