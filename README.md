# MyoBot
Control an FRC robot with a <a href="https://youtu.be/oWu9TFJjHaM">Thalmic Labs Myo Gesture Control Armband</a>!

This project composes of 3 programs:
1. A C++ program using the Myo SDK that reads gesture data, and sends it over a socket to the Java program
2. A Java program that reads the data from the C++ program, and sends it over NetworkTables to the robot
3. A Java robot program that reads NetworkTables values and updates the movements accordingly

Both Java programs are Eclipse projects, and the C++ program is a Visual Studio project.

### To Run (Currently Windows-Only)
1. Download and install [Myo Connect](https://www.myo.com/start)
2. Import `MyoBot_Bot` as an Eclipse project
3. Build and upload robot code **(Remember to change the motor definitions in `RobotMap`!)**
4. Run Myo Connect and set up Myo
5. Run `bin/launch.cmd`
6. Operate the robot!

The controls are:
* Fist: Drive Forward
* Wave Left: Drive Left
* Wave Right: Drive Right
* Spread Fingers: Drive Backward

While the program is running, the unlock state and mode of the Myo is printed to the console of `myo_reader.exe`. There are two different unlock modes: automatic and toggle. In automatic unlock, the Myo locks automatically after a short period of inactivity. In toggle mode, the Myo is locked/unlocked through pressing Alt+U on the keyboard. To switch between the unlock modes, press Alt+M. To exit the program, press Alt+E. The key press detection is done through a global key hook, so focus is not needed.

### To Build (Currently Windows-Only)
1. Import `MyoBot_Bot` and `MyoBot_Java` in Eclipse
2. Import `MyoBot_Cpp` in Visual Studio 2017 or higher
3. Add the following properties to the project configuration:
* "C/C++"/General/Additional Include Directories: `$(ProjectDir)include`
* Linker/General/Additional Library Directories: `$(ProjectDir)lib`
* Linker/General/Additional Dependencies: `myo32.lib`
