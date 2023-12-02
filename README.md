# http-1.1

Welcome to Jackie Dong and Cesar Segura's implementation of an Http Server (HTTP/1.1)!

Notes on running the code:

- To run the code in this repository, it is suggested that you recompile the source files on your own machine.
- After compiling all of the source files, the command to run the server is "
    - java server -config httpd.conf
- We have provided a default config file called httpd.conf in our repository
- Here are a couple of example commands that can be run from a separate terminal once the server is up and runnning:
    - curl -i -X POST -H "Host: cicada.cs.yale.edu" -H "Content-Type: application/x-www-form-urlencoded" http://localhost:6789/price.cgi
    - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/index.html
    - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/test.html
      
Thank you for reviewing our code!

Report: nginx Design
- a. The contention is resolved using a mutex. On the welcome socket (the socket that takes in accept requests), the mutex allows the worker processes to accept new connections by turn. The load balancing that occurs among worker threads relies heavily on the operating system's kernel. The kernel decides which worker process should accept a new connection. 
- b. I would handle the 3-second timeout requirement as follows. Each time that a worker thread accepts a new connection, I would add a 3-second timeout timer to the connection. Then, I would create a timer handler that checks if the connection's timer has expired, which would then trigger the the connection to be dropped. Whenever a request is sent over the connection, I would reset the timer. 
- c. Here are the 11 phases of the nginx HTTP server:
    - NGX_HTTP_POST_READ_PHASE 
    - NGX_HTTP_SERVER_REWRITE_PHASE 
    - NGX_HTTP_FIND_CONFIG_PHASE 
    - NGX_HTTP_REWRITE_PHASE
    - NGX_HTTP_POST_REWRITE_PHASE
    - NGX_HTTP_PREACCESS_PHASE
    - NGX_HTTP_ACCESS_PHASE
    - NGX_HTTP_POST_ACCESS_PHASE
    - NGX_HTTP_PRECONTENT_PHASE
    - NGX_HTTP_CONTENT_PHASE
    - NGX_HTTP_LOG_PHASE
- d. The high-level design is as follows. Upstream servers are first defined in the configuration file along with directives that determine how the server will will process requests and proxy them to upstream servers. The keepalive module extends upstream functionality. Upstream configuration can be explicit in the configuration file or implicit through directives like proxy_pass. Configuration parameters, such as max_fails and fail_timeout, can be specified, and the module distinguishes explicitly defined upstreams from those automatically created by directives like proxy_pass. Load-balancing algorithms must initialize methods in the ngx_http_upstream_peer_t object, and the module uses a set of methods for server selection, including get(), free(), notify(), set_session(), and save_session(). These methods handle tasks such as obtaining server addresses, releasing resources after server usage, and managing SSL sessions.
- e. There are several differences between ngx_buf_t and ByteBuffer. For one, ByteBuffer is much higher level and more abstract than ngx_buf_t in several ways. ngx_buf_t is designed specifically for an nginx server, while ByteBuffer can be utilized in many I/O scenarios. Also, ngx_buf_t is more of a reference to some location in memory, while ByteBuffer often uses native memory. 
