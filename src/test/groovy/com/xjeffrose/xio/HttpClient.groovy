import com.xjeffrose.xio.*
import spock.lang.*
import java.nio.*


class HttpClient extends Specification {

    def "HttpClient sends correct request"() {
        when:
        def s = new Server()
        s.serve(8080)

        def c = new Client()
        c.get(8080)

        def response = c.response()

        then:
        response.method() == "GET"
        response.uri() == "/"
        response.httpVersion() == "HTTP/1.1"
        response.headers.headerMap.size() == 3
        response.headers.get("User-Agent") == "curl/7.35.0"
        response.headers.get("Host") == "localhost:8000"
    }
}
