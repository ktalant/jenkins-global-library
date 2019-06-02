#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper
import hudson.FilePath


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
      string(defaultValue: 'fuchicorp-google-service-account', name: 'common_service_account', description: 'Please enter service Account ID', trim: true)
      ]
      )])

      node('master') {
        withCredentials([
          file(credentialsId: "${params_tfvars_id}", variable: 'deployment_fvars'),
          file(credentialsId: "${common_service_account}", variable: 'common_user')]) {
            stage('Poll code') {
              checkout scm
              sh """#!/bin/bash -e
              cp -rf ${common_user} ${WORKSPACE}/fuchicorp-service-account.json
              cp -rf ${deployment_fvars} ${WORKSPACE}/fuchicorp-common-tools.tfvars
              """
            }

          stage('Terraform Apply/Plan') {
            if (!params.terraform_destroy) {
              if (params.terraform_apply) {

                dir("${WORKSPACE}/") {
                  echo "##### Terraform Applying the Changes ####"
                  sh '''#!/bin/bash -e
                  source set-env.sh ./fuchicorp-common-tools.tfvars
                  terraform apply --auto-approve -var-file=$DATAFILE'''
                }

              } else {

                dir("${WORKSPACE}/") {
                  echo "##### Terraform Plan (Check) the Changes #### "
                  sh '''#!/bin/bash -e
                  source set-env.sh ./fuchicorp-common-tools.tfvars
                  terraform plan -var-file=$DATAFILE'''
                }
              }
            }
          }
          stage('Terraform Destroy') {
            if (!params.terraform_apply) {
              if (params.terraform_destroy) {
                if ( environment != 'tools' ) {
                  dir("${WORKSPACE}/") {
                    echo "##### Terraform Destroing ####"
                    sh '''#!/bin/bash -e
                    source set-env.sh ./fuchicorp-common-tools.tfvars
                    terraform destroy --auto-approve -var-file=$DATAFILE'''
                  }
                } else {
                  println("""

                    Sorry I can not destroy Tools!!!
                    I can Destroy only dev and qa branch

                  """)
                }
              }
           }

           if (params.terraform_destroy) {
             if (params.terraform_apply) {
               println("""

               Sorry you can not destroy and apply at the same time

               """)
               currentBuild.result = 'FAILURE'
            }
         }
       }
     }
   }

  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
  }
}



return this
