#!/usr/bin/env groovy
package com.lib


def branchName = "${JOB_NAME}"

def runPipeline() {
  println("${branchName}")
}
