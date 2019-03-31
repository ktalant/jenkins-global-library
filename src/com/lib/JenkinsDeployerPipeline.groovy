#!/usr/bin/env groovy
package com.lib

def runPipeline() {
  def branchName = "${JOB_NAME}"
  branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
  println("${branch}")
}



return this
