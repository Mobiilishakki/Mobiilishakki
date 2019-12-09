
# Temporary Solutions and Functionality

### Board Recognizion
At the moment the image taken with the phone and sent to server is cropped by the user to match the board, so it can be processed easier by the server, just splitting it to 64 smaller images for each square on the board by using dimensions of the original image.
In future a proper computer vision functionality could be implemented, either using it to improve the accuracy of splitting the images, or even replacing the cropping completely and recognizing the board automatically for the player. 
NOTE! [research.md](https://github.com/Mobiilishakki/Mobiilishakki/blob/master/Documentation/reseach.md) in documentation, and the difficulties associated. 

### Controlling the Phone-app through Web-application
Currently, the web-application is used to tell the server when player has made his/her move. This is achieved by sending POST-reguest to server. The server notices incoming POST-requests and updates the state variables. At the same time, phone-app sends GET-requests at certain intervals to server to find out when new picture should be sent to server. When the state variables are altered, the phone sees that it's time to send a new picture. This way the app can be used without user interacting with the phone during game.
This approach should be replaced in future, because it burdens the server with unnecessary requests.


### CORS and proxy
At the moment, the server does not return Access-Control-Allow-Origin -header. This raises [Cross-origin resource sharing](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) -error. To workaroud this issue, the web-application uses proxy, which is configured in the package.json. In addition to the one line that has been inserted to package.json, the .env-file must also be edited to get the web-application work temporarily. While using the the proxy, the backend server addresses in .env-file must be left blank. Once the server has been updated to return correct header information, the proxy can be deleted from package.json and server addresses can be defined in .env-file.



