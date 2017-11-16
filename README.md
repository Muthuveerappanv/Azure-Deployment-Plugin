# Azure-DeploymentPlugin
Maven Plugin for deploying Springboot (or) other java projects to Azure API apps

## Short Description:
  Azure does not provide any maven plugin or jenkins plugin to deploy applications onto the Azure API or Web Apps. This plugin helps deploy binaries in the Azure API or Web Apps by using the Azure AD Oauth app & FTP.

### STEP 1 > Create Azure AD Oauth App & provide access to API App's ResourceGroup

- To create a Oauth App in Azure AD, go to Azure Active directory in the navigation pane & click on New-Application-Registration

  ![Alt text](images/Azure1.jpg?raw=true "Title")

- After Creating the App select Required Permissions on the app to add 'Windows Azure Service Management API' and provide all required permissions. (read - write). This ensures that the AD oauth app has access to the Azure Service Management APIs (start/stop Api/web App)

  ![Alt text](images/Azure33.jpg?raw=true "Title")

  ![Alt text](images/Azure4.jpg?raw=true "Title")

- After granting permissions, access keys needs to be generated & saved as it is visible only during creation

  ![Alt text](images/Azure5.png?raw=true "Title")

- Detailed Explanation of the Azure API app is provided in the following flow : https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-integrating-applications

Before adding resource group, make sure we have the following:
-  Application ID (Client ID)  (From previous steps)
-  Client Password (From previous step)
-  Tenant ID - refer below screenshot

Go to Azure Active Directory -> Properties -> Directory ID
  ![Alt text](images/Azure6.jpg?raw=true "Title")

Last process is to got to provide ResourceGroup access to this particular AD app. Make it the owner or provide modifier access as shown below

  ![Alt text](images/azure7.jpg?raw=true "Title")

Setup the deployment credentials & save them for using further:
https://docs.microsoft.com/en-us/azure/app-service/app-service-deployment-credentials

### STEP 2 > Building & deploying Apps
##### Projects:
1. __azure-deployment-plugin__ - Maven Plugin which needs to be added to the project that needs to be deployed in the azure api app
2. __hello-world-sb__ - Simple hello world springboot application that needs to be deployed in Azure with a configuration in pom.xml to add the custom maven plugin (abiove)

__**Build procedure**__:
- Run a 'mvn clean install' on the root folder of this repository.
- Once successful build move onto the sample application. open the pom.xml & provide the Application & Azure specific values

ftpCredentials -> __'<&lt;username>>::&lt;&lt;password>>'__

  ![Alt text](images/azure8.jpg?raw=true "Title")

| Configuration   	| Description                                                    	|
|-----------------	|----------------------------------------------------------------	|
| appName         	| Application name in Azure                                      	|
| clientId        	| Azure AD Oauth App's Client Id                                 	|
| clientPassword  	| password associated with the Oauth App                         	|
| ftpCredentials  	| Azure deployment credentials - __username::password__ (format) 	|
| ftpHostName     	| Azure api app's ftp hostname                                   	|
| resourceGroup   	| Azure Api App's resource group                                 	|
| tenantId        	| Azure AD Tenant ID                                             	|
| subscriptionId  	| Azure API app's subscription ID                                	|

##### Procedure:
On completing the above configuration, assuming that the project is already built & the binary is available in the target folder:

1. Run the custom maven command __azure-deploy:deploy__ from the 'hell-world-sb' project to push the jar file to the azure cloud via FTP.
2. Ensure that the FTP ports are not blocked in your network.
3. Continuous deployment is enabled by default. If there is an existing deployment already available, then the plugin does the below:
  - Stops the application
  - push the jar onto a temp directory
  - moves the current deployment into backup folder & renames with timestamp (creates backup dir if not present)
  - moves the new jar to the root folder
  - on successful completion of above steps - starts the application back, on any exception - still holds on to the existing deployment already available
  - clears the temp directory.

Concept Inherited from - https://docs.cloudfoundry.org/devguide/deploy-apps/blue-green.html

API APP screenshots:

__Simple API app tied to a app-service-plan:__

![Alt text](images/azure9.jpg?raw=true "Title")

__On Successful Deployment__ (see the following output)

![Alt text](images/azure10.jpg?raw=true "Title")


Sample web.config for deployment is attached :

[web.config](web.config) - Uses Java8 for supporting springboot in Azure API App.
