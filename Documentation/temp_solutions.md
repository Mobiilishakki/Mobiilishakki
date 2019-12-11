
# Temporary Solutions and Functionality

### Board Recognizion
At the moment the image taken with the phone and sent to server is cropped by the user to match the board, so it can be processed easier by the server, just splitting it to 64 smaller images for each square on the board by using dimensions of the original image.
In future a proper computer vision functionality could be implemented, either using it to improve the accuracy of splitting the images, or even replacing the cropping completely and recognizing the board automatically for the player. 
NOTE! [research.md](https://github.com/Mobiilishakki/Mobiilishakki/blob/master/Documentation/reseach.md) in documentation, and the difficulties associated. 

### Controlling the Phone-app through Web-application
Currently, the web-application is used to tell the server when player has made his/her move. This is achieved by sending POST-reguest to server. The server notices incoming POST-requests and updates the state variables. At the same time, phone-app sends GET-requests at certain intervals to server to find out when new picture should be sent to server. When the state variables are altered, the phone sees that it's time to send a new picture. This way the app can be used without user interacting with the phone during game.
This approach should be replaced in future, because it burdens the server with unnecessary requests.


### Turn-clock solution in Web-app
todo



