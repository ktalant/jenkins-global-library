import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

def showUserInfor(jsonData) {
  def data = new JsonSlurper().parse(readFile(jsonData))
  data.each() {
    println("Users first name :${it['first_name']}, Last name :${it['last_name']}")
  }
}
