import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

def showUserInfo(jsonData) {
  def data = new JsonSlurper().parseText(readFile(jsonData))
  data.each() {
    println("Users first name :${it['first_name']}, Last name :${it['last_name']}")
  }
}
