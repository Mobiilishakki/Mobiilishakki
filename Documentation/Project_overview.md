# Mobiilishakki

## Basic program flow
1. A button is pressed in the web app that sends a signal to the phone telling to take a picture
1. The phone takes a picture and sends it to the server
2. The server processes the picture and gives the FEN notation of the board
3. The web app gets the FEN from the server and shows the user the board state & the move made


## Smartphone app (Android)
Used to take pictures of the board from a top down view and sending them to the server for processing. Set to stationary position using a stand. Set-up at the start of the game: stand position, maybe training the neural network with a new piece set etc. 
After being set, the app shouldn't require any further actions from the player for a comfortable user experience. All further promts and feedback should be sent through the web app.

Should contain only the minimum required features for sending pictures of the game to server and set-up. For easier integration between different platforms (Android, iPhone, ...) the mobile app should be as simple as possible as all processing is done over the server.

Based on Java at the moment.

## [Server](https://github.com/Mobiilishakki/Shakkipalvelin) ([Docker](https://hub.docker.com/r/mshakki/chesscaffe))
Responsible for recognising the board and the pieces using computer vision and machine learning, updating the game state for phone and web apps, saving the replay data & statistics, and managing user information & authentication.

Sent images are first processed using OpenCV computer vision library, and then processed using machine learning to find correct pieces and coordinates from the picture to keep track of the game state accurately. The machine learning framework used is PyTorch.

Game states, replay data, and user information are saved to PostgreSQL database. 

The server and database are built to their own modular Docker containers for easy deployment, use and expansion if needed.


## [Web app](https://github.com/Mobiilishakki/WEB_APP)
Shows to the player the current game state (an image of board state) and the next required moves, and any other information necessary. Acts as the main user interface for the player. Player should be able to control everything through the web app after setting up the phone. Features such as player statistics, analytics, etc. could also be accessed through the web app in the future.