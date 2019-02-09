import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

def showUserInfo(jsonData) {
  def slurper = new JsonSlurper()
  def data = new slurper.parse(new File("json/data.json"))
  data.each() {
    println("Users first name :${it['first_name']}, Last name :${it['last_name']}")
  }
}
