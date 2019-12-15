
# Temporary Solutions and Functionality

### Board recognition

The automatic board recognition is currently initiated using an operating system call. We did it because it was the easiest way to do it. However, a better practice would be importing the main file of the board recognition into our app.py file so that no OS calls would have to be made.

### Controlling the phone app through the web application
Currently, the web application is used to tell the server when player has made his/her move. This is achieved by sending a POST request to the server. The server notices incoming POST requests and updates the state variables. At the same time, the phone app sends GET requests at certain intervals to the server to find out when a new picture should be sent to the server. When the state variables are altered, the phone sees that it's time to send a new picture. This way the app can be used without the user interacting with the phone during the game.
This approach should be replaced in the future, because it burdens the server with unnecessary requests. A push notification system might be the best way to do this.


### CORS and proxy
At the moment, the server does not return Access-Control-Allow-Origin header. This raises [Cross-origin resource sharing](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) error. As a workaround for this issue, the web-application uses a proxy, which is configured in the package.json file. In addition to the one line that has been inserted to package.json, the .env file must also be edited to get the web application work temporarily. While using the proxy, the backend server addresses in the .env file must be left blank. Once the server has been updated to return the correct header information, the proxy can be deleted from package.json and the server addresses can be defined in the .env file.

