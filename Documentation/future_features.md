
# Features to be implemented & ideas for future

* Functionality for handling unique game instances for different players
    * Matchmaking
    * Leaderboards?
    * Invitations for friends
    * Game modes: versus AI / versus a friend / tournament / etc.
* Using chess logic to optimize machine learning solutions 
    * Examples: always two kings on board, no more than 8 pawns per side and so on
    * Comparing to previous game states to eliminate errors in recognition
    * Research other possible options for improving chess piece recognition
* Changing the way the pieces are recognized?
    * Currently the top down view has some issues:
        * Some pieces might look almost the same when viewed straight above (might not be a problem with enough data)
        * The angle of the camera can't be lowered much
    * [This paper](https://www.researchgate.net/deref/https%3A%2F%2Fweb.stanford.edu%2Fclass%2Fcs231a%2Fprev_projects_2016%2FCS_231A_Final_Report.pdf) uses both the original picture and the cropped chessboard for piece recognition, allowing for a more flexible camera positioning
* Validation for moves
    * Not saving illegal moves
    * Warning player of illegal moves
* User can train the model for new boards & pieces from the app.
    * Set the board state in the chess UI and send the information with the picture
    * possible tools: https://github.com/bakkenbaeck/chessboardeditor
* User can also play against computers and other players through some internet chess engine/service
    * Stockfish is GPL and thus out of scope (maybe?) -> research & consider other options
* User can control the phone through the web app / sync the web app & phone
    * Device Push notifications?
    * See [temp_solutions.md](https://github.com/Mobiilishakki/Mobiilishakki/blob/master/Documentation/temp_solutions.md)
* User can access their past game data, and the app offers analysis service for it, for example:
    * Go back to previous game and see better moves & tips to improve
    * See statistics of own games, progress, playing habits, etc.
    * Simulated exercises
* Other platforms
    * iPhone app
    * Smart TV (app or through web)
    * Computer (web page with webcam)
* User accounts
    * Design their functionality / features
    * Implement them (phone app & web app)
* Saving data / pictures automatically from all games and using them to improve the machine learning model continously
* Playing only using phone app?
    * Own UI & other functionality
* Handling raising units
    * What if not enough unique pieces? Ex. Trying to raise pawn to queen, but no extra queen pieces. Maybe must copy and train a neural network for one game with, for example, coins recognized as queens, but so that it doesn't impact the general neural network others are using?
* Nicer UI & graphics
* Text-to-speech & other accessibility features
* Sharing game footage live & spectating others' games
    * Streaming functionality
    * Works from game state, load the game data from the database?
    * Disable outside activity
    

