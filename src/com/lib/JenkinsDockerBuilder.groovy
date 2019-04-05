#!/usr/bin/env groovy

package com.lib
import groovy.json.JsonSlurper
import hudson.EnvVars


def runPipeline() {

  /**
  *  In feature we would like to create file build.yaml
  *  1. Read all required config from build.yaml
  *  2. Build the docker images
  *  3. Unitest the application
  *  4. Push the application
  */


  def commonDeployer = new com.lib.JenkinsDeployerPipeline()
  def repositoryName = "webplatform"
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()

  echo "The branch name is: ${branch}"

  switch(branch) {
    case 'master':
    repositoryName = repositoryName + '-prod'
    break

    case 'qa':
    repositoryName = repositoryName +  '-qa'
    break

    case 'dev':
    repositoryName = repositoryName + '-dev'
    break

    default:
        repositoryName = null
        currentBuild.result = 'FAILURE'
        print('You are using unsupported branch name')
  }


  /**
  * This library for now building image only to webplatform application
  */

  node {
    checkout scm
    def app
    stage('Copy kube config') {

      // Copy config files to WORKSPACE if does not exist
      sh 'if [ -f config ]; then  :; else cp -rf /fuchicorp/* ${WORKSPACE} ; fi'
    }

    stage('New release GIT') {

      // Get latest release from local git
      env.release = sh returnStdout: true, script: '''
      git fetch --tags --force
      git describe --abbrev=0 --tags'''
    }

    if (!commonDeployer.findDockerImages(branch).contains(env.release)) {

      stage('Build docker image') {

          // Build the docker image
          app = docker.build(repositoryName)
      }

      stage('Push image') {

         // Push image to the Nexus with new release
          docker.withRegistry('http://nexus.fuchicorp.com:8085', 'docker-private-credentials') {
              app.push("${env.release}")
              app.push("latest")
          }
       }
       stage('clean up') {
         sh "docker rmi *${repositoryName}* --force"
       }
      }
    }
}


return this
