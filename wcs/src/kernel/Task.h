#ifndef __TASK__
#define __TASK__

/**
 * @brief Base class for periodic tasks managed by the scheduler.
 */
class Task {
  int myPeriod;
  int timeElapsed;
  
public:
  /**
   * @brief Initializes the task period.
   * @param period Task period in milliseconds.
   */
  virtual void init(int period){
    myPeriod = period;  
    timeElapsed = 0;
  }

  /**
   * @brief Executes one task step.
   */
  virtual void tick() = 0;

  /**
   * @brief Updates the elapsed time and checks whether the task must run.
   * @param basePeriod Scheduler base period in milliseconds.
   * @return true if the task must execute, false otherwise.
   */
  bool updateAndCheckTime(int basePeriod){
    timeElapsed += basePeriod;
    if (timeElapsed >= myPeriod){
      timeElapsed = 0;
      return true;
    } else {
      return false; 
    }
  }
  
};

#endif