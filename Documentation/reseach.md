
# Notes on major problems

### Recognizing Pieces
First we tried using OpenCV computer vision library's built in methods for recognizing the pieces, mainly Hough Circles, which finds circles in pictures. When the taking pictures straight above the board this method is able to find most of the pieces. But dark pieces on dark squeres (low contrast between colors), reflections, and other problems caused by lightning conditions etc. makes this technique unreliable, as the model might lose some pieces on low contrast areas and falsely detect ones because of reflections. This method couldn't recoqnize pieces individually and would require other solutions, and/or calculation positions to do so. Finding the pieces by their contours was tried as well, but runs in to the same problem, as well as having to deal with differentating the pieces from the boards lines or other clutter.

Other problems arise from finding the colors of the pieces and dividing them based on the colors. Using the pieces found by Hough Circles or Contours-method and trying to calculate the mean color values of the pieces and comparing between them works often, but the afore mentioned lightning problems arise here as well. White piece in shade might be considerable darker than dark piece with reflection, and thus makes calculation the differences very difficult, if possible at all.

Because of these problems, using machine learning to teach a model to recoqnize the pieces seems most plausible, as it solves both of the problems at the same time, and recoqnizes each piece individually, which was unfeasible by the other methods.


### Recognizing Board
todo

# Related Material

