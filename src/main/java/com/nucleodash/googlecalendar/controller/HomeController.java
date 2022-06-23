package com.nucleodash.googlecalendar.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.PostConstruct;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;


@Controller
public class HomeController {

    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String USER_INDENTIFIER_KEY = "DUMMY YSER";

    @Value("${google.oauth.callback.uri}")
    private String CALLBACK_URI;

    @Value("${google.secret.key.path}")
    private Resource gdSecretsKeys;

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    @Value("${google.credentials.folder.path}")
    private String CREDENTIALS_FILE_PATH;

    private GoogleAuthorizationCodeFlow flow;

    @PostConstruct
    public void init() throws Exception {
        // Load client secrets.

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretsKeys.getInputStream()));

        flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .build();
    }

    @GetMapping(value = "/")
    public String showHomePage() throws IOException {
        boolean isUserAuthenticated = false;
        Credential credential = flow.loadCredential(USER_INDENTIFIER_KEY);
        if(credential != null){
            boolean tokenValid = credential.refreshToken();
            if(tokenValid){
                isUserAuthenticated = true;
            }
        }
        return isUserAuthenticated?"dashboard.html":"index.html";
    }
    @GetMapping(value = "/googlesignin")
    public void doGoogleSignIn(HttpServletResponse response) throws Exception{
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectUrl = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();
        response.sendRedirect(redirectUrl);
    }
    @GetMapping(value = "/oauth")
    public String saveAuthorizationCode(HttpServletRequest request) throws Exception{
        String code = request.getParameter("code");
        if(code != null){
            saveToken(code);
            return "dashboard.html";
        }
        return "index.html";
    }

    private void saveToken(String code) throws IOException {
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
        flow.createAndStoreCredential(response, USER_INDENTIFIER_KEY);

    }


}
