@echo off
REM First start the Myo reader
start myo_reader.exe
REM Delay a bit for the Myo reader program to set up the sockets
ping localhost -n 1 -w 500 > nul
REM Now start the Java program in a new window and keep it open
start cmd /c "title MyoBot Bridge Program && java -jar bridge.jar"