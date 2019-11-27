
# Features to be implemented & ideas for future

* Functionality for handling unique game instances for different players
    * Matchmaking
    * Leaderboards?
* Using chess logic to optimize machine learning solutions 
    * Examples: always two kings on board, no more than 8 pawns per side, and so on
    * Comparing to previous states to eliminate errors in recognision
* Validation for moves
    * Not saving illegal moves
    * Warning player of illegal moves
* User can train the model for new boards & pieces from the app.
    * Set the board state in chess UI and send the information with the picture
    * possible tools: https://github.com/bakkenbaeck/chessboardeditor
* User can also play against computers and other players through some internet chess engine/service
    * Stockfish is GPL and thus  out of scope (maybe?) -> research & consider other options
* User can control the phone through the web-app / sync web-app & phone
    * Device Push notifications?
    * See: See: [temp_solutions.md](https://github.com/Mobiilishakki/Mobiilishakki/blob/master/Documentation/temp_solutions.md)
* User can access their past game data, and the app offers analysis service for it, for example:
    * Go back to previous game and see better moves & tips to improve
    * See statistics of own games, progress, playing habits, etc.
    * Simulated exercises
* iPhone app
* User accounts
    * Design functionality / features
    * Implement (phone app & Web-app)
* Move to actual board recognision from the crop / user calibrated solution
    * See: [temp_solutions.md](https://github.com/Mobiilishakki/Mobiilishakki/blob/master/Documentation/temp_solutions.md)
* Saving data / pictures automatically from all games and using them to improve the machine learning model continously
* Playing only using phone app?
    * Own UI & other functionality
* Handling raising units
    * What if not enough unique pieces? Ex. Trying to raise Pawn to Queen, but no extra Queen pieces.
    * Input from player? But what about discepancy with model?
* Nicer UI & Graphics
* Text-to-Speech & other accessibility
* 

