
# Notes on major problems

### Recognizing Pieces
First we tried using OpenCV computer vision library's built in methods for recognizing the pieces, mainly Hough Circles, which finds circles in pictures. When the taking pictures straight above the board this method is able to find most of the pieces. But dark pieces on dark squeres (low contrast between colors), reflections, and other problems caused by lightning conditions etc. makes this technique unreliable, as the model might lose some pieces on low contrast areas and falsely detect ones because of reflections. This method couldn't recoqnize pieces individually and would require other solutions, and/or calculation positions to do so. Finding the pieces by their contours was tried as well, but runs in to the same problem, as well as having to deal with differentating the pieces from the boards lines or other clutter.

Other problems arise from finding the colors of the pieces and dividing them based on the colors. Using the pieces found by Hough Circles or Contours-method and trying to calculate the mean color values of the pieces and comparing between them works often, but the afore mentioned lightning problems arise here as well. White piece in shade might be considerable darker than dark piece with reflection, and thus makes calculation the differences very difficult, if possible at all.

Because of these problems, using machine learning to teach a model to recoqnize the pieces seems most plausible, as it solves both of the problems at the same time, and recoqnizes each piece individually, which was unfeasible by the other methods.


# Recognizing Board
Recognizing the chess board was one of the most critical steps of the project and many hours of work has been invested to it. Howerver, even after all the effort, we could not find a general solution that would work recognize different kind of chess boards. We were able to recognize our test board in some cases but the error rate was too high to call it a success. There were no specific reason, why the board recognition did not work. Instead it was more of a sum of multiple small issues caused by variation in environment and hardware.

Our approach was the following (after many hours of testing and dead ends):
1. Blur the image to reduce the number of edges that can be detected from image containing the board. This was achieved by using OpenCV's blur-function.
2. Next step was trying to detect edges from image. To do this, we used OpenCV's canny-function.
3. After detecting the edges, we tried to detect the lines from the picture by using the OpenCV's HoughLines-function.
4. Now that we had detected all the lines, we divided them to horizontal and vertical lines.
5. Next, we removed extra lines that were similar to some other line. This is because HoughLines may detect same line multiple time with small variation.
6. Because HoughLines detects also lines that are irrelevant, much of filtering was done to remove these lines.
7. Once the extra lines have been filttered, the next step was to calculate intersection points for horizontal and vertical lines.
8. After intersection points were calculated, the 4 corners of the chessboard were selected from them.
9. Final step was to warp perspective by using OpenCV's warpPerspective-function.

In short, there were multiple steps and nearly each of them were prone to errors. For example, blurring the image worked if the resolution of the input image was high but when reducing the input image resolution, blurring had a negative effect and caused the edge detection to fail.

Edge detection itself was also problematic. The canny-function needs two parameters as threshold values for edge detection. However, the best possible values vary and trying to find values dynamically is hard. 

Enviromental properties also have a huge impact to the detection algorithms. For example lighting and reflections made it hard to determine where lines actually were and caused the canny-function together with HoughLines fail frequently.

We also had problems with handling the detected lines. OpenCV uses polar coordinate system (rho, theta) for representing lines and this caused some unexpected bugs when trying to filter irrelevant lines. Lines that seemed to be similar together actually had a angle (theta) difference of pi and rho value something totally different.

Filtering the irrelevant lines was also hard. HoughLines detects many lines that are needed and it might faile with lines that are actually relevant. There is no way to be 100% sure that some detected line is or is not actually a chess board grid line .
