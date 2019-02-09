#!/usr/bin/env groovy
package com.lib

import groovy.json.*



def hello() {
  println("Hello World")
}



def result(data) {

  def jsonSlurper = new JsonSlurper()
  def reader = new BufferedReader(
  new InputStreamReader(
    new FileInputStream("${workspace}/" + data),"UTF-8")
    )

  resultData = jsonSlurper.parse(reader)
  if (resultData.data) {
    resultData.data.each() {
      println("Users first name :${it['first_name']}, Last name :${it['last_name']}")
    }
  }
}
