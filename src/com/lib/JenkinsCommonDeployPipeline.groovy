#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper


def runPipeline() {

  def environment = ""
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()

  switch(branch) {
    case 'master': environment = 'tools'
    break

    default:
        currentBuild.result = 'FAILURE'
        print('This branch does not supported')
  }

  try {
    properties([ parameters([
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraform_apply'),
      booleanParam(defaultValue: false, description: 'Destroy deployment', name: 'terraform_destroy'),
      string(defaultValue: 'common-tools-tfvars',  name: 'params_tfvars_id', description: 'Please give tfvars secret ID', trim: true),
      string(defaultValue: 'fuchicorp-common-service-account', name: 'common_service_account', description: 'Please enter service Account ID', trim: true)
      ]
      )])

    withCredentials([
      file(credentialsId: "common-tools-tfvars", variable: 'deployment_fvars'),
      file(credentialsId: "${common_service_account}", variable: 'common_user')]) {
      node('master') {
        stage('testing') {
          sh "ls ${common_user}"
          sh "ls ${deployment_fvars}"
        }
       //    checkout scm
       //    stage('Terraform init') {
       //      dir("${WORKSPACE}/deployment/terraform") {
       //        sh "source set-env.sh ${deployment_fvars}"
       //      }
       //    }
       //
       //    stage('Terraform Apply/Plan') {
       //      if (!params.terraform_destroy) {
       //        if (params.terraform_apply) {
       //
       //          dir("${WORKSPACE}/") {
       //            echo "##### Terraform Applying the Changes ####"
       //            sh 'terraform apply --auto-approve -var-file=$DATAFILE'
       //          }
       //
       //        } else {
       //
       //          dir("${WORKSPACE}/") {
       //            echo "##### Terraform Plan (Check) the Changes #### "
       //            sh "terraform plan -var-file=$DATAFILE"
       //          }
       //        }
       //      }
       //    }
       //    stage('Terraform Destroy') {
       //      if (!params.terraform_apply) {
       //        if (params.terraform_destroy) {
       //          if ( branch != 'tools' ) {
       //            dir("${WORKSPACE}/") {
       //              echo "##### Terraform Destroing ####"
       //              sh 'terraform destroy --auto-approve -var-file=$DATAFILE'
       //            }
       //          } else {
       //            println("""
       //
       //              Sorry I can not destroy PROD!!!
       //              I can Destroy only dev and qa branch
       //
       //            """)
       //          }
       //        }
       //     }
       //
       //     if (params.terraform_destroy) {
       //       if (params.terraform_apply) {
       //         println("""
       //         Sorry you can not destroy and apply at the same time
       //         """)
       //       }
       //   }
       // }
     }
   }

  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
  }
}



return this
