package com.nucleodash.googlecalendar.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.nucleodash.googlecalendar.model.dto.TokenGoogleCalendarRequest;
import com.nucleodash.googlecalendar.service.ICalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/calendar")
public class CalendarController {
    private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";

    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    @Value("${google.oauth.callback.uri}")
    private String CALLBACK_URI;

    @Value("${google.secret.key.path}")
    private Resource gdSecretsKeys;

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private GoogleAuthorizationCodeFlow flow;

    @Autowired
    ICalendarService calendarService;

    @PostConstruct
    public void init() throws Exception {

        // Load client secrets.

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretsKeys.getInputStream()));

        flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .build();
    }



    @GetMapping("/obtenerToken")
    public ResponseEntity<?> sendMailrecuperar (
            @RequestHeader("X-Auth-Id") String authIdHeader
    ){
        Map<String, Object> response = new HashMap<>();
        try{
            response.put("token",   calendarService.getToken(authIdHeader));
            response.put("success", "true" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK) ;
        } catch (Exception e) {
            response.put("mensaje", "Problemas para generar el token" );
            response.put("success", "false" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.SEE_OTHER);
        }

    }
    @PostMapping(value = "/oauth")
    public ResponseEntity<?> saveAuthorizationCode(
            @RequestBody TokenGoogleCalendarRequest token,
            @RequestHeader("X-Auth-Id") String authIdHeader
    ) throws Exception{
        String code = token.getToken();
        Map<String, Object> response = new HashMap<>();
        log.info(authIdHeader);

        try{
            if(code != null){
                saveToken(code, authIdHeader);
            }
            response.put("success", "true" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK) ;
        }catch (Exception e){
            response.put("mensaje", "Error al guardar el token" );
            response.put("error",  e.getMessage());
            response.put("success", "false" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void saveToken(String code, String authIdHeader) throws IOException {
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
        flow.createAndStoreCredential(response, authIdHeader);

    }

    @PostMapping(value = "/create")
    public ResponseEntity<?> createCalendar(
            @RequestHeader("X-Auth-Id") String authIdHeader
    ) throws Exception{
        Map<String, Object> response = new HashMap<>();
        log.info(authIdHeader);
        try{
            Credential cred = flow.loadCredential(authIdHeader);
            Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            log.info(service.toString());
            System.out.println("Client : "+service);
            com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
            calendar.setSummary("Campania");
            calendar.setTimeZone("America/Mexico_City");

            // Insert the new calendar
            com.google.api.services.calendar.model.Calendar createdCalendar = service.calendars().insert(calendar).execute();

            System.out.println(createdCalendar.getId());
            response.put("success", "true" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK) ;
        }catch (Exception e){
            response.put("mensaje", "Error al guardar el token" );
            response.put("error",  e.getMessage());
            response.put("errorTrace",  e.getStackTrace());
            response.put("success", "false" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    @PostMapping(value = "/createEvent")
    public ResponseEntity<?> createEvent(
            @RequestHeader("X-Auth-Id") String authIdHeader
    ) throws Exception{
        Map<String, Object> response = new HashMap<>();
        log.info(authIdHeader);
        try{
            Credential cred = flow.loadCredential(authIdHeader);
            Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Event event = new Event()
                    .setSummary("Google I/O 2015")
                    .setLocation("800 Howard St., San Francisco, CA 94103")
                    .setDescription("A chance to hear more about Google's developer products.");

            DateTime startDateTime = new DateTime("2015-05-28T09:00:00-07:00");
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setStart(start);

            DateTime endDateTime = new DateTime("2015-05-28T17:00:00-07:00");
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setEnd(end);

            String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
            event.setRecurrence(Arrays.asList(recurrence));

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail("lpage@example.com"),
                    new EventAttendee().setEmail("sbrin@example.com"),
            };
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(10),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            event = service.events().insert(calendarId, event).execute();
            System.out.printf("Event created: %s\n", event.getHtmlLink());

            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK) ;
        }catch (Exception e){
            response.put("mensaje", "Error al guardar el token" );
            response.put("error",  e.getMessage());
            response.put("errorTrace",  e.getStackTrace());
            response.put("success", "false" );
            return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
