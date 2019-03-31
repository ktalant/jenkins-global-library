#!/usr/bin/env groovy
package com.lib

def runPipeline() {
  def branchName = "${JOB_NAME}"
  println("${branchName}")
}



return this
