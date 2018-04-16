## Integration Tests

This folder contains tests that integrate with a mocked server. By default, if no server is set up, 
they will be skipped. To run them we need to set up an sdk tester, either locally or on a remote 
server.

### Tester server

The tester can be found here: https://github.com/pusher/platform-sdk-tester (Private). If you are 
running it locally there is nothing else to set up. If you have a remote server when you have to 
provide the following values in your `~/.gradle.properties` or as `envvars`:

 - sdk_tester_url
 - sdk_tester_username (May be optional)
 - sdk_tester_password (May be optional)

