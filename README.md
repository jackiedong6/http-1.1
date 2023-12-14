# http-1.1

Welcome to Jackie Dong and Cesar Segura's implementation of an Http Server (HTTP/1.1)!

Notes on running the code:

- To run the code in this repository, it is suggested that you recompile the source files on your own machine.
- After compiling all of the source files, the command to run the server is "
    - java server -config httpd.conf
- We have provided a default config file called httpd.conf in our repository# http-1.1

Welcome to Jackie Dong and Cesar Segura's implementation of an Http Server (HTTP/1.1)!

Notes on running the code:

- To run the code in this repository, it is suggested that you recompile the source files on your own machine.
- After compiling all of the source files, the command to run the server is "
  - java server -config httpd.conf
- We have provided a default config file called httpd.conf in our repository
- Here are a couple of example commands that can be run from a separate terminal once the server is up and runnning:
  - curl -i -X POST -H "Host: cicada.cs.yale.edu" -H "Content-Type: application/x-www-form-urlencoded" -H "Content-Length: 15" -d "item1=A&item2=B" "http://localhost:6789/price.cgi?company=appl"
  - curl -i -X POST -H "Host: cicada.cs.yale.edu" -H "Content-Type: application/x-www-form-urlencoded" -H "Content-Length: 15" -d "item1=A&item2=B" "http://localhost:6789/price-stdin-raw.cgi?company=appl"
  - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/index.html
  - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/test.html

Thank you for reviewing our code!

Report: Netty Comparison
a. Netty provides multiple event loop implementations. In a typical server channel setting, two event loop groups are created, with one typically called the boss group and the second worker group. What are they? How does Netty achieve synchronization among them?

Boss Group: This is responsible for accepting incoming connections. In a server, the boss group's main job is to create new channels (connections) and pass them to the worker group.
Worker Group: Once a connection is established, the worker group handles the traffic of the accepted connection, performing I/O operations, reading data, and writing responses.
Synchronization:
Netty achieves synchronization between these groups through efficient threading and task scheduling. The event loop groups manage an internal pool of threads, and tasks are assigned to these threads in a way that ensures thread safety and efficient execution.
Event loops are bound to a single thread, and all I/O operations and events for a particular channel are handled exclusively by the same event loop (and thus the same thread). This design minimizes the need for synchronization since most operations happen within the context of a single thread.


b. Netty's IO Ratio Based Scheduling Algorithm
Netty's event loop uses an ioRatio based scheduling algorithm to balance time spent on I/O operations and non-I/O tasks.

ioRatio Setting: The ioRatio represents the percentage of time that the event loop will dedicate to processing I/O tasks. The rest of the time is spent on non-I/O tasks.
Operation: During each iteration of the event loop, it checks the ioRatio setting. If ioRatio is set to 80, for example, the event loop will spend 80% of its time handling I/O operations (like reading from or writing to channels) and the remaining 20% on non-I/O tasks (like executing user-defined tasks or scheduled tasks).
Purpose: This approach ensures a balanced distribution of time and resources between I/O and non-I/O activities, helping to maintain overall application performance and responsiveness.


c.
The HttpHelloWorldServer, found in Netty's examples, is structured to send back the content of the received HTTP request in a simple plaintext form. The server is set up with a boss group and a worker group for handling incoming connections and subsequent I/O tasks. The main class, HttpHelloWorldServer, configures the server, including setting up the SSL context (if SSL is enabled) and initializing the server with a specific ChannelInitializer, namely HttpHelloWorldServerInitializer.

The HttpHelloWorldServerHandler, which extends SimpleChannelInboundHandler<HttpObject>, is responsible for handling the incoming HTTP requests. It processes the HttpRequest and sends back a response with the content "Hello World". The response handling includes checking for keep-alive headers and writing the response back to the client.

HttpSnoopServerHandler
The HttpSnoopServerHandler extends SimpleChannelInboundHandler<Object> and is more complex compared to the Hello World example. It is designed to process both HttpRequest and HttpContent messages.
This handler builds a detailed response including information like request version, hostname, URI, headers, and query parameters. It also handles content of the HTTP request, appending it to the response.
It includes detailed logging for each part of the request and response, making it useful for snooping or inspecting the traffic that passes through the server.

d.

e. ByteBuf vs. ByteBuffer
ByteBuf: Netty's ByteBuf is an advanced alternative to Java's ByteBuffer.
Key Difference: One major difference is the way they handle read and write operations. ByteBuf maintains two separate pointers for read and write operations, allowing for more efficient read/write manipulation without the need to call flip() like in ByteBuffer. This dual-pointer approach simplifies the usage and improves performance, especially in network operations where read and write operations are frequent.

Report: nginx Design
- a. The load balancing that occurs among worker threads relies heavily on the operating system's kernel. The kernel decides which worker process should accept a new connection. The contention is resolved using a mutex. On the welcome socket (the socket that takes in accept requests), the mutex allows the worker processes to accept new connections by turn.
- b. I would handle the 3-second timeout requirement as follows. Each time that a worker thread accepts a new connection, I would add a 3-second timeout timer to the connection. Then, I would create a timer handler that checks if the connection's timer has expired, which would then trigger the the connection to be dropped. Whenever a request is sent over the connection, I would reset the timer.
- c. Here are the 11 phases of the nginx HTTP server:
  - NGX_HTTP_POST_READ_PHASE: ngx_http_core_generic_phase()
  - NGX_HTTP_SERVER_REWRITE_PHASE: ngx_http_core_rewrite_phase()
  - NGX_HTTP_FIND_CONFIG_PHASE: ngx_http_core_find_config_phase()
  - NGX_HTTP_REWRITE_PHASE: ngx_http_core_rewrite_phase()
  - NGX_HTTP_POST_REWRITE_PHASE: ngx_http_core_post_rewrite_phase()
  - NGX_HTTP_PREACCESS_PHASE: ngx_http_core_generic_phase()
  - NGX_HTTP_ACCESS_PHASE: ngx_http_core_access_phase()
  - NGX_HTTP_POST_ACCESS_PHASE: ngx_http_core_post_access_phase()
  - NGX_HTTP_PRECONTENT_PHASE: ngx_http_core_generic_phase()
  - NGX_HTTP_CONTENT_PHASE: ngx_http_core_content_phase()
  - NGX_HTTP_LOG_PHASE: ngx_http_core_log_phase()
- d. The high-level design is as follows. Upstream servers are first defined in the configuration file along with directives that determine how the server will will process requests and proxy them to upstream servers. The keepalive module extends upstream functionality. Upstream configuration can be explicit in the configuration file or implicit through directives like proxy_pass. Configuration parameters, such as max_fails and fail_timeout, can be specified, and the module distinguishes explicitly defined upstreams from those automatically created by directives like proxy_pass. Load-balancing algorithms must initialize methods in the ngx_http_upstream_peer_t object, and the module uses a set of methods for server selection, including get(), free(), notify(), set_session(), and save_session(). These methods handle tasks such as obtaining server addresses, releasing resources after server usage, and managing SSL sessions.
- e. There are several differences between ngx_buf_t and ByteBuffer. For one, ByteBuffer is much higher level and more abstract than ngx_buf_t in several ways. ngx_buf_t is designed specifically for an nginx server, while ByteBuffer can be utilized in many I/O scenarios. Also, ngx_buf_t is more of a reference to some location in memory, while ByteBuffer often uses native memory.



**Netcat Testing:**\
nc -c localhost 6789\
**Basic Headers**
1. ```GET / HTTP/1.1```
2. ```
   GET / HTTP/1.1
   Host: cicada.cs.yale.edu
   ```
3. ```
   GET /test.html HTTP/1.1
   Host: mobile.cicada.cs.yale.edu
   Authorization: Basic amFja2llOmNwc2M0MzQ=
   ```
4. ```
   GET / HTTP/1.1
   If-Modified-Since: Mon, 23 Oct 1999 04:57:34 GMT
   ```
5. ```
   GET / HTTP/1.1
   If-Modified-Since: Mon, 23 Oct 2023 04:57:35 GMT
   ```
6. ```
   GET / HTTP/1.1
   User-Agent: \"Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 Mobile/14E5239e Safari/602.1\""
   ``` \

**Post Request**
1. ```
   POST / HTTP/1.1
   Content-Type: application/x-www-form-urlencoded
   Content-Length: 15
   
   item1=A&item2=B
   
   ```
**Error Checking**
1. ```GET /slakghaklh HTTP/1.1```
2. ```GET / HTTP/1.2```

**Performance Testing**\
``ab -n 10000 -c 10 http://localhost:6789/``
\
![](performance.png)
We have a transfer rate of 3245.74 Kbytes / sec.

Thank you for reviewing our code!

**Program Structure**
Our HTTP/1.x program server structure is structured to be modular, with functionality broken into several key components. 


ChannelHandler (ChannelHandler.java): An interface defining a generic handler for channel-related events, including exception handling.

AcceptHandler (AcceptHandler.java): Defines an interface for handling new connections. It extends the ChannelHandler interface and includes a method handleAccept to process accepted connections.

Acceptor (Acceptor.java): Implements the AcceptHandler interface. It's responsible for accepting new connections, creating ReadWriteHandler instances for each

HTTPInfo (HTTPMessage.java, HTTPRequest.java, HTTPResponse.java): Defines classes for HTTP messages, requests, and responses, encapsulating the data and headers for HTTP communication.

HTTP1xReadWriteHandler and HTTP1xReadWriteHandlerFactory: These components handle HTTP 1.x read and write operations. The factory class creates instances of the handler.

Our server spawns n multiplexing loops determined by our configuration file. Our select loop design is symmetric, with each dispatcher thread handling accepting requests, reading requests, and writing requests. Each thread utilizes java's non-blocking NIO channels to multiplex ready i/o tasks. 

Our server also utilizes a management thread that listens for operator entered management commands. The terminal supports the minimal command: shutdown, which disables the server from accepting any new requests, but will continue to process the existing requests until all requests are finished.

In addition, our server supports timeout functionality. If a new connection does not give a complete request to the server approximately within our timeout value of three seconds, from the time of being accepted, the server should disconnect the connection. 

**Execution**

We don't utilize as many states as NGINX does in its request parsing. However, our read write handler does  utilize a state machine with the following states.
```
private enum State {
  READ_REQUEST,
  READ_BODY,
  PARSE_REQUEST,
  RESPONSE_READY,
  RESPONSE_SENT,
}
```