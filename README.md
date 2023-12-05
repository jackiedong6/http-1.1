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
- Here are a couple of example commands that can be run from a separate terminal once the server is up and runnning:
    - curl -i -X POST -H "Host: cicada.cs.yale.edu" -H "Content-Type: application/x-www-form-urlencoded" http://localhost:6789/price.cgi
    - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/index.html
    - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/test.html
      
Thank you for reviewing our code!

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
