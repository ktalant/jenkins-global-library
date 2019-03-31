#!/usr/bin/env groovy
package com.lib

def runPipeline() {

  branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
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





return this
