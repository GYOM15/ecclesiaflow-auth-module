${msg("eventUpdatePasswordTitle","Password updated")}

${msg("eventUpdatePasswordSubtitle","Your EcclesiaFlow password was successfully changed.")}

${msg("eventDate","Date")}: ${event.date?datetime?string("dd/MM/yyyy HH:mm")}
${msg("eventIpAddress","IP Address")}: ${event.ipAddress}

${msg("eventUpdatePasswordOk","If you made this change, no further action is needed.")}

${msg("eventUpdatePasswordWarning","If you did NOT change your password, your account may be compromised. Please reset your password immediately and contact your church administrator.")}

${msg("eventUpdatePasswordHint","This is a security notification. You will receive this email whenever your password is changed.")}

---
© ${.now?string('yyyy')} EcclesiaFlow. ${msg("allRightsReserved","All rights reserved.")}
