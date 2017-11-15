# Azure-DeploymentPlugin
Maven Plugin for deploying Springboot (or) other java projects to Azure API apps

## Short Description:
  Azure does not provide any maven plugin or jenkins plugin to deploy applications onto the Azure API or Web Apps. This plugin helps deploy binaries in the Azure API or Web Apps by using the Azure AD Oauth app & FTP.

### STEP 1 > Create Azure AD Oauth App & provide access to API App's ResourceGroup

- To create a Oauth App in Azure AD, go to Azure Active directory in the navigation pane & click on New-Application-Registration

![Alt text](images/Azure1.jpg?raw=true "Title")

- After Creating the App select Required Permissions on the app to add 'Windows Azure Service Management API' and provide all required permissions. (read - write). This ensures that the AD oauth app has access to the Azure Service Management APIs (start/stop Api/web App)

![Alt text](images/Azure2.jpg?raw=true "Title")


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
