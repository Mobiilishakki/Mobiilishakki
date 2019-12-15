
### Used technology, libraries & dependencies:

+ [OpenCV](https://opencv.org/)
+ [Pytorch](https://pytorch.org/)
+ [PostgreSQL](https://www.postgresql.org/)

## [Web app](https://github.com/Mobiilishakki/WEB_APP)
The web application shows the player the current game state (an image of the board state) and the next required moves, and any other information necessary. The web application also contains a JSON server that can be used for testing and developing purposes. 

### Requirements
The web app uses [React](https://reactjs.org/), which is a Javascript library for building user interfaces. Developing and running the application requires [Node.js](https://nodejs.org/en/) which contains node and npm executables.

### `sudo apt-get install nodejs`
Install Node.js to the computer from the terminal. This command works for Ubuntu (Linux). In case you use Mac, Windows or other Linux-distros, please refer to https://nodejs.org/en/download/package-manager/.

### `npm install`
This command installs all the node modules required by the application. This must be run before the application can be started or built. Installed modules can be found from the node_modules folder.

### `npm start`
Starts the web application in development mode/environment. This is the command you use when you are developing the application. While the application is running in development mode, any changes to the source code will automatically restart the application.

### `npm run server`
This command starts JSON server in port 3001. This can be used for development. Instead of getting the FEN notation from the actual production server, we can host the JSON server locally and get the FEN notation from there instead. JSON server configuration is defined in [routes.json](https://github.com/Mobiilishakki/WEB_APP/blob/dev/web_app/routes.json) and [db.json](https://github.com/Mobiilishakki/WEB_APP/blob/dev/web_app/db.json) files.

### `npm run build`
This command builds the application. Build version can be found from the build folder. After building the application, it can be deployed. For environments using Node, the easiest way to handle this would be to install the npm package "serve" and let it handle the rest. 

### `npm install -g serve`
This command installs serve. Serve can be used to run the build version of the application.

### `serve -s build -l 4000`
This command starts the build version of the application in port 4000. For additional information regarding the deployment, please refer to https://create-react-app.dev/docs/deployment/.

### Other
To be able to develop and run the application, [.env](https://create-react-app.dev/docs/adding-custom-environment-variables/) file must be created to the project folder. In this file, all the required environmental variables can be defined. Required variables are <strong><em>REACT_APP_BACKEND_URI_PROD</em></strong> and <strong><em>REACT_APP_BACKEND_URI_TEST</em></strong>. These are the addresses for production and for testing the backend servers. The testing server can be the previously mentioned JSON server.
