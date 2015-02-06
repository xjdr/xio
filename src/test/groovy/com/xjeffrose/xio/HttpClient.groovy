import com.xjeffrose.xio.*
import spock.lang.*

class HttpClientSpock extends Specification {

  def "HttpClient gets valid response for GET"() {
    when:
      def server = new Server()
      server.addRoute("/", new PooService())
      server.serve(8080)
      def client = new Client()
      client.defaultRequest()
      def f = client.get(8080)
      def resp = f.get()

    then:
      resp.ok == true
      server.close()

  }

  // def "HttpServer sends valid response for POST"() {
  //   when:
  //     def client = new Client();
  //     def f = c.get('localhost:8080')
  //     def resp = f.get()
  //
  //   then:
  //   resp.ok == true
  //
  // }

}
