@Grab(group='org.codehaus.groovy.modules.http-builder',
      module='http-builder', version='0.7' )
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import com.xjeffrose.xio.*
import spock.lang.*

class HttpServerSpock extends Specification {

  def "HttpServer sends valid response for GET"() {
    when:
      def server = new Server()
      server.addRoute("/", new PooService());
      server.serve(8080)
      def http = new HTTPBuilder('http://localhost:8080');
      def html = http.get( path : '/')
      server.close()

    then:
      html.response == "Hello from /poo"

  }

}
