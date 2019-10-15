#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper
import hudson.FilePath


def runPipeline() {
  def environment = ""
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
  def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"

  environment = 'tools'
  // switch(branch) {
  //   case 'master': environment = 'tools'
  //   break
  //
  //   default:
  //       currentBuild.result = 'FAILURE'
  //       print('This branch does not supported')
  // }

  try {
    properties([ parameters([
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraform_apply'),
      booleanParam(defaultValue: false, description: 'Destroy deployment', name: 'terraform_destroy'),
      text(name: 'deployment_tfvars', defaultValue: 'deployment_name = "tools"', description: 'terraform configuration'),
      string(defaultValue: 'fuchicorp-google-service-account', name: 'common_service_account', description: 'Please enter service Account ID', trim: true)
      ]
      )])



      def slavePodTemplate = """
      metadata:
        labels:
          k8s-label: ${k8slabel}
        annotations:
          jenkinsjoblabel: ${env.JOB_NAME}-${env.BUILD_NUMBER}
      spec:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                - key: component
                  operator: In
                  values:
                  - jenkins-jenkins-master
              topologyKey: "kubernetes.io/hostname"
        containers:
        - name: docker
          image: docker:latest
          imagePullPolicy: Always
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
            - mountPath: /etc/secrets/service-account/
              name: google-service-account
        - name: fuchicorptools
          image: fuchicorp/buildtools
          imagePullPolicy: Always
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
            - mountPath: /etc/secrets/service-account/
              name: google-service-account
        securityContext:
          runAsUser: 0
          fsGroup: 0
        volumes:
          - name: google-service-account
            secret:
              secretName: fuchicorp-service-account
          - name: docker-sock
            hostPath:
              path: /var/run/docker.sock
    """

  podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate) {
      node(k8slabel) {
          stage('Generate Configurations') {
            // sh "sleep 200"
            sh " mkdir /deployment/terraform/ -p  && cat  /etc/secrets/service-account/credentials.json > ${WORKSPACE}/deployment/terraform/fuchicorp-service-account.json"
            def file = new File("${WORKSPACE}/deployment/terraform/deployment_configuration.tfvars")
            file.write "${deployment_tfvars}".stripIndent()
          }

          stage('Terraform Apply/Plan') {
            if (!params.terraform_destroy) {
              if (params.terraform_apply) {

                dir("${WORKSPACE}/deployment/terraform/") {
                  echo "##### Terraform Applying the Changes ####"
                  sh '''#!/bin/bash -e
                  source set-env.sh ./fuchicorp-common-tools.tfvars
                  terraform apply --auto-approve -var-file=$DATAFILE'''
                }

              } else {

                dir("${WORKSPACE}/deployment/terraform/") {
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
                  dir("${WORKSPACE}/deployment/terraform/") {
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
