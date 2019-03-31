#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper

def runPipeline() {

  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()

  switch(branch) {
    case 'master':
    println('This will go to prod')

    case 'qa':
    println('This will go to qa')

    case 'dev':
    println('This will go to qa')

    default:
        print('This branch does not supported')
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
