package com.avon.sst.mw.az.deploy.plugin;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPTransferType;

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = true, threadSafe = false, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AzDeployMojo extends AbstractMojo {

	@Override
	public Log getLog() {
		return super.getLog();
	};

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}", readonly = true, required = true)
	private String targetFilePath = "src/main/resources/deployment.jar";

	@Parameter(defaultValue = "${project.artifactId}.${project.packaging}")
	private String jarName = "deployment.jar";

	@Parameter(defaultValue = "${project.artifactId}", required = true)
	private String buildName = "deployment";

	@Parameter(required = true, property = "ftpCredentials")
	private String ftpCredentials;

	@Parameter(required = true, property = "resourceGroup")
	private String resourceGroup;

	@Parameter(required = true, property = "appName")
	private String appName;

	@Parameter(required = true, property = "tenantId")
	private String tenantId;

	@Parameter(required = true, property = "clientId")
	private String clientId;

	@Parameter(required = true, property = "clientPwd")
	private String clientPassword;

	@Parameter(required = true, property = "ftpHostName")
	private String ftpHostName;

	Log log = getLog();

	public void execute() throws MojoExecutionException, MojoFailureException {
		String ftpUserName;
		String ftpPassword;
		String[] credentialsArray = StringUtils.splitByWholeSeparator(ftpCredentials, "::");
		if (credentialsArray != null && credentialsArray.length > 1) {
			ftpUserName = credentialsArray[0];
			ftpPassword = credentialsArray[1];
		} else {
			throw new RuntimeException("INVALID_CRDENTIALS");
		}

		try {
			deployToAzure(ftpUserName, ftpPassword);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("Exception While deploying to Azure", e);
		}
	}

	public static void main(String[] args) throws Exception {
		AzDeployMojo azDeployMojo = new AzDeployMojo();
		azDeployMojo.deployToAzure("test-cicd-jenkins\\avonmwusr-cicd", "Infosys@123");
	}

	private void deployToAzure(String ftpUser, String ftpPwd) throws Exception {
		com.enterprisedt.net.ftp.FTPClient ftpClient = new com.enterprisedt.net.ftp.FTPClient();
		String token = null;
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
			// e7f*** is the tenant id of AD
			String tokenUrl = UriComponentsBuilder
					.fromHttpUrl(String.format(AzDeployConstants.AZ_AD_BASE_URL, tenantId)).path("token").build()
					.toString();
			token = getAzureADToken(tokenUrl, restTemplate);
			log.info("Token Received Successfully!");

			log.info("Azure deployment - Base URL == " + AzDeployConstants.AZ_APP_URL);

			// Invoke FTP push to add binary in temp folder
			ftpClient.setRemoteHost(ftpHostName);
			ftpClient.connect();
			ftpClient.user(ftpUser);
			ftpClient.password(ftpPwd);
			ftpClient.chdir(AzDeployConstants.AZ_ROOT_DIR);
			log.info("Successfully logged into the FTP server " + ftpHostName + " Path - "
					+ AzDeployConstants.AZ_ROOT_DIR);
			String fileTimestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
			List<String> dirList = Arrays.asList(ftpClient.dir());
			Boolean hasTempDir = dirList.stream().anyMatch(x -> x.equalsIgnoreCase(AzDeployConstants.AZ_TEMP_DIR));
			if (!hasTempDir) {
				ftpClient.mkdir(AzDeployConstants.AZ_TEMP_DIR);
			}
			ftpClient.setType(FTPTransferType.BINARY);
			String tempFileName = AzDeployConstants.AZ_TEMP_DIR + "/" + buildName + "_" + fileTimestampStr + ".zip";
			ftpClient.put(Files.readAllBytes(Paths.get(targetFilePath)), tempFileName);

			log.info("Successfully pushed build onto temp folders");

			// Rest call to stop application
			controlAppStartStop(token, restTemplate, AzDeployConstants.AZ_STOP_ACTN);

			Boolean containsBkp = dirList.stream().anyMatch(x -> x.equalsIgnoreCase("backup"));
			if (!containsBkp) {
				ftpClient.mkdir(AzDeployConstants.AZ_BACKUP_DIR);
			}

			String oldFileName = AzDeployConstants.AZ_BACKUP_DIR + "/" + buildName + "_" + fileTimestampStr + ".jar";

			log.info("Successfully moved application to backup with modified name- " + oldFileName);

			replaceExistingDeployment(ftpClient, tempFileName, oldFileName);

			log.info("New build is moved to Root Folder");
			try {
				ftpClient.rmdir(AzDeployConstants.AZ_TEMP_DIR);
			} catch (Exception e) {
				log.error("Temp Dir not-empty");
				ftpClient.chdir(AzDeployConstants.AZ_TEMP_DIR);
				for(String a:Arrays.asList(ftpClient.dir())) {
					ftpClient.delete(a);
				}
				ftpClient.chdir(AzDeployConstants.AZ_ROOT_DIR);
				ftpClient.rmdir(AzDeployConstants.AZ_TEMP_DIR);
			}
			log.info("Temp directory is cleared");
		} catch (Exception e) {
			log.error("Unknown Exception during deploy ", e);
		} finally {
			ftpClient.quit();
			controlAppStartStop(token, restTemplate, AzDeployConstants.AZ_START_ACTN);
		}
	}

	public String getAzureADToken(String tokenUrl, RestTemplate restTemplate) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("grant_type", AzDeployConstants.AZ_AUTH_TYPE);
		map.add("client_id", clientId);
		map.add("client_secret", clientPassword);
		map.add("resource", AzDeployConstants.AZ_RESOURCE_URL);
		log.info("Start of Get Token from Azure AD from " + tokenUrl);
		ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUrl, map, Map.class);
		String token = resp.getBody().get("token_type") + " " + resp.getBody().get("access_token");
		return token;
	}

	public void controlAppStartStop(String token, RestTemplate restTemplate, String action) {
		String appUrl = String.format(AzDeployConstants.AZ_APP_URL, resourceGroup);
		URI finalUrl = UriComponentsBuilder.fromHttpUrl(appUrl).pathSegment(appName, action)
				.queryParam("api-version", AzDeployConstants.AZ_API_VRSN).build().toUri();
		HttpEntity<String> httpEntity = new HttpEntity<String>(new HttpHeaders() {
			{
				set("Authorization", token);
			}
		});
		restTemplate.exchange(finalUrl, HttpMethod.POST, httpEntity, String.class);
		log.info("Successfully " + action + " of application - " + appName);
	}

	public void replaceExistingDeployment(FTPClient ftpClient, String tempNewBuild, String oldFileName)
			throws Exception {
		int count = 0;
		int maxTries = 3;
		while (true) {
			try {
				Thread.sleep(5000);
				log.info("Modifying existing app - Number of Tries: " + count);
				if (Arrays.asList(ftpClient.dir()).contains(jarName)) {
					ftpClient.rename(jarName, oldFileName);
				}
				ftpClient.rename(tempNewBuild, jarName);
				break;
			} catch (Exception e) {
				// handle exception
				if (++count == maxTries) {
					throw e;
				}
			}
		}
	}
}