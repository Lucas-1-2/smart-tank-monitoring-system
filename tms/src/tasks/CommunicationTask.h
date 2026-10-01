#ifndef COMMUNICATION_TASK_H
#define COMMUNICATION_TASK_H


struct CommunicationTaskParams {
    const char* ssid;
    const char* password;
    unsigned long taskPeriodMs;
    unsigned long wifiRetryDelayMs;
};


void communicationTask(void* parameter);


#endif