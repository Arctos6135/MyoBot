# MyoBot
Control an FRC robot with a <a href="https://youtu.be/oWu9TFJjHaM">Thalmic Labs Myo Gesture Control Armband</a>!

<img alt="Control Center" src="https://tylertian123.github.io/images/MyoBot/control_center.png"/>

Connect the Myo, launch the Driver Station and MyoBot Control Center, and direct the robot's movements with your arm gestures!

This project is currently Windows-only. It uses JNI to invoke native methods of the Windows C++ Myo API, and sends motor speed data to the robot via NetworkTables.
