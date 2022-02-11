# _Packtpub Scrapper_


## TL;DR

This tool lets you download ebooks and videos from [https://www.packtpub.com/](https://www.packtpub.com/) even if you don't have a full paid subscription. 


## System Requirement 



* Docker 
* Yeh That's all you need 😂


## Steps to run 



* Get a trial account at [https://www.packtpub.com/](https://www.packtpub.com/)  
* It requires a simple visa or a mastercard, and 1 USD will be charged and refunded in a day.
* Clone this repo locally.
*  Create a **.env** file which contains the following variables.
    * user
    * pass 
    * bookid  
    *  TOKEN (optional)

  A sample env file looks like 


```
user=meghdut.window@gmail.com
pass=/i6X#G345g@Ji5
bookid=9781789132779
TOKEN="Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQ..

```



* bookid can be found from the URL such as : 
* https://www.packtpub.com/product/aws-security-cookbook/**_9781838826253_**
* Run **docker-compose build**
* Run **docker-compose up**
* Now sit back a grab a cup of Chai ☕ 


# Technical  Details 


## System Requirement for Development 



* Java 11
* Intellij IDEA (or any idea with awesome Kotlin Support)
* Webstorm (or any idea with awesome Nodejs support)
* Nodejs 
* Docker

**Dependencies**



* JVM 
    * Jetbrains Exposed for easily handling sql Queries 
    * Sqlite an embedded db
    * Ktor-client for making network requests 
    * Gson for Json parsing 
* Nodejs 
    * epub-gen
    * Express
