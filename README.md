# MyoBot
Control an FRC robot with a <a href="https://youtu.be/oWu9TFJjHaM">Thalmic Labs Myo Gesture Control Armband</a>!

This project composes of 3 programs:
1. A C++ program using the Myo SDK that reads gesture data, and sends it over a socket to the Java program
2. A Java program that reads the data from the C++ program, and sends it over NetworkTables to the robot
3. A Java robot program that reads NetworkTables values and updates the movements accordingly

Both Java programs are Eclipse projects, and the C++ program is a Visual Studio project.

### To Run/Build
1. In Eclipse, import `MyoBot_Bot` as an existing project
2. Build the robot program and upload to the RoboRIO
3. Install drivers for the Myo controller
4. Connect the Myo controller
5. Launch `bin/launch.bat` and start controlling the robot!

Making a fist drives forward, while waving in and out turns the robot.
