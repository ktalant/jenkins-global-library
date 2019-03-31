#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper

def runPipeline() {

  def environment = ""
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()

  switch(branch) {
    case 'master':
    environment = 'prod'

    case 'qa':
    environment = 'qa'

    case 'dev':
    environment = 'dev'

    default:
        print('This branch does not supported')
  }

  node('master') {
    properties([ parameters(

      [ choice(name: 'Docker images', choices: findDockerImages(branch), description: 'Please select docker image to deploy!')]

      )])
      stage('check docker image') {
          echo "${WORKSPACE}"
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
