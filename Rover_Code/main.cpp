#include "server.h"
#include <unistd.h>
#include "wiringPi.h"
#include "softPwm.h"

//define some constants
#define size 6
#define CAM_UP 30
#define CAM_DOWN 10
#define CAM_PIN 17
#define LIGHT_PIN 25
#define RIGHT_MOTOR_1 22
#define RIGHT_MOTOR_2 27
#define RIGHT_MOTOR_PWM 4
#define LEFT_MOTOR_1 18
#define LEFT_MOTOR_2 23
#define LEFT_MOTOR_PWM 24

//char (byte) array to hold the incoming command
char command[size];
//variables to hold the respective speeds of the right and left motors
int rightSpeed =0;
int leftSpeed =0;

int main(int argc, char *argv[])
{
    //boolean to check if some of the socket setup failed
    bool bResult = false;

    //setup WiringPi to use the GPIO pin numbers
    wiringPiSetupGpio();

    //setup the camera servo pin to output software PWM, and turn off the lights
    pinMode(CAM_PIN,PWM_OUTPUT);
    digitalWrite(CAM_PIN,LOW);
    pinMode(LIGHT_PIN,OUTPUT);
    softPwmCreate(CAM_PIN,0,220);
    softPwmWrite(CAM_PIN,0);

    //setup the two direction pins and the PWM pin for the right motors
    pinMode(RIGHT_MOTOR_1,OUTPUT);
    pinMode(RIGHT_MOTOR_2,OUTPUT);
    pinMode(RIGHT_MOTOR_PWM,PWM_OUTPUT);
    digitalWrite(RIGHT_MOTOR_PWM,LOW);
    softPwmCreate(RIGHT_MOTOR_PWM,0,100);
    softPwmWrite(RIGHT_MOTOR_PWM,0);

    //setup the two direction pins and the PWM pin for the left motors
    pinMode(LEFT_MOTOR_1,OUTPUT);
    pinMode(LEFT_MOTOR_2,OUTPUT);
    pinMode(LEFT_MOTOR_PWM,PWM_OUTPUT);
    digitalWrite(LEFT_MOTOR_PWM,LOW);
    softPwmCreate(LEFT_MOTOR_PWM,0,100);
    softPwmWrite(LEFT_MOTOR_PWM,0);

    //port number to be used for socket connection (-1 indicates that UDP will not be used)
    int port = 5270;
    int dataport = -1;

    //writes some information about the pending connection
    printf("Server, listening on port %d, datagram port %d\n", port, dataport);
    fflush(NULL);

    //make a server object using the port numbers
    Server myLink(port, dataport, &bResult);

    //check if the server was created properly
    if (!bResult)
    {
        printf("Failed to create Server object!\n");
        return 0;
    }

    //more helpful printing
    printf("Server, waiting for connection...\n");
    fflush(NULL);

    //wait for client connection
    myLink.Connect();

    //if above method exits, a connection is established
    printf("Server, got a connection...\n");
    fflush(NULL);

    //loop through this code forever
    while(true){
        //receive the command
        myLink.RecvBytes(command,size);
        //if the forward/reverse byte is set to forward, set right and left speeds to the speed byte value
        if(command[1]==1){
            rightSpeed=command[0];
            leftSpeed=command[0];
        }
        //if the forward/reverse byte is set to reverse, set right and left speeds to the speed byte value times negative one
        if(command[1]==2){
            rightSpeed=-command[0];
            leftSpeed=-command[0];
        }
        //if the forward/reverse byte is set to stationary, set right and left speeds to 0
        if(command[1]==0){
            rightSpeed=0;
            leftSpeed=0;
        }

        //if the turning byte is set to right, increase the left motors' speed
        if(command[3]==1){
            leftSpeed+=command[2];
        }
        //if the turning byte is set to left, increase the right motors' speed
        if(command[3]==2){
            rightSpeed+=command[2];
        }
        //if the right speed is above 100, adjust both speeds to fit in the proper range
        if(rightSpeed>100){
            leftSpeed-=(rightSpeed-100);
            rightSpeed=100;
        }
        //if the left speed is above 100, adjust both speeds to fit in the proper range
        if(leftSpeed>100){
            rightSpeed-=(leftSpeed-100);
            leftSpeed=100;
        }
        //if the right speed is below -100, adjust both speeds to fit in the proper range
        if(rightSpeed<-100){
            leftSpeed-=(rightSpeed+100);
            rightSpeed=-100;
        }
        //if the left speed is below -100, adjust both speeds to fit in the proper range
        if(leftSpeed<-100){
            rightSpeed-=(leftSpeed+100);
            leftSpeed=-100;
        }
        //set the direction pins to go forward
        digitalWrite(RIGHT_MOTOR_1,LOW);
        digitalWrite(RIGHT_MOTOR_2,HIGH);
        digitalWrite(LEFT_MOTOR_1,LOW);
        digitalWrite(LEFT_MOTOR_2,HIGH);
        //if the right speed is negative, change the pins to reverse
        if(rightSpeed<0){
            rightSpeed=rightSpeed*-1;
            digitalWrite(RIGHT_MOTOR_1,HIGH);
            digitalWrite(RIGHT_MOTOR_2,LOW);
        }
        //if the left speed is negative, change the pins to reverse
        if(leftSpeed<0){
            leftSpeed=leftSpeed*-1;
            digitalWrite(LEFT_MOTOR_1,HIGH);
            digitalWrite(LEFT_MOTOR_2,LOW);
        }

        //write the speed values to the PWM pins
        softPwmWrite(LEFT_MOTOR_PWM,leftSpeed);
        softPwmWrite(RIGHT_MOTOR_PWM,rightSpeed);

        //if the camera direction pin is up, move the camera up for 15 milliseconds
        if(command[4]==1){
            softPwmWrite(CAM_PIN,CAM_UP);
            delay(15);
            softPwmWrite(CAM_PIN,0);
        }
        //if the camera direction pin is down, move the camera down for 15 milliseconds
        if(command[4]==2){
            softPwmWrite(CAM_PIN,CAM_DOWN);
            delay(15);
            softPwmWrite(CAM_PIN,0);
        }
        //set the IR LED control pin to the 6th byte in the command
        digitalWrite(LIGHT_PIN,command[5]);

        //sleep for 1000 microseconds (reduces CPU usage)
        usleep(1000);
    }
    return 0;
}
