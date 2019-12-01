# bank-application example

Bank Application is demo for the following task:
- design and implement a RESTful API (including data model and the backing implementation) for  money transfers between accounts. 
-  keep it simple and to the point (e.g. no need to implement any authentication). 


To support functionality:
-  store collection of user accounts 
 - query balance per account
 - query total balance over bank (it should not change between other operations)
 - request transfer money between accounts

Non-functional requirements to meet 

- assume the API is invoked by multiple systems and services on behalf of end users 
- the datastore should run in-memory for the sake of this demo
- the final result should be executable as a standalone program (should not require a  pre-installed container/server).  


Solution concept
- Used Java fraimwork: Jersey + Jetty embedded container to support RESTful http service with JSON payload.
  The application is packed into runnable JAR (uber-jar) package.
- supported multiple parallel requests to service like reading balance and writing (transfer money)
- consistency between read/write operations is implmented as following:
–- used read/write lock at  bank level  to separate access  to bank balance and write transfer backlog;
–- to reduce total time when the bank is under write (exclusive) lock;
-- the  transfer operations are put in backlog,  wich is collected and processed asynchronously by 
the scheduled periodic task for batch processing.
    

     
How to compile project.
1. download, unpack the source from the link https://github.com/ViktorDus/bank-app-parent
    into any separate directory, say “c:\bank-app”;

2. open comman (terminal)  screen and run the following commands
   >cd  c:\bank-app
   >
3. run Maven command
    c:\bank-app>mvn -DskipTests=true clean package 

4. Executable jar can be found in the directory (Windows style):
       c:\bank-app\BankRestServer\target\bank-rest-server.jar

How to run Server

simply run it as command
c:\bank-app>java -jar BankRestServer\target\bank-rest-server.jar

Note: by default the port 8090  is used. If it needs choose another port then run with appropriate argument as following example:

c:\bank-app>java -jar BankRestServer\target\bank-rest-server.jar 9090
 

How to submit requests to the service

1. To request total balance, just run the GET  requests (in any browser or http client)
http://localhost:8090/bankService/total 

You should see “1000” as text response

2. To request balance per account  you run the GET request like the address:
http://localhost:8090/bankService/account/7 

where “7” – it is account number. Can be in range 1...10

the expected response is json string like the following

{"responseStatus":"SUCCESS","errorMessage":null,"accountNumber":7,"balance":100}


3. To request money transfer operation between 2 accounts it needs sending POST request to address http://localhost:8090/bankService/transfer
  
 with the body (substitute the values as per needed):
   {"fromAccountNumber":2,"toAccountNumber":7,"amount":45}

The expected response is json string like the following:
{"responseStatus":"SUCCESS","errorMessage":null,"accountNumber":2,"balance":55}


(The sample files with json request and expected responce are also placed under directory demo:
         tryTransferRequest.json  
         expectedResponse.json
)


For sending POST requests, various tools of http clients can be used (Intellyj Idea Http client,  Chrome, Firefox plugins, curl, ...other tools). 
For example, curl can post with the following command line

>curl -i -X POST -H 'Content-Type: application/json' -d '{"fromAccountNumber":2,toAccountNumber":7,"amount":45}' http://localhost:8090/bankService/transfer

 
Or alternatively, GET request is also implemented for transfer operation as following example address line:
http://localhost:8090/bankService/transfer?fromAccountNumber=3&toAccountNumber=5&amount=77  


How to run unit tests
 - use the maven command
 - port 9998 should be available
c:\bank-app> mvn clean test
