package com.nucleodash.googlecalendar.service;


import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;

public interface ICalendarService {
    String getToken(String authId);

}

