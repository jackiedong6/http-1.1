# http-1.1

Welcome to Jackie Dong and Cesar Segura's implementation of an Http Server (HTTP/1.1)!

Notes on running the code:

- To run the code in this repository, it is suggested that you recompile the source files on your own machine.
- After compiling all of the source files, the command to run the server is "java server -config ". We have provided a default config file called httpd.conf in our repository
- Here are a couple of example commands that can be run from a separate terminal once the server is up and runnning:
    - curl -i -X POST -H "Host: cicada.cs.yale.edu" -H "Content-Type: application/x-www-form-urlencoded" http://localhost:6789/price.cgi
    - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/index.html
    - curl -i -H "Host: cicada.cs.yale.edu" -H "Accept: text/html" http://localhost:6789/test.html
      
Thank you for reviewing our code!
