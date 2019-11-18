# Mobiilishakki


## Smartphone app (Android)
Used to take pictures of the board from an overhead view and sending them to the server for processing. Set to stationary position using a stand. Set-up at the start of the game: stand position, calibrating board, etc. After being set shouldn't require any further actions from the player for comfortable user experience. All further promts and feedback should be sent through web-app.

Should contain only the minimum required features for sending pictures of the game to server and set-up. For easier integration between different platforms (Android, iPhone, ...) the mobile app should be as simple as possible as all processing is done over the server.


## [Server](https://github.com/Mobiilishakki/Shakkipalvelin)
Responsible for the recognising the board and pieces using computer vision and machine learning, updating game state for phone and web apps, saving replay data & statistics, and managing user information & authentication.

Sent images are first processed using OpenCV computer vision library, and then processed using machine learning to find correct pieces and coordinates form the picture to keep track of the game state accurately. Used machine learning framework is Pytorch.

Game states, replay data, and user information are saved using postgresql. 

The server and database are build to their own modular docked containers for easy to deployment, use and expansion if needed.


## [Web-app](https://github.com/Mobiilishakki/WEB_APP)
Shows to player the game state (image of board state) and next required moves, and any other information necessary. Acts as the main user-interface for the player. Player should be able to control everything through the web-app after setting up the phone. Features such as player statistics, analytics, etc. could also be accessed through the web-app.