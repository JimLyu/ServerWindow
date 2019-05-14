package jim.server;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

//s42026501@gmail.com( LABUSE20162017 ) https://www.twilio.com/console
class SMSClient {
  // Find your Account Sid and Token at twilio.com/user/account
  final String ACCOUNT_SID = "AC2506da20fd47ef6a489b656029be2ca0";
  final String AUTH_TOKEN = "67d39a02119d311b341adeaa5b68d414";
  final String SMSFROM = "+12565884250";

  SMSClient(String smsto) {
    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    String msg = "TEST From Java";
    Message message = Message.creator(new PhoneNumber(smsto), new PhoneNumber(SMSFROM), msg).create();
    System.out.println(" SID =" + message.getSid());
  }
}