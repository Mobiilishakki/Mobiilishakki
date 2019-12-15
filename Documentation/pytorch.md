# PyTorch in our application

The application uses PyTorch on the server to classify individual chess tiles.

## Some basic terms explained

### Tensor

Tensors in PyTorch are multi-dimensional matrices that contain the data. If you want to do PyTorch calculations, you want to put your input data into a Tensor. In our use cases, the tensors are basically images converted into matrices so each numerical value represents a pixel. The higher the resolution, the bigger the Tensor.

### Feature

A feature is an element of a Tensor.

### Classifier / model

Basically a trained instance of a neural network, with weights determined between the neurons.

### Class

The different categories that a model classifies. In our classifier (= model), there are 13 classes, one for each different chess piece (6 black + 6 white) and one for empty square.

### Autograd

Autograd basically stores the Tensor calculation history. Every time a Tensor operation is made, it will be stored so it can be accessed later. This is extremely important with neural networks as we will see shortly.

## The official tutorial

Most of the theory behind all machine learning is very mathematical. There is a coherent and quite short introductionary tutorial to PyTorch in their home site. If you have little or no previous experience working with PyTorch, it is highly recommended. It covers tensors and torch.nn package (neural networks) and how to operate them to train a classifier (= a model). The link is here https://pytorch.org/tutorials/beginner/deep_learning_60min_blitz.html.

Next, we are going to explain the most important things about our PyTorch neural network and some of the design choices.


## Our neural network

The package **torch.nn** contains structures and functions related to defining and managing the neural networks itself. In this project, we use a pretrained resnet50 model as a base for our neural network, replacing only the last layers of it to suit our needs. These layers are called *fully connected (fc)* layers. The neural network is defined in **train.py** file. We use **nn.Sequential** to compactly define these last layers of our neural network all at once.

The resnet50 model's last non-fc layer outputs 2048 features. Our first fc layer **nn.Linear(2048, 512)** has inputs for those 2048 features and has outputs for 512. Linear layers are key layers as they are used to output a feature set smaller than the input feature set. Then we have a ReLU layer, which just calculates *max(0, feature_value)* so we don't get negative values. This is done so the neurons won't get saturated, which would mean that the learning process stops. Next we have a dropout layer **nn.Dropout(0.2)** that randomly zeroes some features in the input tensor with probability of 0.2 in order to prevent co-adaptation of neurons. Then we have another linear layer **nn.Linear(512, 13)** that has inputs for those 512 features and outputs for 13 features. These last 13 features represent the 13 possible different types of chess squares we can have (6 white pieces, 6 black pieces and an empty chess square). Lastly we have a **nn.LogSoftmax(dim=1)** layer that basically turns the output values into probabilities of a feature set belonging to a certain class.

## General

 To create a model, a training process must be done with an existing dataset. In our application, this is done using the **train.py** file found in the server's git repository. The dataset itself is not in git, however, there is code for gathering data in the **collect.py** file. After the training is done, the model is saved into a .pth file. It can then be loaded into the application using the code in the "*LOADING*" section of the server's **app.py** file.

## Data collection

The data collection is currently done so that the board is hard coded into the **collect.py** file. When the server gets a cropped image of the board, it cuts the image into 64 squares. Then each square is saved to a directory containing other pictures of the same chess piece in that square. The problem with this approach is that when changing the positioning of the chess pieces, the new positions must be changed manually in the code every time. A better, automated solution with a graphical user interface could be implemented (preferably so the positions could be rearranged from the phone app and sent to the server).

## Training

Training the model works so that a pretrained model is used and trained with our chess dataset. We are using a resnet50 model as a base for our custom model. This must be done because our dataset is simply not big enough yet for a fresh model training. This is a normal way to make custom models. However, the resnet50 model might not be optimal for our purposes, it simply is a model that we managed to implement into the program. It seems to work pretty well, but experimenting with different base models could be done to see which works the best as there probably are some (probably slight) differences. This is much less important than gathering a big and diverse dataset, though. The training process itself is done in the "epoch" loop. Epochs are well explained [here](https://deepai.org/machine-learning-glossary-and-terms/epoch). The amount of epochs can be changed in the code to figure out the optimal epoch count. During the project we have used 5-10 epochs.

Before training, every square is also cloned 3 times and each clone is rotated by 90, 180 and 270 degrees, respectively. This provides more variance in the dataset with little effort (more different angles).

## Classifying

The classifying in the **app.py** file is done when a HTTP POST request is sent to the '/upload' path with an image in it. First, the chessboard is detected from the image and cropped automatically, then the resulting image is splitted automatically into 64 squares. Then each of those squares is fed to the PyTorch network, which will give its prediction as a result. The prediction of each square is appended into a string so that the resulting string is the fen notation of the board state.