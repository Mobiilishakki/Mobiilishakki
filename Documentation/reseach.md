
# Notes on major problems

### Recognizing Pieces
First we tried using the built in methods of OpenCV computer vision library for recognizing the pieces, mainly Hough Circles, which finds circles in pictures. When taking pictures straight above the board, this method is able to find most of the pieces. However, dark pieces on dark squeres (low contrast between colors), reflections, and other problems caused by lightning conditions etc. makes this technique unreliable, as the model might lose some pieces on low contrast areas and falsely detect ones because of reflections. This method couldn't recognize pieces individually and would require other solutions, and/or calculation positions to do so. Finding the pieces by their contours was tried as well, but runs in to the same problem, as well as having to deal with differentating the pieces from the boards lines or other clutter.

Other problems arise from finding the colors of the pieces and dividing them based on the colors. Using the pieces found by Hough Circles or Contours-method and trying to calculate the mean color values of the pieces and comparing between them works often, but the aforementioned lightning problems arise here as well. A white piece in shade might be considerable darker than dark piece with reflection, and thus makes calculation the differences very difficult, if possible at all.

Because of these problems, using machine learning to teach a model to recognize the pieces seems most plausible, as it solves both of the problems at the same time, and recognizes each piece individually, which was unfeasible with the other methods.


### Recognizing Board
Recognizing the chess board was one of the most critical steps of the project and many hours of work has been invested to it. However, even after all the effort, we could not find a general solution that would work on different kind of chessboards. We were able to recognize our test board in some cases but the error rate was too high to call it a success. There was not a specific reason why the board recognition did not work. Instead, the failure was caused by a sum of multiple small issues.

Our approach was the following (after many hours of testing and dead ends):
1. Blur the image to reduce the number of edges that can be detected from image containing the board. This was achieved by using OpenCV's blur function.
2. Next step was trying to detect edges from image. To do this, we used OpenCV's Canny function.
3. After detecting the edges, we tried to detect the lines from the picture by using OpenCV's HoughLines function.
4. Now that we had detected all the lines, we divided them to horizontal and vertical lines.
5. Next, we removed extra lines that were similar to some other line. This is because HoughLines may detect same line multiple time with small variation.
6. Because HoughLines detects also lines that are irrelevant, much of filtering was done to remove these lines.
7. Once the extra lines have been filtered, the next step was to calculate intersection points for the horizontal and the vertical lines.
8. After intersection points were calculated, the 4 corners of the chessboard were selected from them.
9. Final step was to warp perspective by using OpenCV's warpPerspective-function so that the resulting image was a perfect square.

In short, there were multiple steps and nearly each of them were prone to errors. For example, blurring the image worked if the resolution of the input image was high but when reducing the input image resolution, blurring had a negative effect and caused the edge detection to fail.

Edge detection itself was also problematic. The Canny function needs two parameters as threshold values for the edge detection. However, the best possible values vary and trying to find values dynamically is hard. 

Enviromental properties also have a huge impact to the detection algorithms. For example, the lighting and reflections made it hard to determine where the lines actually were and caused the Canny function together with HoughLines to fail frequently.

We also had problems with handling the detected lines. OpenCV uses polar coordinate system (rho, theta) for representing lines and this caused some unexpected bugs when trying to filter irrelevant lines. Lines that seemed to be similar together actually had a angle (theta) difference of pi and rho value something totally different.

Filtering the irrelevant lines was also hard. HoughLines detects many lines that are needed and it might fail with lines that are actually relevant. There is no way to be 100% sure that some detected line is or is not actually a chessboard grid line.

After a while we decided to circumvent the problems with the board recognition by having the user crop the chessboard from the phone view with a selector. Sending a cropped picture to the server meant the image could be analyzed as is without having to process it, which also decreased processing time on the server.

Now the server uses automatic chessboard recognition and cropping, thanks to Maciej A. Czyzewski's [neural-chessboard](https://github.com/maciejczyzewski/neural-chessboard) library. [This paper](https://www.researchgate.net/publication/328461364_Chessboard_and_chess_piece_recognition_with_the_support_of_neural_networks) by Czyzewski et al. explains the functionality of the chessboard detector quite in-depth. The board recognition should work on almost any chessboard and from different angles with a very high prediction probability (99,5% according to the paper).

### Android & Android Studio
Android Studio created some problems, mainly because we had no previous experience on it. Connection with Android phones and getting developer mode on also caused some problems in the beginning.

The app requires an Android phone with API level of 23 or higher. It's the requirement for the Async HTTP Library. Android's Camera 2 API requires API level of 21 or higher.

While the old Android camera API has been deprecated for a long time and most phones support API level 23, they still have problems with certain aspects of it. Full support varies with different models, but most phones since 2017, at least from most known manufacturers, are fully compatible.


## Related Material
* https://web.stanford.edu/class/cs231a/prev_projects_2016/CS_231A_Final_Report.pdf
    * https://github.com/jialinding/ChessVision
* https://arxiv.org/pdf/1708.03898.pdf
https://loopj.com/android-async-http/
* https://medium.com/@daylenyang/building-chess-id-99afa57326cd
    * https://github.com/daylen/chess-id
* https://tech.bakkenbaeck.com/post/chessvision
* https://github.com/bakkenbaeck/chessboardeditor
* https://github.com/maciejczyzewski/neural-chessboard
