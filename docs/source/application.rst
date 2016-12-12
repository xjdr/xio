=============
 Application
=============

The :java:extdoc:`Application<com.xjeffrose.xio.application.Application>`
object holds all of the global state and configuration data across
multiple server objects. It should be configured and instantiated by
an :java:extdoc:`ApplicationBootstrap<com.xjeffrose.xio.bootstrap.ApplicationBootstrap>`
instance.

.. code-block:: java
   :linenos:

   Application application = new ApplicationBootstrap("example.application")
     .addServer("echo", (serverBootstrap) -> serverBootstrap.addToPipeline(new EchoPipeline()))
     .addServer("http", (serverBootstrap) -> serverBootstrap.addToPipeline(new HttpPipeline()))
     .build();

Each application will be created with a
:ref:`configuration server <configuration_server>` which may be used
to update :ref:`dynamic configuration <dynamic_configuration>` values
while the application is running.
