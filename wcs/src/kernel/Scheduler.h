#ifndef __SCHEDULER__
#define __SCHEDULER__

#include "Task.h"

#define MAX_TASKS 50

/**
 * @brief Cooperative scheduler for periodic tasks.
 */
class Scheduler {
  
  int basePeriod;
  int nTasks;
  Task* taskList[MAX_TASKS];  

public:
  /**
   * @brief Initializes the scheduler.
   * @param basePeriod Scheduler base period in milliseconds.
   */
  void init(int basePeriod); 
  
  /**
   * @brief Adds a task to the scheduler.
   * @param task Pointer to the task to add.
   * @return true if the task is added, false otherwise.
   */
  virtual bool addTask(Task* task); 
  
  /**
   * @brief Executes one scheduler cycle.
   */
  virtual void schedule();
};

#endif