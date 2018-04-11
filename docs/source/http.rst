=======================
 Http Message Handling
=======================

xio can handle both Http/1.1 and Http2 (h2) messages. 
To make things simple for the user these messages are abstracted behind the http.Request and http.Response interfaces.

Http/1.1 Abstraction
--------------------

Things to note:

* stream id is always -1

Http2 Abstraction
-----------------

Things to note:

* stream id

Full Messages vs Segmented Messages
-----------------------------------
