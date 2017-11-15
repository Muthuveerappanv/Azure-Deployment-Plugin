package com.avon.sst.mw.az.deploy.plugin;

public class AzDeployConstants {
	public static final String AZ_AD_BASE_URL = "https://login.microsoftonline.com/%s/oauth2/";
	public static final String AZ_RESOURCE_URL = "https://management.core.windows.net/";
	public static final String AZ_AUTH_TYPE = "client_credentials";
	public static final String AZ_START_ACTN = "start";
	public static final String AZ_STOP_ACTN = "stop";
	public static final String AZ_API_VRSN = "2016-03-01";
	public static final String AZ_APP_URL = "https://management.azure.com/subscriptions/cf645eb0-744f-4cf8-bc4a-9f47c412f3a7/resourceGroups/%s/providers/Microsoft.Web/sites/";
	public static final String AZ_ROOT_DIR = "/site/wwwroot";
	public static final String AZ_TEMP_DIR = "temp";
	public static final String AZ_BACKUP_DIR = "backup";
}
