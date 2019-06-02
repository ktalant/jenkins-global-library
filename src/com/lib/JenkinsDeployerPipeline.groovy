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
          messanger.sendMessage("slack", "STARED", "#devops")

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
              sh "terraform init"
            }
          }

          stage('Terraform Apply/Plan') {
            if (!params.terraformDestroy) {
              if (params.terraformApply) {

                dir("${WORKSPACE}/deployment/terraform") {
                  echo "##### Terraform Applying the Changes #####"
                  sh "terraform apply --auto-approve -var-file=webplatform.tfvars"
                  messanger.sendMessage("slack", "APPLYED", "#devops")
                }

              } else {

                  dir("${WORKSPACE}/deployment/terraform") {
                    echo "##### Terraform Plan (Check) the Changes #####"
                    sh "terraform plan -var-file=webplatform.tfvars"
                    messanger.sendMessage("slack", "PLANED", "#devops")
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
                    messanger.sendMessage("slack", "DESTROYED", "#devops")
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

  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
    messanger.sendMessage("slack", "FAILURE", "#devops")
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

return this
