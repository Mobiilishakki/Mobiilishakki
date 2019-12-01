
### Used Technology, Libraries & Dependencies:

+ [OpenCV](https://opencv.org/)
+ [Pytorch](https://pytorch.org/)
+ [PostgreSQL](https://www.postgresql.org/)

## [Web-app](https://github.com/Mobiilishakki/WEB_APP)
Web-application shows player the game state (image of board state) and next required moves, and any other information necessary. The web-application also contains json-server that can be used for testing and development purposes. 

### Requirements
Web-app uses [React](https://reactjs.org/), which is a javascript library for building user interfaces. Development and running the application requires [Node.js](https://nodejs.org/en/) which contains node and npm executables.

### `sudo apt-get install nodejs`
Install Node.js to computer from terminal. This command works for Ubuntu (linux). In case you use Mac, Windows or other linux-distros, please refer to https://nodejs.org/en/download/package-manager/.

### `npm install`
This command installs all the node modules required by the application. This must be run before the application can be started or build. Installed modules can be found from node_modules-folder.

### `npm start`
Starts the web-application in development mode/environment. This is the command you use, when you are developing the application. While the application is running in development mode, any changes to source code will automatically restart the application.

### `npm run server`
This command starts json-server in port 3001. This can be used for development. Instead of asking fen-notation from actual production server, we can host json-server locally and get the fen-notation from there instead. Json-server configuration is defined in [routes.json](https://github.com/Mobiilishakki/WEB_APP/blob/dev/web_app/routes.json) and [db.json](https://github.com/Mobiilishakki/WEB_APP/blob/dev/web_app/db.json) files.

### `npm run build`
This command builds the application. Build version can be found from build-folder. After building the application, it can be deployed. For environments using Node, the easiest way to handle this would be to install serve and let it handle the rest. 

### `npm install -g serve`
This command installs serve. Serve can be used to run build-version of the application.

### `serve -s build -l 4000`
This command starts the build-version of the application in port 4000. For additional information regarding to deployment, please refer to https://create-react-app.dev/docs/deployment/.

### Other
To be able to develop and run the application, [.env-file](https://create-react-app.dev/docs/adding-custom-environment-variables/) must be created to project folder. In this file, all the environmental variables must defined. Required variables are <strong><em>REACT_APP_BACKEND_URI_PROD</em></strong> and <strong><em>REACT_APP_BACKEND_URI_TEST</em></strong>. These are the addresses for production and testing backend-servers. Testing server can be the previously mentioned json-server.
