# DeadlockDetection

@ivceh - Mtichell and Merritt's algorithm
@sandrolovnicki - OR CMH
@sebbonaut - Kshemkalyani-Singhal

## AND / OR example

#### what is a deadlock in AND model, but not in OR model:
java DeadlockTester test 0 4 [class] 1 2  
java DeadlockTester test 1 4 [class]  
java DeadlockTester test 2 4 [class] 3  
java DeadlockTester test 3 4 [class] 0  

#### what is a deadlock in both AND and OR models:
java DeadlockTester test 0 4 [class] 1 2  
java DeadlockTester test 1 4 [class] 2  
java DeadlockTester test 2 4 [class] 3  
java DeadlockTester test 3 4 [class] 0  
