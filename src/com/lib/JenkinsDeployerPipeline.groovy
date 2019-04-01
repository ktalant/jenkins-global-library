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
      choice(name: 'Docker images', choices: findDockerImages("fuchicorp"), description: 'Please select docker image to deploy!'),
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraformApply')

      ]
      )])
      checkout scm

      stage('Terraform init') {
        dir("${WORKSPACE}/deployment/terraform") {
          sh "terraform init"
        }
      }

      if (terraformApply == true) {
        stage('Apply Changes') {
          dir("${WORKSPACE}/deployment/terraform") {
            sh "terraform apply -var-file=webplatform.tfvars"
          }
        }

      } else {
        stage('Terraform Plan') {
          dir("${WORKSPACE}/deployment/terraform") {
            sh "terraform plan -var-file=webplatform.tfvars"
          }
        }
      }

  }
}

def findDockerImages(branchName) {

  def versionList = []
  def myJsonreader = new JsonSlurper()
  def nexusData = myJsonreader.parse(new URL("http://nexus.fuchicorp.com/service/rest/v1/components?repository=webplatform"))

  nexusData.items.each {
    if (it.name.contains(branchName)) {
       versionList.add(it.version)
    }
  }
  return versionList
}



return this
